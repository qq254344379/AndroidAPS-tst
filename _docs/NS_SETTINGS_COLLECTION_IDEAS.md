# NS APIv3 `settings` collection — AAPS integration

The Nightscout APIv3 `settings` collection is a free-form per-app document
store that AAPS uses to sync running configuration and (planned) coordination
state between a master AAPS instance and its clients. This document is the
specification — wire format, modules, semantics, and the roadmap.

## Roadmap at a glance

| Phase | Scope                                                             | State    |
|-------|-------------------------------------------------------------------|----------|
| 1     | Running configuration master→client sync                          | shipped  |
| 2a    | Active scene state (master-published, with scoped record NS ids)  | shipped  |
| 2b    | Pairing infrastructure (master toggle + QR + client scanner)      | shipped  |
| 2c    | Command channel + first commands (`hello`, `scene.start/stop`)    | shipped  |
| 2d    | Orphan detection (master publishes roster; client self-checks)    | shipped  |
| 3     | Shared editable configs (scenes / automation / TT presets)        | planned  |
| 4     | Backup / restore                                                  | planned  |
| 5     | Insulin configuration editing (gated)                             | optional |

## Collection semantics

Per-app document keyed by an `identifier`. Schema is free-form JSON.

- **Authorization.** READ-by-id, CREATE, PATCH, UPDATE, DELETE follow the
  standard `:read` / `:create` / `:update` / `:delete` scopes. SEARCH, HISTORY,
  and the WS `settings` channel require `api:settings:admin`. Tokens without
  the admin role silently lose WS push but retain catch-up GET.
- **Validation.** `validateCommon` requires `date` (number > 946684800000),
  `utcOffset` (number in [-1440, 1440]), `app` (non-empty string) on
  UPDATE / CREATE. All three are immutable after first create — the publisher
  sends fixed constants because the doc is a snapshot, not a timestamped event.
- **Sync.** Only `srvModified` is honored. No `created_at` / `date` fallback.
- **Dedup.** On `identifier` only. Two CREATEs with the same identifier — the
  second becomes a replace. PATCH only updates existing docs (404 on first
  call); UPDATE (PUT) is the upsert primitive.
- **Auto-prune.** If the server enables `API3_AUTOPRUNE_SETTINGS`, docs expire
  by `srvCreated`. PATCH does not refresh `srvCreated`. UPDATE refreshes it.
  Active users get free refresh on every pref change; idle users need an
  explicit re-PUT (planned, not yet implemented).

---

## Phase 1: Running configuration sync (shipped)

Master writes a single `settings/aaps` document on every relevant pref change;
clients read it on connect and on WS push. Replaces the legacy piggyback on
`devicestatus.configuration`.

### Wire format

Identifier: `aaps` — single document per NS instance.

```
{
  identifier:    "aaps",                  // server-managed
  srvCreated, srvModified,                // server-managed
  date:          946684800001,            // fixed constant; immutable after create
  utcOffset:     0,                       //         "
  app:           "AAPS",                  //         "
  schemaVersion: 1,
  runningConfig: {
    insulin, insulinConfiguration,
    aps, apsConfiguration,
    sensitivity, sensitivityConfiguration,
    smoothing,
    safetyConfiguration,
    quickWizardConfiguration,
    scenesConfiguration,
    automationConfiguration,
    overviewConfiguration: {              // free-floating keys (no domain owner)
      units, low_mark, high_mark,
      statuslights_*,
      boluswizard_percentage,
      temp_target_presets,
      used_autosens_on_main_phone
    },
    activeScene: { … } | null,            // Phase 2a — currently running scene
    authorizedClients: {                  // Phase 2d — Active-client roster
      clientIds: [ "<uuid>", … ]
    },
    pump, version
  }
}
```

### Block ownership

Each `*Configuration` block is owned by exactly one `ConfigExportImport`
implementer. The implementer declares the prefs it owns (`syncedKeys`) and how
to refresh its in-memory cache (`reloadInternalState`).

| Block                      | Owner                                       | Cache refresh on apply                        |
|----------------------------|---------------------------------------------|-----------------------------------------------|
| `insulinConfiguration`     | `InsulinImpl`                               | `loadSettings()`                              |
| `apsConfiguration`         | active `APS` plugin                         | none                                          |
| `sensitivityConfiguration` | active `Sensitivity` plugin                 | none                                          |
| `safetyConfiguration`      | `SafetyPlugin`                              | none                                          |
| `quickWizardConfiguration` | `QuickWizard`                               | `setData(...)`                                |
| `scenesConfiguration`      | `SceneRepository`                           | none — `StateFlow`-backed                     |
| `automationConfiguration`  | `AutomationPlugin`                          | `loadFromSP()` + `EventAutomationDataChanged` |
| `overviewConfiguration`    | none (free-floating)                        | none — values read fresh                      |
| `activeScene`              | `ActiveSceneManager`                        | client → `applyActiveScene` snapshot (2a)     |
| `authorizedClients`        | `AuthorizedClientsRepository` (master only) | client → `OrphanDetector.onSettingsDoc` (2d)  |

### Architecture

- `core/interfaces/configuration/ConfigExportImport.kt` — interface declaring
  `val syncedKeys: List<NonPreferenceKey>` and `fun reloadInternalState()`.
  Each plugin that contributes to the running config implements it.
- `core/interfaces/configuration/RunningConfigurationKeys.kt` —
  `fun observableKeys(): List<NonPreferenceKey>`. Returns the union across all
  installed Insulin / APS / Sensitivity / Safety / QuickWizard / Scenes /
  Automation owners plus free-floating overview keys. Implemented by
  `RunningConfigurationImpl`.
- `core/nssdk/localmodel/configuration/NSRunningConfiguration.kt` — typed
  payload of the `runningConfig` block.
- `plugins/configuration/configBuilder/RunningConfigurationImpl.kt` — single
  source of truth for serialization and dispatch.

### Master flow (`config.APS`)

1. `RunningConfigurationPublisher.start(scope)` from `NSClientV3Plugin.onStart()`.
2. Initial publish on start. Then merges per-key
   `preferences.observeChange(...)` for every key in
   `RunningConfigurationKeys.observableKeys()` plus
   `rxBus.toFlow(EventConfigBuilderChange)` (catches plugin-selection changes
   that aren't pref-driven).
3. Debounces 5 s, builds
   `{ schemaVersion, runningConfig, date, utcOffset, app }` and PUTs via
   `nsAndroidClient.updateSettings("aaps", body)`.
4. Logs `► SETTINGS aaps srvModified=<human-readable>` on success or
   `✕ SETTINGS aaps HTTP <code> <body>` on failure.

### Client flow (`config.AAPSCLIENT`)

- **Catch-up GET.** `LoadSettingsWorker` runs in the standard `executeLoop`
  chain on every reconnect. GETs `settings/aaps`, parses, calls
  `runningConfiguration.apply(NSRunningConfiguration)`, advances
  `lastLoadedSrvModified.collections.settings`. `srvModified` is read from the
  doc body (`doc.optLong("srvModified")`); ETag header is used as fallback
  because NS doesn't always set it for settings.
- **Real-time WS.** `NSClientV3Service` settings branch parses incoming
  `create` / `update` events and applies through the same path. Subscription
  requires `api:settings:admin`; without it only catch-up GET runs.

---

## Phase 2: Active scene + command channel

State propagation remains one-way (master → client) via `settings/aaps`. A new
client → master direction is added for **command intent only** — clients never
write state directly. Master is the single authoritative writer for state;
client commands are processed by master, applied locally, and propagated back
through the existing settings doc.

### 2a — Active scene state (master-published) — shipped

Master publishes the currently-running scene as a new block of `runningConfig`,
carrying scene timing and the **NS identifiers** of records the scene created
(but not the master's local Room ids or the master-only `priorSmb` revert flag):

```
runningConfig.activeScene: {
  sceneId:     "<scene definition id>",
  activatedAt: <epoch ms>,
  durationMs:  <number>,
  ttNsId:      <string|null>,    // NS id of the scene's TempTarget
  psNsId:      <string|null>,    // NS id of the scene's ProfileSwitch
  rmNsId:      <string|null>,    // NS id of the scene's RunningMode
  teNsId:      <string|null>     // NS id of the scene's TherapyEvent
} | null
```

- Owner: `ActiveSceneManager` (in `ui/`) implements `ActiveSceneSync` and
  exposes a wire snapshot. The runtime state model is split so the misnomer is
  gone: `ActiveSceneState.priorSmb` (the only truly prior value, master-only)
  is separate from `ActiveSceneState.ScopedRecords` (records the scene
  created, carried as Room id + NS id pairs).
- NS-id ↔ Room-id reconciliation is symmetric: both master and client subscribe
  to `persistenceLayer.observeChanges<TT|PS|RM|TE>()` and fill in whichever
  half of the pair they don't yet have (master gains the NS id once the
  record is uploaded; client gains the Room id once the matching record
  syncs in). Either side may briefly show the scene name without chips
  while the other half is in flight.
- Client UI renders "Scene X running, expires HH:MM" plus the existing
  scene-managed chips (TT/profile/loop-mode), driven by `ScopedRecords.*Id`.
- A `null` block means no scene is active; client clears.

### 2b — Pairing infrastructure — shipped

NS auth alone provides a token-gated channel but no client identity, no replay
protection, and no tamper detection beyond TLS. Per-client signing is layered
on top: pair devices once, sign every message. Secrets never touch NS — only
HMAC signatures do.

#### Master gate

NSCv3 settings: a new boolean preference **`NsClientAllowClientControl`**
(default OFF, master-only — `showInNsClientMode = false,
showInPumpControlMode = false`). When OFF the receiver short-circuits in
`NSClientV3Plugin` before any verification; when ON the polling loop and the
WS dispatch route to `ClientControlReceiver`.

The toggle is observed via `preferences.observe(...).collectLatest { }` so
flipping it on or off live (re)starts/cancels the polling loop without a
plugin restart (`NSClientV3Plugin.kt` ~line 230).

#### Pairing flow (shipped)

One-time per client.

1. **Master**: Manage sheet → **Authorized clients** screen → FAB **+** → name prompt.
2. Master generates a 32-byte random `secret` and a `clientId` (UUID).
   `AuthorizedClientsRepository.addPending(name, qrTtlMs, now)` persists the
   entry as `{ clientId, encryptedSecret, name, state: Pending, createdAt,
   qrExpiresAt, counterReceived = 0 }` to a JSON-serialized
   `StringNonKey.NsClientControlAuthorizedClients` preference. Secrets are
   wrapped via `SecureEncrypt` (AndroidKeyStore-backed AES/GCM) before
   persistence.
3. Master displays a `PairingPayload` QR carrying
   `{ masterInstallId, clientId, secretHex, expiresAt }`. (No capabilities
   field — see "Authorization model" below.) The pairing screen uses
   `FLAG_SECURE`; QR content is blurred until tap.
4. **Client**: Manage sheet → **Pair with master** → camera scanner
   (CameraX + ZXing). Decoded payload routes to a confirmation dialog.
5. On confirm, `ClientPairingRepository.pair(payload, now)` stores
   `{ masterInstallId, clientId, encryptedSecret, counterSent = 0, pairedAt = now }`
   via the same SecureEncrypt wrapping. `pairedAt` (`LongNonKey.NsClientControlPairedAt`,
   `exportable = false`) feeds the orphan-detector race guard — see 2d.
6. Client immediately publishes a signed `hello` envelope (counter = 1) via
   `ClientControlPublisher.publish(ClientControlMessage.Hello())`.
7. Master verifies; on success `markActive` flips the entry from Pending to
   Active and bumps `counterReceived`.
8. Pending entries with `now > qrExpiresAt` are pruned by
   `AuthorizedClientsRepository.current(now)` on every read.

#### Authorization model

**Binary**: paired clients can issue any command the master version supports.
There is no per-client capability scoping (the original spec had one; it was
removed mid-implementation as unnecessary complexity for the v1 use case —
human-driven AAPSClient on a partner's phone).

#### Storage

- **Master**: `AuthorizedClientsRepository` — list of `AuthorizedClient`
  entries serialized as JSON to `StringNonKey.NsClientControlAuthorizedClients`
  (`exportable = false`). Secrets always wrapped via `SecureEncrypt`. The
  preference is excluded from AAPS export. `secretLookup(clientId)` returns
  decrypted bytes + `counterReceived` as a single atomic snapshot under a
  monitor lock.
- **Client**: `ClientPairingRepository` — single pairing in
  `StringNonKey.NsClientControlMasterSecretEnc` plus scalar pref keys for
  `masterInstallId` / `clientId` / counter. `nextSignedEnvelope(type, payload,
  now)` increments and persists `counterSent` atomically before returning
  the signed envelope.

#### Revocation

Master's Authorized clients screen → swipe to delete → entry removed from the
JSON list. Future envelopes from that `clientId` route to "unknown clientId"
in the receiver and the doc is DELETEd as hopeless (no signature check
attempted — there's no key to check against).

#### Failure modes

- **AndroidKeyStore reset / backup-restore**: `SecureEncrypt.decrypt` returns
  empty or `isValidDataString` rejects the blob → `secretLookup` returns null
  → entry behaves as if missing. Logged distinctly per case.
- **Master reinstall** wipes the paired-clients table; clients become orphans.
  2d closes the loop in the common case: the master's next `settings/aaps`
  publish carries an empty `authorizedClients.clientIds`, the client's
  `OrphanDetector` posts an Android notification prompting re-pair.
  **Coverage gap:** an uninstalled / dead master never republishes — that case
  needs a heartbeat / liveness mechanism, deferred per Phase 2 scope.
- **Scraped expired QR replay**: signed envelope with a pending entry's
  pre-expiry secret arriving after `qrExpiresAt`. `current(now)` prunes the
  expired entry; receiver captures the raw pre-prune entry via `findRaw`
  to log distinctly (`pairing window expired` vs `unknown clientId`) before
  DELETEing the doc. Behavior unchanged from a pure pruning model; the split
  log makes real attempted replays visible against ordinary typo noise.

### 2c — Command channel — shipped

Master subscribes to the NS WS `settings` channel (already in place from
Phase 1) and routes any settings `create`/`update` event whose identifier
starts with `aaps_clientcontrol_` to `ClientControlReceiver.onSettingsDocChanged`
(see `NSClientV3Service.onDataCreateUpdate`'s `"settings"` branch + the
`NSClientV3Plugin.handleClientControlSettingsEvent` delegate).

A polling fallback runs every 5 minutes (mirroring main NSCv3 loop cadence)
when WS is configured, calling `ClientControlReceiver.processPending()` which
uses `searchSettings(limit = 100)` and filters by the
`aaps_clientcontrol_` prefix — catches anything WS missed during disconnect
without needing to know identifiers in advance.

#### Wire format

Settings doc:

```
{
  identifier:    "aaps_clientcontrol_hello_<clientId>"
              | "aaps_clientcontrol_cmd_<type>_<clientId>",
  date, utcOffset, app: "AAPS",          // validateCommon shims
  schemaVersion: 1,
  envelope: {
    clientId:    "<paired-client uuid>",
    counter:     <number>,                // strictly monotonic per client
    timestamp:   <epoch ms>,
    type:        "hello" | "scene.start" | "scene.stop" | ...,
    payload:     "<JSON string of ClientControlMessage>",
    signature:   "<hex HMAC-SHA256>"
  }
}
```

`payload` is the **literal JSON string** that travelled the wire (not a JSON
object). Signature verification compares the bytes that travelled, immune to
JSON canonicalization differences. Inside that JSON is a polymorphically
serialized `ClientControlMessage` sealed class — every variant carries
`@SerialName` as the wire discriminator (`"hello"`, `"scene.start"`,
`"scene.stop"`).

`envelope.type` is derived from the polymorphic discriminator at publish
time (single source of truth — no risk of drift between the two).

#### Identifier scheme

Per-type slot, one per (client, message-type) pair:

- **Hello**: `aaps_clientcontrol_hello_<clientId>`
- **Commands**: `aaps_clientcontrol_cmd_<type>_<clientId>` (e.g.
  `aaps_clientcontrol_cmd_scene.start_<clientId>`)

This prevents cross-type collision (a fresh `scene.start` won't overwrite an
unprocessed `scene.stop`). **Same-type latest-wins is intentional**:
re-publishing the same identifier overwrites the previous in-flight message,
which matches user-changed-mind semantics for human-paced taps. Future
variants needing queueing must invent a different identifier scheme.

#### Canonical signing input

```
"$clientId|$counter|$timestamp|$type|$payload"
```

`signature = HMAC-SHA256(secret, canonical)` (hex-encoded). Implementation in
`core/nssdk/utils/ClientControlCrypto.kt`.

#### Verification order on master

In `ClientControlReceiver.verifyAndAck`, in this order:

1. Parse envelope from `doc.envelope`. Malformed → DELETE (hopeless).
2. Resolve client by `envelope.clientId`. Unknown → DELETE (hopeless).
   Pre-prune raw lookup via `AuthorizedClientsRepository.findRaw` happens
   first so the receiver can log distinct messages for "pairing window
   expired" (a scraped expired-QR replay) vs "unknown clientId" (typo /
   stale identifier). Same outcome either way; the split log makes real
   attempted replays visible against ordinary noise.
3. **HMAC signature** verifies against the stored secret. Fail → leave doc
   for diagnostics, no state change.
4. **Counter** strictly greater than `counterReceived`. Fail → leave doc
   (replay).
5. **Timestamp** within ±5 min of master clock. Fail → leave doc.
6. Decode `envelope.payload` as `ClientControlMessage`. Fail (verified-but-
   undecodable) → advance counter + DELETE (older master, newer client).
7. Dispatch by sealed-class `when`. Currently: `Hello` → markActive /
   bumpLastSeen; `SceneStart` → `SceneAutomationApi.runScene(...)`;
   `SceneStop(triggerChain)` → either `stopActiveScene()` or
   `stopActiveSceneAndStartScene(targetId)` — see "Scene chain handling".
8. DELETE the doc.

Sig-first means a failure log unambiguously identifies forgery rather than
being shadowed by a benign replay log on a forged-but-stale message.

#### Scene chain handling

`SceneStop(triggerChain: Boolean)`:

- `triggerChain = false`: master calls `SceneAutomationApi.stopActiveScene()`.
  Plain deactivate, chain dies if the active scene chains to another.
- `triggerChain = true`: master reads its currently-active scene's `endAction`
  fresh at receipt time (via `ActiveSceneSync.activeSceneSnapshot()` +
  `SceneAutomationApi.getScene(id).endAction`). If a `ChainScene(targetId)` is
  there, master calls
  `SceneAutomationApi.stopActiveSceneAndStartScene(targetId)` which does the
  TOCTOU re-check (target enabled + runtime allows activation) and falls
  back to plain deactivate if conditions aren't met. Chain target id is
  **never** taken from the wire — master uses its own current state so a
  stale client view can't trigger an unintended scene.

Outcome distinguishes `chain→<id>` / `no-chain-target` / `plain-stop` in the
NS log line for diagnosability.

#### Cross-module decoupling

- **`SceneAutomationApi`** (in `core/interfaces/scenes`) is the single
  cross-module surface for scene operations. Three callers — automation,
  wear-sync, and now client-control — all use it. The `runScene` /
  `stopActiveScene` / `stopActiveSceneAndStartScene` triplet covers all v1
  command needs. `stopActiveSceneAndStartScene` returns the new
  `SceneAutomationResult.ChainCompleted(endedSceneName, targetSceneName,
  failedCount, totalCount)` variant so callers (notifications + NS log line)
  can render the "ended → target: X of Y failed" detail without
  re-implementing the activation logic. Other API methods never return
  `ChainCompleted`; their callers carry an exhaustive-when branch that
  treats it as a contract violation.
- **`ClientControlSceneSender`** (in `core/interfaces/scenes`) is the thin
  primitive-typed outbound surface so `:ui` can dispatch wire commands without
  taking a project dependency on `:plugins:sync`. Implemented by
  `ClientControlPublisher` which delegates each method to
  `publish(message: ClientControlMessage)`. Returns the sealed
  `ClientControlSendResult` (`Success` / `NotPaired` / `PublishFailed(reason?)`)
  rather than a `Boolean` — collapsing the three cases hides the recoverable
  "not paired" case from the user. `:ui` call sites chain
  `.surfaceErrorDialog(rxBus, rh)` (in `ui/.../scenes/ClientControlErrorDialog.kt`)
  to surface distinct `ErrorDialog`s via the global `EventShowDialog.Error` bus.
- **`SceneChainTargetResolver`** (in `:ui`) is the single source of truth for
  the canChain policy. `runtimeAllowsActivation()` (loop running + pump
  initialized + profile set) is the master-side gate; AAPSClient uses
  `resolveCatalogChainTarget` (catalog-only — local pump/loop don't reflect
  master state) and relies on master's TOCTOU re-check.

### 2d — Orphan detection (master-published roster) — shipped

Closes the asymmetry where a paired client has no signal that the master has
revoked its pairing (or been wiped and reinstalled): the master publishes the
active-client roster as part of `settings/aaps`, and the client checks itself
into that roster on every doc apply.

#### Wire format

New block under `runningConfig`:

```
runningConfig.authorizedClients: {
  clientIds: ["<active-client uuid>", ...]   // Pending entries excluded
}
```

Only `clientId` UUIDs are exposed — no names, timestamps, or secret material.
Pending entries are deliberately omitted: a client that has scanned the QR
but not yet completed `hello` hasn't been authorized as Active.

**Block absence is the backward-compat marker.** Old masters that don't
publish the block return `null` here; clients must not infer orphan status
from `null`. Only `block present + own clientId missing` is an orphan signal.

#### Publish trigger

Master's `RunningConfigurationPublisher` (`:plugins:sync`) merges
`AuthorizedClientsRepository.observe()` into its existing trigger flow
(pref changes + plugin-switch events). Same 5-second debounce — so pairing
completion, swipe-to-revoke, and pending-prune all republish naturally.

The block itself is appended to the JSON payload at publish time inside
`:plugins:sync`, not via `RunningConfigurationImpl.configuration()`. That
keeps `:plugins:configuration` free of an inter-module dependency on the
sync module's `AuthorizedClientsRepository`.

#### Client-side detection

`OrphanDetector` (in `:plugins:sync`) is called by both apply paths on the
client (`LoadSettingsWorker` catch-up GET + `NSClientV3Service` WS settings
branch). Logic:

1. No-op on master role (`!config.AAPSCLIENT`).
2. Block absent → no signal (older master, can't infer).
3. Block present + own clientId in `clientIds` → dismiss any prior orphan
   notification (recovery path covers re-pair).
4. Block present + own clientId missing → race-window guard, then fire.

**Race-window guard:** master debounces 5s after pairing-state changes.
A `settings/aaps` doc in flight from before our hello won't include our
clientId. Skip the alarm when `docSrvModified < pairedAt + 60s`. One-sided
guard: a doc older than our pairing is presumed pre-pair, never an orphan
signal.

Notification on fire: `NotificationId.NSCLIENT_PAIRING_ORPHAN(95, NORMAL,
SYNC)` with body string `clientcontrol_orphan_notification`. Posted via the
existing notification machinery; dismissed automatically on the next doc
that contains our clientId.

#### Coverage gap

A dead or uninstalled master never republishes `settings/aaps`. `OrphanDetector`
cannot signal that case — the orphan signal requires at least one inbound
republish that excludes us. Closing this would need a separate liveness
mechanism (heartbeat, last-publish-staleness check, or per-command ACK).
Out of scope for Phase 2.

### UI

**Master:**

- NSCv3 settings → **Allow client control** preference toggle (default OFF).
- Manage sheet → **Authorized clients** entry (gated on the toggle).
- Authorized clients screen: list of paired clients with state badge,
  last-seen, swipe-to-delete; FAB **+** triggers the pair dialog (name + QR).

**Client:**

- Manage sheet → **Pair with master** entry (always shown in
  `config.AAPSCLIENT` mode).
- Pair screen: camera scanner → confirmation → already-paired status when
  paired (Unpair button).
- Scene-control failure (publish failed / not paired) surfaces a modal
  `ErrorDialog` via the global `EventShowDialog.Error` channel — distinct
  messages for the two cases. See cross-module decoupling notes.
- Orphan signal (`NSCLIENT_PAIRING_ORPHAN`) surfaces as an Android
  notification when the master's `runningConfig.authorizedClients.clientIds`
  doesn't contain our clientId (subject to the race-window guard in 2d).

### Out of scope for v1

- Per-client capability scoping (dropped during slice 2 — see "Authorization
  model").
- Multi-master per client.
- ECDSA keypairs (HMAC is sufficient for the threat model).
- Master → client response messages (the `settings/aaps` republish on state
  change is the implicit ack — client UI updates from the new active-scene
  state, not from a direct command response).

---

## Phase 3: Shared editable configs (planned)

Multi-writer surface for editable lists: scene definitions, automations, TT
presets. Insulin configurations are explicitly excluded (see phase 5).

### Concurrency model

NS APIv3 has no server-side If-Match or version-checked PATCH; concurrency is
app-level optimistic.

1. Client reads doc, captures `srvModified = X`.
2. User edits.
3. Client re-reads `srvModified` immediately before PATCHing. If it has
   advanced, prompt "this was changed on another device — reload?" rather
   than silently overwriting.
4. PATCH body includes `__expectedSrvModified: X` plus the actual changes.
5. Client captures the new `srvModified` from the response.

Audit trail: every cross-device edit also writes a `Note` treatment with
`{ enteredBy: "AAPSClient/<installId>", notes: "Edited scene 'Exercise'" }`.

### Trust model

Most clients can read everything; only some can edit. Phase 2 pairing is the
foundation: paired clients are the candidates for the edit allowlist. Server
does not enforce — the gate is in-app on the client (untrusted clients refuse
to expose edit UI). Threat model is "wrong family member opens AAPSClient",
not adversarial.

---

## Phase 4: Backup / restore (planned)

Independent of phases 1–3.

- Identifier class: `aaps-backup-<installId>` — one doc per install,
  exclusive owner.
- Reuses the existing `ImportExportPrefs` serializer with secrets filtered out
  (master password, paired pump MACs, NS tokens, paired-clients table).
- Daily upload + manual "Backup now" button.
- Restore is always user-triggered. SEARCH lists existing backups, user picks,
  diff against current prefs, confirm. Per-section opt-out during restore.

---

## Phase 5 (optional): Insulin configuration editing

Gated behind a stricter allowlist than phase 3 — `criticalConfigClients`,
opted into per-client by an explicit confirmation on the master phone. PATCH
path same as phase 3 with additional confirmation UI on commit.

---

## Identifier strategy

| Identifier                                       | Owner                           | Phase   | Purpose                                                          |
|--------------------------------------------------|---------------------------------|---------|------------------------------------------------------------------|
| `aaps`                                           | master                          | 1+2a+2d | Running configuration (incl. active scene + authorized-clients)  |
| `aaps_clientcontrol_hello_<clientId>`            | client (signed) → master DELETE | 2c      | First post-pairing handshake, promotes Pending → Active          |
| `aaps_clientcontrol_cmd_<type>_<clientId>`       | client (signed) → master DELETE | 2c      | Per-(client, message-type) command slot, latest-wins             |
| `aaps-cfg-<masterInstallId>`                     | master                          | 3       | Per-master config when single `aaps` is no longer enough         |
| `aaps-backup-<installId>`                        | install (exclusive)             | 4       | Per-install backup                                               |

`<installId>` / `<masterInstallId>` is a UUID generated on first launch and
persisted in prefs. Stable across app updates; not re-generated on restore.

---

## Out of scope (deliberately)

- **Insulin config edits without trust allowlist.** Phase 5 only.
- **Server-side enforcement of write permissions.** NS has no per-section
  ACLs; trust is in-app and assumes the client app respects its own rules.
- **Live loop telemetry on settings.** IOB / COB / current temp basal / loop
  status stay on devicestatus.
- **Profile (basal / ISF / ICR) sync.** Stays on the `profile` collection.
- **Per-section ACLs on the wire.** Doc is not partitioned on the server.
