# Client Insulin Delivery — Phase B (all bolus entry points remote)

Builds on Phase A (`_docs/CLIENT_INSULIN_DELIVERY.md`), which made the **QuickWizard WIZARD-mode
button** deliver remotely (client → master `BolusPrepare`/`BolusCommit` over the signed round-trip,
master computes + caps + delivers once). Phase B extends the same spine to **every** bolus/insulin
entry point so a paired client becomes a thin remote and the master is the single executor.

## Decisions (maintainer, locked)

1. **Wizard / Insulin / Treatment** → a reachable paired client **defaults to deliver-via-master**;
   record-only stays as a choice/fallback (NOT forced).
2. **Carbs** → routed through the master too (a carbs entry then needs the master online). Reuses
   the fixed-amount path with `insulin = 0`.
3. **Fill** → three actions, split:
    - **prime/fill bolus** → visible-but-**disabled** on a client (physical at-the-pump action,
      never remote);
    - **site change** → logging allowed (CarePortal record, syncs);
    - **insulin / cartridge change** → logging allowed; the insulin-type change rides the existing *
      *insulin-activate** client→master path.
4. **Record-only / logging** → **kept on both sides** (master + client). Phase B only removes the
   `|| config.AAPSCLIENT` **forcing**; the record-only branch + toggle stay.

## Two flavours (the map showed)

- **Wizard** dose is computed from live BG/COB/IOB/trend (`BolusWizard.doCalc`) → master must *
  *recompute on its own state** (send the inputs).
- **Insulin / Treatment / Carbs** are **fixed** user amounts → master only **caps** + delivers (send
  the amounts).

## Architecture (reuses Phase A: `BolusCommit`,

`BolusPreview`, round-trip, confirm dialog, consume-once slot)

- **Wire (`:core:nssdk`)** — new messages:
  -
  `WizardPrepare(bg, carbs, percentage, directCorrection, carbTime, useBg, useTt, useTrend, useIob, useCob, notes, profileName?)`
    - `FixedBolusPrepare(insulin, carbs, eventType, notes, timestamp)` — covers Insulin (carbs 0),
      Treatment (both), Carbs (insulin 0).
    - `BolusCommit` + `BolusPreview` reused unchanged.
- **Executor (`:core:interfaces` + `:implementation`)**:
    - `PendingBolus` gains `mode: BolusMode` (WIZARD / FIXED) + the fixed-mode `eventType`.
    - `prepareWizard(inputs)` and `prepareFixedBolus(insulin, carbs, eventType, notes, ts)`.
      `prepareQuickWizard` stays.
    - `confirm()` dispatches by parked mode → `deliverWizardBolus`/`deliverBolusAdvisor` (WIZARD) or
      `deliver(...)` (FIXED). Shared `executeBolus` core unchanged → one idempotent path.
    - **Key risk:** `prepareWizard` must build a `BolusWizard` from the client's raw inputs on the *
      *master's** live profile/BG/COB. Extract a shared `buildWizard(inputs)` the
      `WizardDialogViewModel` and the master both use.
- **Sync (`:plugins:sync`)** — `onVerifiedWizardPrepare` / `onVerifiedFixedBolusPrepare` (commit
  handler shared); round-trip dispatch + publisher classification.
- **UI (`:ui`)** — Wizard / Insulin / Treatment / Carbs route remote on a reachable client (OK →
  prepare → master's confirm → commit); keep the record-only toggle. Remove only the `|| AAPSCLIENT`
  forcing (keep `cantDeliver/!pumpInit`). Fill: prime disabled, logging allowed.

## Phasing

- **B.1** — executor generalization (`BolusMode`, `prepareWizard`, `prepareFixedBolus`,
  mode-dispatch `confirm`) + both wire commands + receiver/round-trip/publisher + the **Bolus Wizard
  ** dialog rewired (incl. the `buildWizard` extraction). *(in progress)*
- **B.2** — Insulin + Treatment + Carbs dialogs (UI only, reuse `FixedBolusPrepare`).
- **B.3** — Fill tweaks + drop the `AAPSCLIENT` record-only forcing.

## B.1 checklist

- [x] `WizardPrepare` message (`:core:nssdk`) + publisher classification  *(`FixedBolusPrepare`
  moved to B.2 — fixed amounts are simpler + share nothing with the wizard recompute)*
- [x] `Command.WizardPrepare(inputs)` (`:core:interfaces` dispatcher, carries
  `WizardBolusExecutor.WizardInputs`) + round-trip dispatch
- [x] `PendingBolus` carries `carbTimeMinutes`/`notes` so a manual wizard (no QuickWizard entry)
  delivers identically via `confirm`; `confirm` reads the parked fields
- [x] `prepareWizard(inputs)` — master recomputes `BolusWizard.doCalc` on its **active** profile +
  live temp-target + COB/IOB, constraint-caps, parks, returns the signed-payload `BolusPreview` (
  lines + advisor)
- [x] `onVerifiedWizardPrepare` (receiver) → signed `BolusPreview`; commit reuses
  `onVerifiedBolusCommit`
- [x] compiles both flavors + impl/sync test sources; executor test ctor updated (
  `bolusWizardProvider` mock)
- [x] **`WizardDialogViewModel.confirmRemote()`** — on a client, OK → `run(WizardPrepare(inputs))` →
  master's confirm (`EventShowDialog`, master's lines, no "recorded only") →
  `run(BolusCommit(asAdvisor))`; advisor fork master-driven. `WizardDialogScreen` branches to it on
  `isAapsClient` (skips the local confirmation). Dropped `|| config.AAPSCLIENT` from
  `forcedRecordOnly`.
- [x] receiver `wizardPrepareAcksOkWithSignedPreviewPayload`; all suites green (sync/impl/ui/nssdk),
  both flavors compile
- Deferred (noted): manual-wizard **eCarbs** not carried over the wire; **stored-profile** selection
  not honored (master uses active); executor `prepareWizard` unit test (dose logic is
  `BolusWizard.doCalc`, already covered; plumbing mirrors `prepareQuickWizard`); dedupe
  `confirmRemote` ↔ `MainViewModel.confirmWizardBolus`; super-bolus N/A (Compose wizard has none).

**B.1 DONE** (uncommitted; real-device test pending). The "recorded only" the user saw on a client
wizard is gone — it now shows the master's color-coded confirmation and the master delivers.
