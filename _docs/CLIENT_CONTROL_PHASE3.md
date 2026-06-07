# Phase 3 — Liveness Probe, Reconnect Self-Healing & Generic Preference Round-Trip

**Status:** Phase 1 validated on device. **3.1 (PING-PONG + `fresh`-term generalization) and 3.2
(reconnect self-healing) are IMPLEMENTED and real-device tested OK** (`ns_settings`; both app
flavors;
unit tests green). **3.3 (generic preference round-trip) remains design only — next up.**

This phase builds on **Phase 1** (`_docs/CLIENT_CONTROL_ACK_PHASE1.md` — the two-step signed ACK +
command TTL, piloted on insulin activation; committed). It is **independent of** the other planned
"Phase 2" (per-domain execution interfaces that remove the `if (config.AAPSCLIENT)` branches and
extend the round-trip to scenes/watch — see the Phase 1 doc §Phase 2). The two can proceed in either
order; this doc covers a different axis: **reachability/liveness and config self-healing**, then the
**generic preference round-trip**.

Everything here came out of a design discussion (2026-06-07). The point of writing it down is so the
*reasoning and the rejected alternatives* aren't lost.

---

## Problem context

1. **Slow "master offline" banner.** On a client, `masterReachable` (NSClientV3Plugin) requires a
   *fresh* master devicestatus heartbeat (≤9 min, fail-closed at boot). A freshly-opened client
   shows
   the red "master offline" banner until the master's next **live** devicestatus push — up to a loop
   cycle — even when the master is online right now. There is no way to actively confirm liveness.

2. **Config changes can be silently missed**, with three distinct failure points:
    - **Client missed a live WS `settings` push** during a blip that didn't trigger a full
      reconnect →
      client holds stale config, no catch-up runs.
    - **Master changed config while its own NS link was down.** Verified gap:
      `RunningConfigurationPublisher`
      publishes once at `start()` then only on change triggers; `putSettings` on failure just **logs
      **
      (no retry, no dirty flag, no reconnect re-publish), and `nsAndroidClient ?: return` silently
      drops
      the publish. So an offline-master edit is stranded in local prefs until the next unrelated
      change.
    - **Auto-pruned doc.** If NS has `API3_AUTOPRUNE_SETTINGS` enabled and the master is idle past
      the
      TTL, `settings/aaps` can be deleted; a client GET then returns null (handled gracefully —
      stale,
      not blank — but never refreshed).

3. **Optimistic preference sync.** Today a client edit to a bidirectionally-synced pref is stored
   locally immediately and pushed fire-and-forget (`PreferencesClientPublisher` + LWW). The UI shows
   the value before the master (the authority) has accepted it. We want the UI to reflect only what
   the
   master actually applied.

---

## The three mechanisms (converged design)

Each closes a **distinct, verified** failure point. They are small and independent.

| Concern                                                           | Mechanism                                                                           | Side            |
|-------------------------------------------------------------------|-------------------------------------------------------------------------------------|-----------------|
| Is the master alive *now*? (clear the banner fast)                | **PING-PONG** liveness probe                                                        | client → master |
| Client missed a config push                                       | **Client self-re-GET** of `settings/aaps`+`aaps-state` (reuse `LoadSettingsWorker`) | client          |
| Master edited config while offline / doc pruned / start-before-NS | **Master re-publish cold+hot on its WS rising edge**                                | master          |
| Client edit should show only when the master applied it           | **Generic preference round-trip** (pessimistic)                                     | client → master |

### 3.1 PING-PONG (liveness)

- New `ClientControlMessage.Ping` (signed, rides the existing Client-Control channel). The master's
  handler is a **no-op that returns `AckOutcome(Ok)`** — the ack *is* the pong.
- The pong is the fast, authenticated, master-exclusive "alive now" signal we concluded (back in the
  opening heartbeat discussion) was the right answer — *not* scraping BG/treatments.
- **`masterReachable` `fresh`-term generalization:** today fed only by `lastDevicestatusReceivedAt`.
  Generalize it to "last authenticated master signal" = devicestatus **or** a Ping pong **or** a
  config republish. Safe because all three are master-exclusive (and the pong is signed). The pong (
  or
  any live settings push) bumps the liveness clock → banner clears in ~1–2 s.
- **Triggers:** client start; entering an offline-gated screen **while WS is up** (rate-limited /
  debounced). Pointless when WS is down (can't reach master) → rely on reconnect.
- The Ping must be sendable **while `masterReachable` is false** (that's the whole point) —
  publishing
  is gated on pairing, not reachability. Don't let any future "block sends when offline" suppress
  it.

### 3.2 Reconnect self-healing (config freshness)

**Key insight:** config freshness is a *pull*; only liveness needs a *probe*. The client can
re-fetch
the current doc itself — it does **not** need the master to re-push for the common cases.

- **Client side (mostly exists):** `LoadSettingsWorker` already does an *unconditional* GET of
  `settings/aaps` + `aaps-state` on every `executeLoop` (which runs on (re)connect). Add the same
  trigger on client start / offline-screen-open so a *missed-push* or *silent-master-change* is
  pulled
  without master involvement (the updated doc is already on NS).
- **Master side (NEW):** in `RunningConfigurationPublisher.start()`, the
  `NSClientV3Plugin.wsConnectedFlow`
  rising-edge trigger → `publishCold(); publishHot()` (debounced) is now the **initial *and*
  reconnect**
  publish — there is intentionally **no blind publish at plugin start** (the NS connection isn't
  ready
  then, so it would just fail silently). A `StateFlow` delivers its current value on subscribe, so
  an
  already-connected start publishes immediately. Poll-mode masters (`NsClient3UseWs` off) have no WS
  connect event, so they keep a start-time publish as a fallback. This:
    - recovers an **offline-master edit** that failed to publish (the verified gap),
    - **recreates an auto-pruned doc** from the master side,
    - fixes **`start()`-before-NS-ready** (initial publish failed, nothing retried).
      Republishing unchanged config is harmless: clients get a WS push of the same values;
      `applyCold`/
      `applyHot` are idempotent; per-key LWW no-ops (same `SyncedPrefModified`) → no echo storm.

### 3.3 Generic preference round-trip (the big one)

Goal: on a client, a synced-pref edit shows in the UI **only once the master has applied it**.

**STATUS — GENERALIZED to ALL synced keys (`ns_settings`, unit-tested, real-device test pending):**
every Bidirectional local edit on a client now routes through the optimistic confirmed round-trip
(modal + master ACK) instead of fire-and-forget. `PreferencesClientPublisher` accumulates changed
keys
over a **500 ms settle window** (collapses a slider drag / a burst into one batch) and ships them as
**one batched `runPreferenceEdit`**; the collector suspends until the modal resolves, so further
edits
queue into the next round-trip — never two concurrent pref round-trips (this dissolves the
single-in-flight contention and the shared `preferences_update` identifier collision). Modal UX:
**1.5 s minimum visible** (measured from shown, not additive). The fire-and-forget pref path
(`ClientControlPreferencesSender` usage) is removed from the publisher.

**Prerequisite done:** programmatic client-side `put`s of Bidirectional keys were eliminated so the
modal only ever shows for genuine user edits — `MainApp` migrations for `GeneralSimpleMode`,
`ApsUseDynamicSensitivity`, `ApsDynIsfAdjustmentFactor`, and `migrateTempTargetPresets` are now
gated
`!config.AAPSCLIENT` (clients adopt these via sync from the master). Audit confirmed the rest are
UX-driven, master-only, or already `putRemote`.

**Done since:** master **always-republish on receipt** —
`ClientControlReceiver.onVerifiedPreferencesUpdate`
calls `RunningConfigurationPublisher.requestColdRepublish()` so the client converges to the master's
authoritative value even when LWW dropped the push as a no-op. The dead fire-and-forget pref path
(`ClientControlPreferencesSender` + binding + `ClientControlPublisher.sendPreferencesUpdate`) is
removed.
Definition **edit screens** are confirmed already gated — QuickWizard/Scenes/TempTarget/Insulin all
use
`masterEditingEnabled()`/`isPlayMode = (PLAY || !editingEnabled)` to hide editor+toolbar+save and
show
`MasterOfflineBanner` when the master is unreachable, so nothing reachable saves while offline.

**Done since:** insulin (Phase-1) modal min-visible parity — both modals now share
`ClientControlActionDispatcher.MIN_MODAL_VISIBLE_MS` (1.5 s).

**Still deferred:** per-key applied/superseded ACK (so the modal can say "overridden" vs "applied" on
LWW loss — only meaningful now that always-republish landed).

History — the incremental spike was: ONE key (`OverviewBolusPercentage`) → chosen over pessimistic
for the
spike because, given the always-republish rule, it delivers the same UX (modal covers the control;
republish auto-corrects a superseded value) with none of the pessimistic blockers (no per-overload
`put` surgery, no read-after-write, no circular DI). Wiring:
`ClientControlActionDispatcher.Command.PreferenceEdit`

+ `preferenceEditProgress`/`dismissPreferenceEditProgress`;
  `ClientControlRoundTrip.runPreferenceEdit`
  (reuses the Phase-1 round-trip + ack); `PreferencesClientPublisher` routes the spike key through
  it
  (immediate, excluded from the batch); app-level `ClientControlPendingDialog` host in
  `ComposeMainActivity`. Deferred until generalization: master **always-republish on receipt** (
  needed
  for LWW-no-op / supersede; spike's single editor never hits it), the **shared `preferences_update`
  identifier collision** (batch vs round-trip — fine while only one key is round-tripped), per-key
  applied/superseded ack, and pessimistic mode for any key with a local client-side effect.
  Real-device
  test pending.

**Chosen model (for generalization) — pessimistic ("don't store on client `put`, apply on master
broadcast"):**

- `put` gains a client/master split (the natural seam — `put` on a synced key *already* means "sync
  this", it's not a plain setter):
    - **master:** store immediately (unchanged).
    - **client + Bidirectional + user-origin:** do **not** store; send the value, show a modal with
      a
      tentative/unconfirmed copy, and **adopt the master's broadcast** as the stored value.
- This makes concurrency/LWW correct *for free*: two clients edit at once → master picks a winner
  via
  LWW → republishes → both adopt the winner and converge. The client never holds an un-reconciled
  local
  value, so there's nothing to roll back. The "loser" simply sees the winning value — honest.
- **Confirmation = the master's republish of that key** (reuse the sync-back channel), not a bespoke
  ack payload. To guarantee one always arrives (so the modal never hangs on a no-op LWW-drop), the
  **master must always echo/republish the key's authoritative value on receipt of a pref command,
  even
  when LWW drops it as a no-op.** Then: win → your value; lose → other value; no-op → current
  value —
  uniformly resolved, no per-key applied/superseded flags needed.

**Rejected alternative — optimistic (store + roll back on reject):** less invasive (keeps `put`
durable, hooks the existing post-store `onLocalSyncedWrite` chokepoint), but it shows the value
before
confirmation, which defeats the goal. Kept here only as the fallback if the pessimistic blockers
below
prove too costly.

---

## SEND_CONFIGURATION — considered and **dropped** (don't re-propose)

Early idea: a client command that makes the master republish cold+hot, doubling as liveness + config
refresh. Dropped because each of its jobs is better served elsewhere:

- liveness → **Ping pong** (don't need a republish to know the master is alive),
- missed-push / silent-change → **client self-re-GET** (the doc is already on NS; pull it),
- offline-master-edit / auto-prune → **master re-publish on WS rising edge** (§3.2).

So SEND_CONFIGURATION has no unique remaining job. If some future need appears, reconsider — but it
is
intentionally not in this plan.

---

## Blockers for the preference round-trip (from code analysis, 2026-06-07)

Most were retired during the discussion; recorded so they aren't re-litigated.

1. **Value capture — SOLVED by construction.** The current publisher serializes by *re-reading
   storage* (`PreferencesClientPublisher.serialize` → `preferences.get(key)`), which only works
   because
   `put` stores first. Putting the divert *inside* `put` (with the value in hand) sends the actual
   value directly — no re-read. The `if (client) send else store` split is exactly this.
2. **Read-modify-write / observe-after-write — MOSTLY RETIRED.** Editors do `put(blob)` then re-read
   (`SceneRepository.saveScene` → `getScenes`; `TempTargetManagementViewModel` → `loadData`;
   `ConfigBuilderImpl` observes the key). The **modal serializes edits** (no second edit reads a
   stale
   blob), and synced screens are already moving to **observe** the key (memory note: "non-pref
   screens
   must observe synced keys themselves"). Residual = an **audit**: any screen still doing imperative
   `put`→`get`/`loadData` (not observing) must move to observe so it refreshes from sync-back.
3. **LWW-supersede vs "Applied" — SOLVED** by the pessimistic model + "always echo authoritative
   value" rule (§3.3). No per-key ack flags needed.
4. **Pre-store interception point.** "Don't store" must skip the actual SharedPreferences write in
   each
   `put` overload (~5: Boolean/String/Int/Double/UnitDouble). The existing single chokepoint
   (`onLocalSyncedWrite`) runs *after* the write, so it's not reusable for the pessimistic skip.
   Mechanical, not hard, but it's per-overload.
5. **Non-UX writes to Bidirectional keys.** The "always from UX" assumption is *nearly* true on the
   client once filtered to local `put`: `ActivePlugin*` regen is `!AAPSCLIENT` (master-only),
   automation last-run persistence is the master loop, `InsulinConfiguration` bootstrap uses
   `putRemote`. **The one real client-side offender: `MainApp.migrateTempTargetPresets()` does a
   plain
   `put(TempTargetPresets, …)` at first-run** → would fire a spurious round-trip during startup.
   Fix:
   seed via `putRemote` or gate it. Also note the assumption is *fragile* — a future programmatic
   `put`
   on a synced key would silently trigger a round-trip; keep the divert rule explicit/documented.
6. **`put` is synchronous; modal from a non-Compose layer.** `put` returns `Unit` on the main
   thread,
   can't await — fire the round-trip + wait asynchronously. Surface the modal via the existing
   **`EventShowDialog` + `GlobalDialogHost`** (RxBus; the sanctioned pattern — *not*
   `LocalSnackbarHostState`). `PreferencesImpl` (`:implementation`) gains `RxBus` + the dispatcher
   dep.

---

## Two kinds of synced prefs (why `put` is the right seam)

Both bottom out at `put` on a synced key, which is why hooking `put` covers them in one place:

- **Keys** — simple values (Boolean/String/Int/UnitDouble) edited from settings screens.
- **Definitions** — QuickWizard, Automation, Scenes, Insulin config, persisted as serialized JSON in
  `StringNonKey` keys carrying `SyncSpec(Bidirectional)`. A definition edit is a single `put` of the
  whole blob → one round-trip, one modal, no batching question.

---

## Crystallized design decisions (carry forward)

- **`wantsAck` envelope flag** (already shipped in Phase 1): only round-trip commands get acked;
  fire-and-forget (scenes/prefs today) don't. Pref round-trip commands will set `wantsAck`.
- **Pref ack semantics differ from insulin's.** Insulin = `Ok/Failed/Expired`. Prefs = "here's the
  authoritative value," delivered via a **forced master republish** (reuse sync-back), not a new ack
  payload.
- **`masterReachable.fresh` generalized** to any authenticated master signal (devicestatus | pong |
  config republish).
- **Self-healing is symmetric:** client re-GETs on its connect; master re-publishes on its connect.

---

## Suggested ordering

1. **Validate Phase 1 insulin round-trip on device** (the gate for everything here).
2. **PING-PONG + reconnect self-healing** (§3.1, §3.2). Small, high UX value (banner), and it
   exercises
   the "pong-as-liveness" / `fresh`-term wiring that 3.3 also leans on. Do this before 3.3.
3. **Generic preference round-trip** (§3.3) — the larger rework; addresses blockers §4, §5, §6
   above.

---

## Open questions

- Ping cadence / debounce, and master-side rate-limit on republish (reconnect flapping).
- Whether the offline-screen-open trigger should fire Ping only, or Ping + client self-re-GET
  together.
- Exact modal UX for the pref round-trip (reuse `ClientControlPendingDialog`? it's `ActionProgress`
  -shaped).
- Audit list of synced screens still doing imperative `put`→read (blocker §4.2).
