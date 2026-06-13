# Wear precheck unification — remaining entities (plan)

## Context

The wear bolus-confirmation layer in `DataHandlerMobile` (runs on the **master** phone) is being
unified
onto the shared `WizardBolusExecutor` spine so that:

- the **master is the single capping authority** (no local `applyBolusConstraints` in the handler),
- the confirm round-trip is **consume-once by `bolusId`** (no echoed amount a stale watch could
  resend),
- the watch renders the **master-authored `ConfirmationLine` list** (`ConfirmAction.lines`) — the
  exact
  text the phone dialog + every paired client show — via a `role → colour` map in `AcceptActivity`.

**DONE (Option B, on `ns_settings`):** **manual bolus** (`handleBolusPreCheck`) and **eCarbs**
(`handleECarbsPreCheck`) → both route through
`WizardBolusExecutor.prepareBatch(BatchAction.Bolus(...))`

+ `confirm(bolusId)`; the watch renders the colored lines. Compile + unit tests green
  (`DataHandlerMobileWearBolusTest`, 8/8; `WizardBolusExecutorImplTest`, 12/12), device-verified
  round-trip.

**Also DONE since: Temp Target (FULLY unified) + Loop-Status readouts.** Wear TT now routes through
`prepareBatch([BatchAction.TempTarget]) → confirm(bolusId) → applyTempTarget` (the shared apply the
clients
use); `doTempTarget` deleted, `ActionTempTargetConfirmed → (bolusId)`. It records the real reason,
`buildTtLine` localizes the reason (range + reason show on phone/clients/wear), and
`applyTempTarget` gained a
duration-0 → `CANCEL_TT` branch. Separately, the read-only Loop-Status readouts (`targetsStatus`/
`loopStatus`/
`oAPSResultStatus`) were moved off string concatenation onto `ConfirmationLine` lists (
translatable). 3 TT
unit tests; bolus+TT 8/8, executor 12/12, EventData 1/1.

The remaining wear precheck entities each have a **design tradeoff**, so they are deferred here
rather
than migrated blind. None is broken by the Option-B work: they send no `lines`, so they fall through
to
`AcceptActivity`'s fallback `message` branch and render exactly as before.

---

## 1. QuickWizard — `handleQuickWizardPreCheck`

**Already unified:** capping/compute/park via `prepareQuickWizard`; consume-once via
`ActionWizardConfirmed(result.bolusId)` → `confirm(bolusId)`. Only the confirm **text** is bespoke.

**What `result.lines` already covers** (
`BolusWizard.buildConfirmationLines(advisor=false, quickWizardEntry=entry)`,
`core/objects/.../BolusWizard.kt:368`): BOLUS, CARBS (+ carb-time shift), COB (+ slow-absorption),
constraint
warning, record-only warning, alarm INFO line, **and the eCarbs companion line** (read from the
entry, lines
433–443). So the wear `eCarbsMessagePart` + `carbDelayMessagePart` + alarm `!` are all redundant
with `lines`.

**What the wear message has that `lines` does NOT:**

- the **QuickWizard button name** (`quickWizardEntry.buttonText()`, via `quick_wizard_message`),
- the **`result.explanation`** = `explainShort()` (IC / sens / carbs / BG / IOB / trend breakdown).
  The phone WizardDialog renders **only** `lines` (no explanation), so the watch currently shows
  *more* than
  the phone.

**Decision needed (the migration is NOT a pure win):**

- (a) **Drop** the button name + explanation → watch becomes byte-consistent with the phone, but
  loses the
  per-button context + the calc breakdown it shows today. Smallest diff: send
  `lines = result.lines.map{…}`,
  delete the concatenated message.
- (b) **Preserve** them in the shared path → add the button name and/or an explanation block as
  `ConfirmationLine`s (role `NORMAL`/`INFO`) inside `buildConfirmationLines` / a new
  `PrepareResult.Preview.explanationLines`, which then also appears on the **phone dialog + clients
  ** (per the
  "missing info goes to the general path" principle). Bigger, cross-surface change.

**Recommendation:** (b) for the explanation if it's deemed worth showing everywhere; otherwise leave
QuickWizard as-is — it already meets the unification contract (capping + bolusId), and the message
is the
only cosmetic gap. Low priority.

## 2. Manual Wizard — `handleWizardPreCheck`

Sends `ActionWizardResult` (a numeric breakdown: ic/sens/insulinFromCarbs/BG/COB/IOB/trend/…) that
the watch
renders in its **own** `WizardResultActivity` (a rich, watch-native calculator screen), then
`ActionWizardConfirmed(timeStamp)` → `confirm(bolusId)`. Capping + consume-once are already shared (
via
`setPending` + the computed wizard).

**Tradeoff:** moving this to `ConfirmAction.lines` would replace the bespoke `WizardResultActivity`
breakdown
with a flat line list — a deliberate, information-dense watch screen lost for cross-surface text
identity.
**Recommendation:** leave as-is unless there's product intent to retire `WizardResultActivity`. If
pursued,
it's a UI rewrite (watch breakdown → role-tagged lines) + folding the breakdown into the shared
builder.

## 3. Fill / prime — `handleFillPreCheck` / `handleFillPresetPreCheck`

Still local: caps insulin, builds a concatenated message, echoes the amount back via
`ActionFillConfirmed(insulin)`. Deliberately **excepted** from the batch path (Fill is a `PRIMING`
-type bolus,
no carbs, with a chained profile-switch-on-insulin-change side effect).

**Tradeoff:** a `prepareBatch`-style `BatchAction.Bolus(recordOnly=false, …)` doesn't model the
PRIMING event
type or the post-fill profile-switch chaining. **Recommendation:** if unified, add a dedicated
`prepareFill`/`deliverFillBolus` two-step to the executor (the executor already has
`deliverFillBolus`) and a
`bolusId`-based `ActionFillConfirmed` — but this is its own bounded task, not part of the
bolus/carbs batch.

---

## Verification for any of the above (when picked up)

- Unit: extend `DataHandlerMobileWearBolusTest` (drive the handler, capture `EventMobileToWear`,
  assert the
  `BatchAction`/`returnCommand`/`lines`). For executor changes, `WizardBolusExecutorImplTest`.
- Device: trigger the precheck from the watch and confirm via **phone logcat** the `prepareX` call +
  the
  `ConfirmAction{returnCommand: …(bolusId), lines:[…]}` (the watch's final ✓-tap auto-dismisses
  under adb;
  a real finger or the temporary `AcceptActivity` keep-alive tweak is needed to see live delivery).
