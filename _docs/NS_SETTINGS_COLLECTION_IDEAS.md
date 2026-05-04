# NS APIv3 `settings` collection — design notes

Brainstorming for what AAPS could store in the Nightscout APIv3 `settings` collection.
Phase 1 has shipped: the `settings/aaps` doc is now the sole channel for running
configuration between master and clients. Phases 2-5 remain as planned future work.

## Implementation status

| Component                                                                           | State                |
|-------------------------------------------------------------------------------------|----------------------|
| APIv3 SDK methods (get / search / history / create / patch / **put** / delete)      | ✅ done               |
| `ConfigExportImport` interface refactor (syncedKeys, reloadInternalState)           | ✅ done               |
| Plugin adoption of declarative `syncedKeys`                                         | ✅ done               |
| Unified `put`/`store`/`observeChange` over `NonPreferenceKey`                       | ✅ done               |
| `RunningConfigurationImpl` schema-driven build/apply                                | ✅ done               |
| `RunningConfigurationPublisher` (master writer)                                     | ✅ done (phase 1)     |
| `LoadSettingsWorker` real catch-up loader                                           | ✅ done (phase 1)     |
| WS `settings` branch apply path                                                     | ✅ done (phase 1)     |
| Devicestatus piggyback removed (master write + client read)                         | ✅ done (phase 1)     |
| `Configuration` typed model extracted from `NSDeviceStatus` → `NSRunningConfiguration` | ✅ done (cleanup)  |
| `QuickWizard` / `Scenes` / `Automation` as `ConfigExportImport` with own blocks     | ✅ done (cleanup)     |
| Active scene state + end-from-client                                                | ⏳ phase 2            |
| Shared editable scenes / automations / TT presets (multi-writer)                    | ⏳ phase 3            |
| Backup / restore (per-install identifier)                                           | ⏳ phase 4            |
| Insulin config editing (gated)                                                      | ⏳ phase 5 (optional) |

## What the collection is

- Generic NS APIv3 collection, identified per-app: each "app" (xdrip, AAPS, etc.) keeps
  its own document keyed by an `identifier`. Schema is **completely free-form** JSON
  (just `DocumentBase` envelope + arbitrary fields).
- Authorization: SEARCH and HISTORY require `api:settings:admin`. READ-by-id, CREATE,
  PATCH, UPDATE, DELETE follow the normal `:read`/`:create`/`:update`/`:delete` scopes.
  WS subscription to the `settings` channel requires `api:settings:admin` (other
  collections only need `:read`). Users are instructed to issue an admin token; if they
  use a `:read`-only token, real-time WS push is silently dropped but catch-up GET via
  `LoadSettingsWorker` still works.
- `validateCommon` requires `date` (number > 946684800000), `utcOffset` (number in
  [-1440, 1440]), `app` (non-empty string) on UPDATE/CREATE. All three are **immutable**
  after first create. The publisher sends fixed constants — the doc is a config snapshot,
  not a timestamped event.
- Sync semantics: only `srvModified` is honored — no `created_at` / `date` fallback.
- Dedup: only on `identifier`. Two POSTs with the same identifier → second one becomes
  a replace. **PATCH only updates existing docs (404 on first call)**; PUT (NS3 "UPDATE")
  is the upsert primitive — that's what the publisher uses.
- Auto-prune: if the server enables `API3_AUTOPRUNE_SETTINGS`, docs expire by
  `srvCreated`. PATCH does NOT refresh `srvCreated`. PUT/replace **does** refresh
  `srvCreated` (replace.js sets it to `now` if `storageDoc.srvCreated` was unset or to
  the existing value). For active users every pref change refreshes the doc; idle users
  could still age out — quarterly self-refresh is a phase-2+ concern.

## Scope

Confirmed direction, in shipping order:

1. **Phase 1 — RunningConfiguration migration.** ✅ Shipped. Move what was smuggled
   inside devicestatus (master→client config exchange) onto a dedicated settings doc.
   One writer (master), N readers (clients). No new user-facing surface; replaces an
   objectively broken channel.
2. **Phase 2 — Active scene state + end-from-client.** Master writes scene state to
   the same doc; client can PATCH a single `scene.endRequested` flag; master observes
   and runs deactivate. First multi-writer surface, but on a single boolean — small
   enough to validate the model.
3. **Phase 3 — Shared editable configs** (scenes, automations, TT presets). Real
   multi-writer with optimistic concurrency + audit trail. **Insulin configurations
   are explicitly excluded from this phase.**
4. **Phase 4 — Backup / restore.** Independent of phases 1-3. Per-install identifier,
   distinct doc.
5. **Phase 5 (optional, gated)** — Insulin configuration editing from client, only if
   a trust allowlist is added (see §"Trust model").

## Phase 1 (shipped)

### What's on the wire

Identifier: `aaps` (single, per-NS-instance). Per-master-id deferred to phase 2/3.

```
{
  identifier: "aaps",                              // server-managed
  srvCreated, srvModified,                         // server-managed
  date: 946684800001,                              // fixed constant (NS API requires it; immutable)
  utcOffset: 0,                                    //         "
  app: "AAPS",                                     //         "
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
    overviewConfiguration: {                       // free-floating keys only — no domain owner
      units, low_mark, high_mark, statuslights_*, boluswizard_percentage,
      temp_target_presets, used_autosens_on_main_phone
    },
    pump, version
  }
}
```

Each `*Configuration` block is owned by exactly one `ConfigExportImport` implementer:

| Block                       | Owner                       | Cache rebuild on apply |
|-----------------------------|-----------------------------|------------------------|
| `insulinConfiguration`      | `InsulinImpl`               | yes — `loadSettings()` |
| `apsConfiguration`          | active `APS` plugin         | no                     |
| `sensitivityConfiguration`  | active `Sensitivity` plugin | no                     |
| `safetyConfiguration`       | `SafetyPlugin`              | no                     |
| `quickWizardConfiguration`  | `QuickWizard`               | yes — `setData(...)`   |
| `scenesConfiguration`       | `SceneRepository` (`Scenes`)| no — `StateFlow`-backed |
| `automationConfiguration`   | `AutomationPlugin`          | yes — `loadFromSP()` + `EventAutomationDataChanged` |
| `overviewConfiguration`     | none (free-floating)        | no — values read fresh |

### How it flows

**Master** (`config.APS`):

1. `RunningConfigurationPublisher.start(scope)` (called from `NSClientV3Plugin.onStart()`).
2. Initial publish on start. Then merges `preferences.observeChange(...)` for every
   key in `RunningConfigurationKeys.observableKeys()` plus
   `rxBus.toFlow(EventConfigBuilderChange)` (catches plugin switches that aren't
   pref-driven).
3. Debounces 5s, builds `{ schemaVersion, runningConfig, date, utcOffset, app }`,
   PUTs via `nsAndroidClient.updateSettings("aaps", doc)`.
4. Logs `► SETTINGS aaps srvModified=<human-readable date>` on success or
   `✕ SETTINGS aaps HTTP <code> <error>` on failure.

**Client** (`config.AAPSCLIENT`):

- **Catch-up GET**: `LoadSettingsWorker` runs in the standard `executeLoop` chain on
  every reconnect. GETs `settings/aaps`, parses the doc, calls
  `runningConfiguration.apply(NSRunningConfiguration)`, advances
  `lastLoadedSrvModified.collections.settings`.
- **Real-time WS**: `NSClientV3Service.kt` settings branch parses incoming
  `create`/`update` events, applies the same way. Subscription requires
  `api:settings:admin`; if absent, only catch-up GET runs.

### Architecture

- `ConfigExportImport` interface (`core/interfaces/configuration/`):
  ```kotlin
  interface ConfigExportImport {
      val syncedKeys: List<NonPreferenceKey>
      fun reloadInternalState()
  }
  ```
  Each owner declares the prefs it owns; `reloadInternalState()` is a one-line
  acknowledgement (no-op or cache rebuild).
- `RunningConfigurationKeys` interface (`core/interfaces/configuration/`):
  `observableKeys()` returns the union across all installed
  Insulin/APS/Sensitivity/Safety/QuickWizard/Scenes/Automation owners plus
  `SyncedConfigSchema.overviewKeys`. Implemented by `RunningConfigurationImpl`.
- `NSRunningConfiguration` (`core/nssdk/localmodel/configuration/`): typed payload of
  the `runningConfig` block. Replaced the (now-deleted) `NSDeviceStatus.Configuration`
  nested class. The `configuration` field on `NSDeviceStatus` is gone — devicestatus
  no longer carries any config.

### Side-effect bug fixes

Three issues that pre-dated this work were resolved as side effects of the refactor:

- **`SensitivityAAPSPlugin`** had `AutosensMin` listed twice and was missing
  `AutosensMax` in its `configuration()` chain. The chain is gone; `syncedKeys` lists
  all four correctly.
- **`QuickWizard`** cache went stale on external pref writes (cache rebuilt only at
  app init). Now `applyToPlugin(quickWizard, ...)` calls `reloadInternalState()` →
  `setData(...)`.
- **Single-source-of-truth**: previously each ConfigExportImport had to keep parallel
  build-side and apply-side key lists in sync; the `syncedKeys` declaration is now
  iterated for both directions.

### What was decided along the way (post-design)

- **Drop dual-write entirely.** Original plan was master-side dual-write to both
  devicestatus and settings during a transition window. User opted to skip
  transition: lockstep deployment is the assumption, so cleaner single-channel
  state at the cost of breaking old clients on first deploy. Net code that didn't
  need writing.
- **PUT (UPDATE), not PATCH.** First publish failed 404 because PATCH only updates
  existing docs. Switched to NS3 UPDATE (PUT), which upserts: replaces if exists,
  inserts if not. SDK gained `updateSettings(identifier, body)`.
- **`date`/`utcOffset`/`app` fields required by NS validation.** PATCH didn't
  validate these (only validates fields that are present); UPDATE always does, and
  they're in the immutable list. Solved with fixed constants — the doc has no
  semantic time/offset/app, so any stable value works once stored.
- **`srvModified` from the doc body, not the ETag.** The SDK's `getSettings`
  exposes `lastServerModified` parsed from the ETag header, but NS doesn't set ETag
  on settings GETs. Worker reads `doc.optLong("srvModified")` first, falls back to
  ETag.
- **`Configuration` extracted from `NSDeviceStatus`.** Once the devicestatus
  piggyback was removed, the nested type had no semantic relation to devicestatus.
  Moved to top-level `NSRunningConfiguration` (`core/nssdk/localmodel/configuration/`).
- **`QuickWizard` moved to its own block.** Originally nested in
  `overviewConfiguration` (matching devicestatus shape); after extraction, given a
  sibling block parallel to other plugin-owned blocks. The cache invalidation hook
  (`reloadInternalState()`) only fires when `applyToPlugin(...)` is called, which
  required the dedicated block.
- **`Scenes` + `Automation` joined as full ConfigExportImport sources.** New
  `Scenes` interface in `core/interfaces/scenes/` (impl: `SceneRepository`).
  `Automation` interface extended to also be `ConfigExportImport` (impl:
  `AutomationPlugin`). Each gets its own block + cache-aware apply path.

## Phases 2-5 (planned)

### Phase 2 — Active scene + end-from-client

- Master writes `scene` section on activate / deactivate / duration change.
- Client UI shows active scene from the same doc the running config came from.
- "End scene" button on client: PATCH `{scene: {endRequested: true,
  endRequestedBy: <installId>}}`.
- Master's settings observer reacts to that flag, runs the existing scene
  deactivation path, clears the flag and the scene state.
- Client sees the cleared state via WS push.

Race tolerance: if the master ends the scene at the same moment a client requests
an end, both code paths converge on "scene is now null" — fine.

This phase introduces the first **multi-writer** surface (master + N clients), but
limited to a single flag — keeps the design honest before opening up phase 3.

### Phase 3 — Shared scenes / automations / TT presets

- Add `shared` section with the three editable lists.
- Build the optimistic-concurrency PATCH wrapper.
- Add audit trail (`Note` treatments).
- Trust-gated edit UI on clients (read everywhere, edit only if `installId in
  trustedClients`).
- **Explicitly excluded:** insulin configurations.

### Phase 4 — Backup / restore

- New identifier class `aaps-backup-<installId>`.
- Reuse existing `ImportExportPrefs` serializer; filter out secrets list (master
  password, paired pump MACs, NS tokens).
- Daily upload + manual "Backup now" button.
- Restore is always user-triggered. List existing `aaps-backup-*` docs via SEARCH,
  user picks, diff against current prefs, confirm.
- Per-section opt-out during restore.

### Phase 5 (optional) — Insulin config editing

- Separate `criticalConfigClients` allowlist; explicit per-client opt-in on the
  master.
- Same PATCH path as phase 3, but additional confirmation UI on commit.

## Identifier strategy (forward-looking)

Two identifier classes for phase 2+:

- `aaps-cfg-<masterInstallId>` — shared config doc. Master writes everything;
  trusted clients PATCH only specific sections. One per logical AAPS install.
- `aaps-backup-<installId>` — per-install backup. Only the install that owns the
  id reads/writes this. No contention.

`<masterInstallId>` is a UUID generated on first launch of the master AAPS and
persisted in prefs. Clients are configured to follow a specific master id. Same
model the wear pairing already uses.

Phase 1 uses just `"aaps"` for now — single doc per NS instance.

## Concurrency for multi-writer sections (phase 3)

NS APIv3 has no server-side If-Match or version-checked PATCH. App-level optimistic
concurrency:

1. Client reads doc, captures `srvModified = X`.
2. User edits a scene/automation/preset.
3. Client constructs PATCH body that includes `__expectedSrvModified: X` plus the
   actual changes.
4. Before PATCHing, client re-reads the latest `srvModified`. If it has advanced,
   show "this was changed on another device — reload?" rather than silently
   overwriting.
5. After successful PATCH, capture the new `srvModified` from the response.

Audit trail: every cross-device edit also writes a `Note` treatment with
`{enteredBy: "AAPSClient/<installId>", notes: "Edited scene 'Exercise'"}` so the
patient sees what was changed and by whom.

## Trust model (phase 3+)

Most clients should be allowed to read everything. Only some should be allowed to
edit `shared.*`. Insulin configurations, if ever opened up, need an even stricter
gate.

`trustedClients` list inside the doc itself, master-managed.

- Master device generates a per-install id on first launch.
- Clients pair with master and the master adds the client's install id to
  `trustedClients`.
- Server-side this isn't enforced (any admin token can PATCH the doc); the gate is
  enforced *in-app*. Untrusted clients refuse to expose edit UI for `shared.*`.
  Threat model is "wrong family member opens AAPSClient", not adversarial.

For phase 5 (insulin config editing), a separate `criticalConfigClients` list,
opted into per-client by an explicit confirmation on the master phone.

## Open issues / decisions still pending

- **Auto-prune defense.** Idle users (no pref change for 90+ days) could see the
  doc age out. Phase 1 doesn't have a quarterly re-PUT worker yet. Add when the
  first user reports it, or as part of phase 2.
- **Pairing flow.** Does the existing wear pairing extend to AAPSClient pairing
  for the `trustedClients` list, or do we need a fresh QR/code flow?
- **What `installId` actually is.** A UUID in prefs is fine, but it must be stable
  across app updates and not leak useful identifying info if the doc is exposed.
- **Conflict reload UX in phase 3.** Probably a snackbar with "Reload" and
  "Overwrite anyway" — but worth a real design pass.

## Things deliberately not happening

- **Insulin config edits without trust allowlist.** Phase 5 gates this; without
  the gate it's not on the table.
- **Server-side enforcement of write permissions.** NS doesn't support per-section
  ACLs; trust is in-app and assumes the client app respects its own rules. Threat
  model is "wrong family member", not adversarial.
- **Live loop telemetry on settings.** IOB / COB / current temp basal / loop
  status stay on devicestatus. Settings is for things that change on user action
  or preference change, not on every loop tick.
- **Profile (basal/ISF/ICR) sync.** Stays on the `profile` collection. Settings
  doesn't duplicate.
- **Per-section ACLs on the wire.** Trust is in-app only. Doc is not partitioned
  on the server.

## Discussion log (condensed)

Original framing recommended backup-first; user pushed back, identifying the
running-config piggyback on devicestatus as the strongest case (a) — "ugly,
delayed, inconsistent". Reordered scope: phase 1 = piggyback removal, phase 2 =
active scene, phase 3 = multi-writer shared lists, phase 4 = backup, phase 5 =
gated insulin config editing.

`ConfigExportImport` interface picked as the single source of truth (vs. a central
schema object): each plugin already owns its prefs, so colocating `syncedKeys`
with the owner avoids parallel registries. `reloadInternalState()` left abstract
(no default body) to force every implementer to declare cache awareness — one
line for cacheless plugins, real work for cache-having ones (Insulin, QuickWizard,
Automation).

Refactor was sliced into compile-clean steps with no behavior change before the
publisher could land: interface change → plugin adoption → unified
put/store/observeChange → schema-driven `RunningConfigurationImpl` → drop dead
`configuration()`/`applyConfiguration()` interface methods → publisher + worker
+ WS branch + drop devicestatus piggyback → extract `Configuration` type → add
QuickWizard/Scenes/Automation own blocks. All steps are now in the codebase; this
doc captures the destination, not the journey.
