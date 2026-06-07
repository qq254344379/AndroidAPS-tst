# Phase 1 — Round-Trip ACK for Client-Control

**Goal:** turn the one-way client→master Client-Control channel into a request/response with a
two-step acknowledgement, so that pressing "execute" on a client can wait for the real result and
close like a local execution — while staying honest when the result can't be confirmed.

**Scope of Phase 1:** build and prove the protocol + UX on **one pilot feature (insulin activation)**.
The centralized execution interface that removes the `if (config.AAPSCLIENT)` branches across
ViewModels is **Phase 2** and is out of scope here. The master-side ack is written **generically for
all command types** (one place), but only the insulin path is wired on the client in Phase 1.

---

## 0. What already exists (reused, not rebuilt)

| Capability | Where | Reused for |
|---|---|---|
| Per-client, per-type command docs `aaps_clientcontrol_cmd_<type>_<clientId>` | `ClientControlPublisher.kt:91-115` | Ack docs follow the same per-client scheme |
| `SignedEnvelope{clientId,counter,timestamp,type,payload,signature}` + `canonicalString()` | `core/nssdk/.../clientcontrol/SignedEnvelope.kt` | `counter` = correlation id; ack reuses the signing pattern |
| HMAC-SHA256 sign/verify, constant-time compare, ±5min skew | `core/nssdk/.../utils/ClientControlCrypto.kt:60-88` | Ack is signed/verified with the **same shared secret** |
| Monotonic `counter` replay protection (`counterReceived`) | `AuthorizedClientsRepository` (`bumpLastSeen`/`markActive`) | Idempotency on duplicate delivery — already correct |
| Master verify+dispatch pipeline, per-message handlers that compute a result | `ClientControlReceiver.verifyAndAck()` + `onVerified*` `:120-338` | We hook the result into an ack write |
| WS settings dispatch by identifier prefix | `NSClientV3Service.kt:292-309` → `handleClientControlSettingsEvent` | Add a **client-side** branch for the ack identifier |
| Upsert write to NS settings | `NSAndroidClient.updateSettings(identifier, doc)` (soft-delete only) | Master writes the ack doc; overwrite-in-place |
| `masterReachable` StateFlow | `NSClientV3Plugin.kt:471` | Early-abort the wait if the master drops mid-flight |

**Consequence:** identity, signing, per-client addressing, replay protection, and the master dispatch
are all done. Phase 1 adds: (1) a validity field, (2) the master writing two signed ack docs,
(3) a client-side coordinator that awaits them, (4) a pending modal, (5) the insulin pilot wiring.

---

## 1. Protocol changes

### 1.1 Command validity (TTL)

Add `validUntil: Long` (absolute epoch ms) to `SignedEnvelope` and include it in `canonicalString()`
(so it is tamper-proof). The `ns_settings` branch is unreleased, so evolving the envelope is free.

- **Master** (`verifyAndAck`): after the existing skew check, if `now > envelope.validUntil` →
  do **not** execute; write a terminal ack `status = Expired`; bump `counterReceived` (consume it so
  it can't be retried later); stop. This tightens the existing ±5min coarse drop into a per-command,
  definitive "expired" answer the client gets immediately instead of waiting.
- **Client** derives its wait deadline from `validUntil` (see §3). The client sets `validUntil =
  now + ttlMs`; default `ttlMs = 45_000`. Keep the existing ±5min skew as the **outer** sanity bound.
- **Clock skew note:** `validUntil` is set on the client clock and enforced on the master clock. The
  existing ±5min skew tolerance already absorbs normal phone clock drift, and 45s ≪ 5min, so a
  command rejected purely on `validUntil` due to skew would also be near the skew boundary — acceptable.
  The **client's own wait** (§3) runs on the client's own clock from send-time, so it is skew-immune
  regardless. Do not block on resolving NTP-level skew in Phase 1.

### 1.2 Ack envelope (new)

New model `core/nssdk/.../clientcontrol/AckEnvelope.kt`:

```
@Serializable
data class AckEnvelope(
    val clientId: String,      // echoes the command's clientId (who this ack is for)
    val commandCounter: Long,  // correlation = the command envelope's counter
    val phase: AckPhase,       // Executing | Done
    val status: AckStatus,     // Pending | Ok | Failed | Expired   (Pending only while Executing)
    val reason: String? = null,// human-readable failure/expiry reason
    val timestamp: Long,
    val signature: String      // hex HMAC-SHA256, same secret as commands
) {
    fun canonicalString() = "$clientId|$commandCounter|$phase|$status|${reason ?: ""}|$timestamp"
}
enum class AckPhase { Executing, Done }
enum class AckStatus { Pending, Ok, Failed, Expired }
```

Signing/verification: add `signAck`/`verifyAck` to `ClientControlCrypto` (or a generic
`sign(secret, canonical)` already exists — reuse it over `AckEnvelope.canonicalString()`).

### 1.3 Ack identifier

Per-client, **single identifier** (single-in-flight is enforced by the modal, see §3):

```
aaps_clientcontrol_ack_<clientId>
```

Add `IDENTIFIER_ACK_PREFIX` next to the existing prefixes in `ClientControlPublisher`. One ack doc per
client, overwritten in place: Master writes `Executing` then overwrites with `Done`. Document count is
bounded by paired-client count — nothing to GC (consistent with NS overwrite-only / no hard delete).
Stale acks from a previous command are ignored by `commandCounter` correlation.

---

## 2. Master side

### 2.1 Write the two-step ack

In `ClientControlReceiver`, after a command passes verify (sig + counter + skew) and is **not** expired:

1. **Before dispatch:** write ack `{phase=Executing, status=Pending, commandCounter=envelope.counter}`.
2. **Dispatch** via the existing `onVerified*` handler.
3. **After dispatch:** map the handler's result → `AckStatus` and write ack
   `{phase=Done, status=Ok|Failed, reason}` (overwrites the Executing doc).

Refactor the `onVerified*` handlers (`:189-338`) to **return** a small result instead of only logging:

| Handler | Today | Return for ack |
|---|---|---|
| `onVerifiedInsulinActivate` `:249-264` | logs `applied: Boolean` | `Ok` if applied else `Failed("no active profile" / "invalid iCfg")` |
| `onVerifiedSceneStart` `:205-211` | logs `SceneAutomationResult` | map `Success`→`Ok`, else `Failed(result.tag())` |
| `onVerifiedSceneStop` `:213-241` | logs result incl. `ChainCompleted.failedCount` | `Ok` / `Failed(...)` |
| `onVerifiedPreferencesUpdate` `:276-338` | logs applied list | `Ok` (note: best-effort, partial-apply is still Ok) |
| `onVerifiedHello` `:189-203` | pairing handshake | **no ack** (handshake, not a user action) |

This makes ack generic across all command types in one place; the existing `addLog` lines stay.

### 2.2 Counter / replay ordering

Bump `counterReceived` once the command is **accepted** (before execute), so a failed execution is not
re-run and a duplicate delivery is skipped — current behavior, preserved. On a replay (`counter <=
counterReceived`) keep the current "skip, leave doc" behavior; **do not** re-ack in Phase 1 (no retry —
see §5).

### 2.3 Ack writer

Master writes the ack with `updateSettings(ackIdentifier, doc)` using the same doc wrapper as
`RunningConfigurationPublisher.putSettings` (`date`/`utcOffset`/`app`/`schemaVersion` + `ack` field).
Gate stays under the existing `BooleanKey.NsClientAllowClientControl` (already checked before the
receiver runs, `NSClientV3Plugin.kt:379-385`).

---

## 3. Client side — the round-trip coordinator

New `plugins/sync/.../clientcontrol/ClientControlRoundTrip.kt`, exposed via an interface in
`core/interfaces` so `:ui` can use it **without** depending on `:plugins:sync`:

```
// core/interfaces/.../clientcontrol/ClientControlActionDispatcher.kt
interface ClientControlActionDispatcher {
    fun dispatch(message: ClientControlMessage, ttlMs: Long = 45_000): Flow<ActionProgress>
}
// core/interfaces/.../clientcontrol/ActionProgress.kt
sealed interface ActionProgress {
    data object Sending : ActionProgress                       // uploading the command
    data object MasterExecuting : ActionProgress               // got the Executing ack
    data object Applied : ActionProgress                       // Done/Ok → close like local
    data class  Rejected(val reason: String?) : ActionProgress // Done/Failed | Expired | send failed — NOT applied, safe to say so
    data class  Unconfirmed(val reason: String?) : ActionProgress // no Done before deadline / connection lost — state unknown
}
```

`ActionProgress` deliberately distinguishes **Rejected (definitely not applied)** from **Unconfirmed
(unknown — reconcile via normal state sync-back)**. Only `Unconfirmed` is remote-specific; Phase 2's
local path emits only `Sending?→Applied/Rejected`.

Coordinator flow:

1. Emit `Sending`. Call existing `ClientControlPublisher.publish(message)` (which signs with the next
   `counter` and uploads). Capture that `counter` as the correlation key.
   - `NotPaired` → emit `Rejected("not paired")`, complete. (definitely not applied)
   - `PublishFailed(r)` → emit `Rejected("upload failed: $r")`, complete. (definitely not applied)
   - `Success` → continue, register pending `{counter, deadline = validUntil + Δ}` (Δ ≈ propagation
     margin, e.g. 5_000ms, for the Done ack to travel back).
2. Await acks for this `counter` from a `SharedFlow<AckEnvelope>` fed by the WS handler (§4):
   - `Executing` ack → emit `MasterExecuting`.
   - `Done/Ok` → emit `Applied`, complete.
   - `Done/Failed` → emit `Rejected(reason)`, complete.
   - `Done/Expired` → emit `Rejected("expired before master applied it")`, complete.
3. **Timeout** at `deadline` with no `Done` → emit `Unconfirmed(...)`, complete.
4. **Early abort:** while waiting, observe `masterReachable`; if it flips false → emit
   `Unconfirmed("connection lost while waiting")` immediately (don't burn the full TTL).
5. Verify every incoming ack's **signature** and that `commandCounter == ourCounter`; ignore stale/
   mismatched acks (e.g. lingering ack from a previous command).

**Single-in-flight:** the coordinator rejects a second `dispatch` while one is outstanding for this
client (the modal blocks the UI anyway). This is what lets the ack identifier be one-per-client.

---

## 4. Client-side ack reception (WS + fallback)

In `NSClientV3Service.kt:292-309` settings branch, add a client-side route **before** the existing
master `IDENTIFIER_PREFIX` route:

```
config.AAPSCLIENT && identifier == "aaps_clientcontrol_ack_${myClientId}" ->
    clientControlRoundTrip.onAckDoc(docJson)   // parse → verify sig → emit to SharedFlow
```

(The master `IDENTIFIER_PREFIX` route is gated by `NsClientAllowClientControl` which is off on a
client, so ack docs won't fall into the master receiver — but branch explicitly for clarity.)

Polling fallback: on `deadline` approach (or once), the coordinator may `getSettings(ackIdentifier)`
once to catch an ack missed by WS before declaring `Unconfirmed`. Optional in Phase 1.

---

## 5. Explicitly out of scope for Phase 1

- **Automatic retry / resend.** On `Unconfirmed` we **inform** the user and rely on state sync-back;
  we do not auto-resend. (Resend correctly would require re-publishing the *same* `counter` and the
  master re-emitting the prior ack on replay — deferred.)
- **Per-domain execution interfaces** and removing the `if (config.AAPSCLIENT)` branches — **Phase 2**.
- **Scenes / automation / preferences client-side wiring** to the coordinator — Phase 2 (master-side
  ack is generic and covers them already; only the client consumption + modal is insulin-only here).
- **Watch and other entrypoints** — Phase 2.

---

## 6. Pilot wiring (insulin) — minimal, VM branch retained

`InsulinManagementViewModel.executeActivation()` (`:483-508`), client branch only:

- Replace the fire-and-forget `clientControlInsulinSender.sendInsulinActivate(...)` +
  immediate snackbar with: collect `dispatcher.dispatch(ClientControlMessage.InsulinActivate(json))`
  and drive a UI progress state.
- Master branch unchanged (synchronous `createProfileSwitchWithNewInsulin` + `refreshData()`).
- New UI state on the screen: `clientControlProgress: ActionProgress?` drives the modal.

Modal `ui/.../compose/clientcontrol/ClientControlPendingDialog.kt` (driven by `ActionProgress`):

| Progress | Modal |
|---|---|
| `Sending` | spinner "Sending…" |
| `MasterExecuting` | spinner "Master is applying…" |
| `Applied` | dismiss + close the activation dialog/screen (mirror local), brief Info snackbar |
| `Rejected(reason)` | error state with `reason`, dismissable; do **not** close as success |
| `Unconfirmed(reason)` | "Couldn't confirm — this may or may not have applied; the screen will update when it syncs", dismissable |

Cancel/back = **stop waiting** (transition to `Unconfirmed`), with copy that says the command may still
apply — not "cancelled".

---

## 7. Files touched

**core/nssdk**
- `clientcontrol/SignedEnvelope.kt` — add `validUntil`; include in `canonicalString()`.
- `clientcontrol/AckEnvelope.kt` — **new** (model + `canonicalString`).
- `utils/ClientControlCrypto.kt` — ack sign/verify (or reuse generic `sign` over the ack canonical).

**core/interfaces**
- `clientcontrol/ClientControlActionDispatcher.kt` — **new** interface.
- `clientcontrol/ActionProgress.kt` — **new** sealed type.

**plugins/sync**
- `clientcontrol/ClientControlPublisher.kt` — `IDENTIFIER_ACK_PREFIX`; set `validUntil` when signing;
  (optional) extract a small ack-writer helper or add one.
- `clientcontrol/ClientControlReceiver.kt` — validity drop + ack-Expired; write Executing/Done acks;
  refactor `onVerified*` to return a result.
- `clientcontrol/ClientControlRoundTrip.kt` — **new** coordinator (impl of the dispatcher interface);
  ack `SharedFlow`; timeout from `validUntil`; `masterReachable` early-abort; single-in-flight.
- `services/NSClientV3Service.kt` — client-side ack identifier route → coordinator.
- DI module — bind `ClientControlActionDispatcher` → `ClientControlRoundTrip`.

**ui**
- `insulinManagement/InsulinManagementViewModel.kt` — client branch drives `ActionProgress`.
- `insulinManagement/...Screen.kt` — host the modal.
- `compose/clientcontrol/ClientControlPendingDialog.kt` — **new** modal.

---

## 8. Test plan (unit)

- `ClientControlCrypto`: ack sign → verify round-trips; tampered ack fails; wrong secret fails.
- `ClientControlReceiver`:
  - valid insulin command → writes `Executing` then `Done/Ok`; `createProfileSwitchWithNewInsulin`
    called once.
  - handler returns failure → `Done/Failed` with reason; not retried.
  - `now > validUntil` → no execution, `Done/Expired`, `counterReceived` bumped.
  - replay (`counter <= counterReceived`) → no execution, no new ack.
  - bad signature → no ack, no execution (unchanged).
- `ClientControlRoundTrip`:
  - `Success` → `Sending`→`MasterExecuting`→`Applied` on Ok ack.
  - Failed ack → `Rejected(reason)`; Expired ack → `Rejected`.
  - no Done before deadline → `Unconfirmed`.
  - `masterReachable` flips false mid-wait → `Unconfirmed` early (before deadline).
  - `NotPaired` / `PublishFailed` → immediate `Rejected`, no wait.
  - ack with mismatched `commandCounter` ignored.

---

## 9. Open decisions (small, can default)

1. **Default `ttlMs`** — proposed `45_000`. Configurable later; hardcode in Phase 1.
2. **Propagation margin Δ** — proposed `5_000`.
3. **Polling fallback** before declaring `Unconfirmed` — include or rely on WS only? (Lean: WS only in
   Phase 1; add the single `getSettings` poll if real-device testing shows missed acks.)
4. **Ack `reason` wording** surfaced to the user vs. log-only (lean: short user-facing reason for
   Rejected; generic copy for Unconfirmed).
