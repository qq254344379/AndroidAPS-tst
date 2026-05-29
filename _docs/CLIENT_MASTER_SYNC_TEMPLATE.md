# Client → Master Configuration Sync — Template

Pattern crystallised while wiring scene-definition sync between AAPS master and
AAPSCLIENT over the NS APIv3 settings collection. Use this as a checklist when
adding the same kind of bidirectional sync for TempTarget presets, Automation
rules, QuickWizard entries, profile-store edits, etc.

The shape is the same every time:

1. A piece of configuration the user can edit on *either* device.
2. Master is the source of truth for runtime behaviour.
3. Edits on the client must reach master and converge across every paired client.

## Core invariants

These hold for every config domain that uses this pattern. Violating them
breaks one of the edge cases below.

| Invariant | Why |
|---|---|
| Every domain entity carries `lastModified: Long` (ms epoch, default `0L`). | Drives per-entity last-writer-wins on the master. Bumped *only* on user edit, never on import / republish — otherwise a snapshot round-trip looks fresher than the entity that originated it. |
| Every entity carries `isValid: Boolean` (default `true`). | Soft-delete signal. The wire model has no separate "removed" channel; a `false` entry IS the tombstone. |
| Client→master config edits travel as one new `ClientControlMessage` variant per domain. | Reuses the existing signed-envelope auth + replay protection. New variant = new identifier slot, latest-wins per slot. |
| Master receive performs per-entity last-writer-wins merge against the existing pref JSON. | Non-overlapping client edits never collide; concurrent edits on the same entity resolve by wall-clock (rare). |
| Master republish goes via the existing `RunningConfigurationPublisher` debounce by writing the merged JSON back to the synced pref. | Fan-out to other paired clients is automatic — same path the running-config doc already uses. |
| Editor display and entity lookups (`getById`, etc.) filter `isValid = false`. | Tombstones are an implementation detail, never visible in UI. |
| Editor load calls `purgeInvalid()` (or equivalent) to GC tombstones. | Lazy physical removal — by the time the editor opens, master has had every opportunity to ack the delete and republish without the tombstone. |

## Step-by-step checklist

For each new domain (TT presets, automation, quickwizard, …) you'll touch
roughly the same files. The scene implementation is the worked example.

### 1. Data model

Add the two invariant fields to the entity:

```kotlin
data class TTPreset(
    val id: String,
    /* …existing fields… */
    val lastModified: Long = 0L,
    val isValid: Boolean = true
)
```

Persisted via a `StringNonKey` pref (already true for scenes, TT presets,
automation, …). The JSON serializer round-trips the new fields with `optLong`
/ `optBoolean` defaults so old payloads still parse.

### 2. Repository

The repository is the single editor entry point. Three methods do the work:

```kotlin
fun save(entity: T)              // bump lastModified = now, set isValid = true
fun delete(id: String)           // bump lastModified = now, set isValid = false (no physical removal)
fun purgeInvalid()               // physically drop isValid = false entries
fun get*(...): T?                // filters isValid = true; bypass via private allEntities()
```

`save`/`delete` work on the *raw* (`allEntities()`) list, not the filtered
view, so re-saving an id that was previously soft-deleted revives the slot in
place instead of duplicating it.

The master-side receiver does **not** go through this repository — it does
JSON-level merge directly on the pref, both to avoid an inter-module
dependency from `:plugins:sync` to `:ui` and to preserve incoming timestamps
that `save()` would clobber.

### 3. Wire protocol — new `ClientControlMessage` variant

```kotlin
@Serializable
@SerialName("tt_presets_update")           // wire name — never rename after shipping
data class TempTargetPresetsUpdate(
    val ttPresetsJson: String              // opaque to nssdk; receiver parses
) : ClientControlMessage()
```

Wire-name strings are the contract. Kotlin class names can be refactored
freely after; `@SerialName` cannot.

### 4. Sender wiring

- Extend the domain-specific outbound interface (e.g. `ClientControlSceneSender`)
  with a `sendXxxUpdate(json: String)` method (or create a domain sender).
- Implement in `ClientControlPublisher`: add the new variant to the identifier
  selector `when` — the existing `cmd_<type>_<clientId>` slot pattern works
  unchanged.

### 5. Master-side receiver merge

Add a `onVerifiedXxxUpdate(entry, envelope, message, now)` branch to
`ClientControlReceiver` mirroring `onVerifiedScenesUpdate`:

```kotlin
val incoming = JSONArray(message.xxxJson)
val existing = JSONArray(preferences.get(StringNonKey.Xxx))
// per-id LWW by lastModified: replace existing if incoming is newer,
// add if absent. Tombstones merge as plain data — editor-load purge
// handles physical removal lazily.
if (changed > 0) preferences.put(StringNonKey.Xxx, merged.toString())
```

Writing the pref back triggers the existing
`RunningConfigurationPublisher` debounce, which republishes the merged
state to all paired clients. No extra fan-out plumbing required.

### 6. Client-side publish trigger

Mirror `SceneDefinitionsClientPublisher`:

- `@Singleton` in `:plugins:sync`.
- Active only when `config.AAPSCLIENT`.
- Observes the pref via `preferences.observeChange(StringNonKey.Xxx)`
  (drops initial replay).
- Debounce 2s, then call `sender.sendXxxUpdate(currentJson)`.
- Started/stopped from `NSClientV3Plugin.onStart` / `onStop` alongside
  `runningConfigurationPublisher`.

### 7. UI gating

Every editor screen for a synced domain gets the same gates:

- **Active-state lock** — entities that are currently *in use* (active TT,
  active scene, running automation rule) refuse edit/delete. Domain
  decides what "in use" means.
- **Master-reachable lock** — on AAPSCLIENT only, edit/delete/activate are
  disabled when `masterReachableFlow(nsClient, config, viewModelScope)`
  is `false`. A top banner explains.

`masterReachableFlow` (in `ui/.../scenes/MasterReachableFlow.kt`) is the
shared signal. It combines:
- `NsClient.wsConnectedFlow` with a 5s falling-edge grace.
- `NsClient.lastDevicestatusReceivedAt.freshness(thresholdMs = 9 min, …)`
  for the "master heartbeat" check (client healthy ↔ master alive — pure
  WS-state can't catch a dead master while client–NS stays up).

Reuse `masterReachableFlow` directly; do not duplicate the logic per
viewmodel.

### 8. String resources

Three strings, English-only per CLAUDE.md:

- `xxx_lock_banner_master_offline` — top banner copy.
- `xxx_lock_reason_master_offline` — per-row "Master disconnected" hint.
- `xxx_lock_reason_active` — per-row "stop the running X first" hint.

Or reuse the existing scene strings if the wording works for the new domain.

## Reusable primitives

Already extracted and ready to use:

| Helper | Location | Purpose |
|---|---|---|
| `tickerFlow(periodMs)` | `core/objects/extensions/FlowExtension.kt` | Time-dependent re-evaluation (chip countdowns, staleness ticks). Emits immediately + every `periodMs`. |
| `StateFlow<Long>.freshness(thresholdMs, scope, …)` | same file | "Is this timestamp recent enough right now?" Internal ticker forces re-evaluation on wall-clock progression. `pristine` controls how `0L` is treated. |
| `masterReachableFlow(nsClient, config, scope)` | `ui/.../scenes/MasterReachableFlow.kt` | Composite reachability signal (WS + heartbeat freshness). Reuse as-is. |
| `NsClient.wsConnectedFlow` | `core/interfaces/sync/NsClient.kt` | Live WS-state. |
| `NsClient.lastDevicestatusReceivedAt` | same | Heartbeat timestamp (0L until first batch). |
| `preferences.observe(key)` / `observeChange(key)` | `core/objects/extensions/PreferencesExtension.kt` | Pref state and change events. |
| `persistenceLayer.observeChanges<T>()` | existing | DB row changes. |
| `rxBus.toFlow(EventClass::class.java)` | existing | Non-DB/pref events. |

## NS settings-collection quirks (already worked around)

- **Soft-delete only.** `DELETE` on a settings doc sets `isValid = false`
  server-side; the row stays with its original `date`. Subsequent `PUT`s to
  the same identifier hit "Field date cannot be modified by the client."
  → Always send a constant `DOC_DATE` (1 ms past NS `MIN_TIMESTAMP`) on
  envelopes destined for the settings collection. The real timestamp lives
  in the signed envelope payload.
- **Latest-wins slots.** Stable identifiers like
  `aaps_clientcontrol_cmd_<type>_<clientId>` mean the second PUT overwrites
  the first. For per-edit deltas this is desirable ("user changed their
  mind"); for queue-style commands it isn't. Pick the identifier scheme
  per variant.

## Known accepted trade-offs

These are real edge cases we noticed and *chose* not to engineer around.
If a future domain has tighter requirements, revisit.

| Edge case | Behaviour today | Cost of fixing |
|---|---|---|
| Offline edit during a brief gap between user edit and the 2s publish debounce | Lost if master pushes a snapshot in that window. Locking the UI on `!masterReachable` keeps the window vanishingly small in practice. | Per-entity merge on client receive (instead of full-replace via `applyToPlugin`) — symmetric with master. |
| Wasted echo round-trip | Master push → client `applyToPlugin` → pref change → client publishes same snapshot back → master sees no diff → done. One unnecessary envelope per push. | Track last-published hash on client; skip publish if unchanged. |
| Tombstones in flight | Master republishes its pref as-is until master's editor loads and `purgeInvalid()`s. Clients receive tombstones briefly. They're hidden at display. | Filter `isValid = false` at master's republish step (would mean computing a separate "for export" view of the pref). |
| Concurrent same-entity edit by two clients within seconds | Last `lastModified` wins. Loser sees their edit revert on next master republish. | Optimistic versioning + reject-with-retry — heavy for an edge case that's almost impossible on a typical household setup with sub-second WS push latency. |

## Active-state lock — domain-specific guidance

Pattern is the same across domains, but the "active" definition isn't.
Examples for already-implemented or upcoming work:

- **Scenes**: a scene is active iff its id matches `ActiveSceneManager.activeSceneState.value?.scene?.id`. Implemented.
- **TT presets**: a preset is active iff a TT exists in the DB whose `reason` matches the preset's name (or, if presets get an id field, a matching id). Coming up.
- **Automation rules**: trickier — automations don't have a long-running "active" notion. The lock probably reduces to master-reachable only. Coming up.
- **Profile entries**: active iff `profileFunction.getProfile()?.name` matches. Edits to the active profile arguably *should* be allowed (user wants to tweak now); revisit per-domain UX.

Locking the active entity is mandatory only when editing it mid-flight could
desync runtime state (e.g. deleting a scene's `ChainScene` target while it's
running). Where there's no runtime risk, the lock can be skipped — but
consistency wins for users who learn one mental model.

## Implementation sequence (suggested)

When adding a new domain, do it in this order — each step is testable
independently and a halt anywhere leaves a coherent partial state.

1. Add `lastModified` + `isValid` to the entity. Update serializer.
2. Update repository: `save` / `delete` / `purgeInvalid` / filtered getters.
3. Add the `ClientControlMessage` variant + identifier slot in `ClientControlPublisher`.
4. Add the matching `onVerifiedXxxUpdate` branch in `ClientControlReceiver`.
5. Add the client-side `XxxClientPublisher` singleton; wire start/stop in `NSClientV3Plugin`.
6. **Smoke test:** edit on client A → confirm master receives, applies, republishes; observe on client B.
7. UI gating: inject `NsClient`, derive lock state via `masterReachableFlow`, gate edit/delete buttons, render banner.
8. Editor-load `purgeInvalid()` + display filter for tombstones.
9. Active-state lock per domain semantics.

Each step should compile clean before moving to the next. If you skip the
order, smoke tests get harder to interpret.

## Reference implementations

- **Scenes** (worked example, in tree as of this commit):
  - Model: `core/data/.../Scene.kt`
  - Repository: `ui/.../scenes/SceneRepository.kt`
  - Wire: `core/nssdk/.../ClientControlMessage.SceneDefinitionsUpdate`
  - Sender: `ClientControlSceneSender.sendScenesUpdate`
  - Publisher: `plugins/sync/.../ClientControlPublisher.sendScenesUpdate`
  - Client publisher: `plugins/sync/.../SceneDefinitionsClientPublisher.kt`
  - Receiver merge: `ClientControlReceiver.onVerifiedScenesUpdate`
  - UI gating: `SceneListViewModel.masterOfflineBanner` / `editLockReasons`
  - Editor purge: `SceneListViewModel.init` calls `sceneRepository.purgeInvalid()`
- **Reachability**:
  - `masterReachableFlow` — `ui/.../scenes/MasterReachableFlow.kt`
  - Heartbeat publisher — `NSDeviceStatusHandler.handleNewData`
  - Heartbeat consumer — `NsClient.lastDevicestatusReceivedAt`
