# Client Insulin Delivery (remote bolus over NS Client-Control)

A paired AAPSCLIENT (no pump) triggers an insulin bolus that the **master** (has the pump)
actually delivers, over the existing signed NS Client-Control round-trip. Watch-style
**prepare → commit**: the master computes + constraint-caps the dose on its own live state,
hands the **number** to the client user to confirm, then delivers idempotently by a prepared id.

Builds on the ACK round-trip channel (`_docs/CLIENT_CONTROL_ACK_PHASE1.md`) and reuses the
`WizardBolusExecutor` prepare/confirm spine verbatim (the engine was built transport-neutral —
"wear today, client-control next").

## Decisions (user)

1. **QuickWizard only** for the MVP — it already has a master-side `prepareQuickWizard(guid)`. The
   full bolus wizard (would need a new `prepareWizard(inputs)`) is out of scope for now.
2. **Pairing is the opt-in.** No dedicated bolus sub-toggle — remote bolus rides the same
   authorization as every other client-control command (a paired client +
   `NsClientAllowClientControl`
   on the master). The act of pairing + enabling client control is the consent.
3. **Leave** the watch's `rejectIfAapsClient` block (a watch paired to a client still cannot bolus
   through it) — not lifted in this work.

## Flow

1. Client taps a QW button → `MainViewModel.executeQuickWizard(guid)` gains an
   `if (config.AAPSCLIENT)`
   branch → dispatch **`BolusPrepare(guid)`**. (QW *definitions* already sync to the client via the
   Bidirectional `StringNonKey.QuickWizard`, so the buttons already exist there.)
2. **Master** receiver → `wizardBolusExecutor.prepareQuickWizard(guid)` (the same call the watch
   uses):
   gates (running mode, pump initialized, BG/COB/profile), **constraint-caps**, parks the
   consume-once
   `pending` slot, returns `Preview(insulin, carbs, explanation, bolusId)`. **No raw dose ever
   leaves
   the client** → client staleness can't inflate a dose (master is authoritative, like
   `InsulinActivate`).
3. Master acks the prepare with the Preview in the new signed `payload`.
4. Client renders the **master's own color-coded wizard confirmation** — the master runs
   `BolusWizard.buildConfirmationLines` during prepare and ships the `List<ConfirmationLine>` in the
   payload
   (as `ConfirmationLineDto`); the client shows it through the **same** `EventShowDialog.OkCancel` +
   `GlobalDialogHost` the master's wizard uses. Shared builder + shared renderer; only the delivery
   transport differs. (Lines are master-locale — the same locale for a single-user master+client.)
5. User confirms → client dispatches **`BolusCommit(bolusId)`**.
6. Master receiver → `confirm(bolusId, Sources.NSClient, onError)` → drains the slot → delivers via
   the
   shared `executeBolus`; super-bolus / eCarbs ride along automatically from the parked entry.
7. Commit acked Ok/Failed. **On `Unconfirmed`, NEVER shown as delivered** → reconciled from the
   synced
   `BS` record (the existing `markMasterUnreachable` + re-pull on a missed ack).

## The one channel extension

The ACK carries status only today. Add an **optional, HMAC-signed `payload: String?`** to
`AckEnvelope`
(default null; included in `canonicalString` so a forged dose is not believable — same threat model
as a
forged "Ok"). The prepare handler returns `{insulin, carbs, explanation, bolusId, lines}` JSON in
it —
`lines` is the master's `buildConfirmationLines` output (as `ConfirmationLineDto`s) so the client
renders the
identical confirmation. The commit ACK uses the existing Ok/Failed/Expired terminal unchanged.

## Safety (all map to existing mechanisms)

- **Idempotent commit** — two independent layers: the consume-once `bolusId` slot (`confirm` drains
  it →
  a retried/duplicate commit gets `NoPending` → no double-dose) **and** the envelope
  monotonic-counter
  replay rejection at the receiver.
- **Short-lived prepare** — 8s `validUntil`; the parked slot is the real guard. A superseded/stale
  prepare
  (master's own user/watch prepared in between, overwriting the single global slot) → commit gets
  `NoPending` → surfaced as `Rejected(NoPendingBolus)` → user re-prepares. Never a wrong bolus.
- **Master authoritative** — only `guid` (prepare) + `bolusId` (commit) travel; the dose is
  computed +
  capped on the master and re-capped again by `commandQueue.bolus`.
- **Opt-in = pairing** — master bolus handlers gated behind the same `NsClientAllowClientControl` +
  signed-pairing authorization as all client-control commands.
- **Client gating** — QW bolus buttons disabled (with `MasterOfflineBanner`) when `masterReachable`
  is
  false (the prepare can't round-trip).
- **`Unconfirmed` ⇒ reconcile, never "delivered"** — a bolus that times out must not be shown as
  applied;
  the delivered `BS` syncs back to the client.

## Touch points

- `core/nssdk/.../clientcontrol/ClientControlMessage.kt` — `BolusPrepare("bolus_prepare", guid)`,
  `BolusCommit("bolus_commit", bolusId)`.
- `core/nssdk/.../clientcontrol/AckEnvelope.kt` (+ `payload`, into `canonicalString`),
  `utils/ClientControlCrypto.kt` (`signAck`/`verifyAck` cover `payload`).
- `core/interfaces/.../clientcontrol/ClientControlActionDispatcher.kt` (`Command.BolusPrepare`/
  `BolusCommit`),
  `ActionProgress.kt` (a preview-carrying terminal + `FailureReason.NoPendingBolus`/
  `BolusComputeFailed`).
- `plugins/sync/.../clientcontrol/ClientControlRoundTrip.kt` (dispatch map + parse the prepare
  payload),
  `ClientControlPublisher.kt` (identifier classify), `ClientControlReceiver.kt` (
  `onVerifiedBolusPrepare`
  → `prepareQuickWizard`; `onVerifiedBolusCommit` → `confirm`; `AckOutcome.payload`).
- `ui/.../compose/main/MainViewModel.kt` — `executeQuickWizard` AAPSCLIENT branch + the confirm
  dialog.

## Phasing

- **Phase A (this work):** the channel payload + the 2 commands/handlers + master prepare/commit
  wiring +
  the client QW trigger and confirm dialog — a full end-to-end remote-bolus path for QuickWizard.
- **Phase B (later):** dedicated remote-bolus opt-in UI if wanted, masterReachable button gating
  polish,
  full bolus-wizard (`prepareWizard`), QW-extras display parity, watch-on-client unblock.
