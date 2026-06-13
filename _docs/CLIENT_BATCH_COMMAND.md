# Client batch command — `BatchPrepare` + `BolusCommit` (SHIPPED)

> Supersedes the earlier one-step `BatchApply` sketch (scrapped). The contract is client→master with the
> **master** as the sole authority that caps the dose and authors the confirmation — so a one-step
> fire-and-commit can't work: the client must show (and the user must approve) the master's exact numbers
> first. End state = a **two-step** `prepare → commit`, the same shape as the recompute path's `WizardExecutor`
> (see `_docs/GENERAL_EXECUTION_PATH_PLAN.md`).

## Facade — `BatchExecutor` (role-transparent)

`core/interfaces/.../bolus/BatchExecutor.kt`:
- `prepare(actions: List<BatchAction>, source, label): ActionProgress` — ask the master to cap + build the
  MERGED confirmation lines, park the bundle, and return `ActionProgress.Prepared(bolusId, lines)`.
- `commit(bolusId, source, label): ActionProgress` — apply the parked bundle exactly once.

`BatchExecutorImpl` is the only role branch:
- **client** → the signed round-trip `dispatcher.run(BatchPrepare(actions))` then `dispatcher.run(BolusCommit(bolusId))`,
  gated on `masterReachable` (offline → `Rejected.NotReachable`, nothing applied);
- **master** → local `WizardBolusExecutor.prepareBatch(actions)` then `confirm(bolusId)`.

The dialog is role-agnostic: build the `BatchAction` list → `prepare` → render the returned `lines` → on the
user's OK, `commit`. Both roles render the master's identical confirmation.

## `BatchAction` (domain; wire form = `BatchActionDto` in `:core:nssdk`)

At most one of each per batch:
- **Bolus** — fixed insulin/carbs (capped; `recordOnly` persists as-given, *uncapped*, with its `iCfg`).
- **TempTarget** — `startOffsetMinutes` from now (an eating-soon/activity TT starts now even when the bolus is back-dated).
- **ProfileSwitch** — `%` / timeshift / duration; `profileName` null → the active profile, non-null → that named profile from the master's store.
- **RunningMode** — `RM.Mode` (+ duration for the temporary modes); the master re-validates it's a legal transition.

## Master `prepareBatch` + `confirm` (`WizardBolusExecutorImpl`)

- `prepareBatch` caps a delivery (caps only reduce = safe), keeps a record-only as given, validates the
  profile-switch / running-mode up front, parks the bundle under a `bolusId`, and returns the merged `lines`.
- `confirm` applies in **decision-B order**: a target-RAISING TT (hypo/activity) first + unconditional; the
  Bolus; a target-LOWERING TT (eating-soon) only if the bolus was accepted; then the ProfileSwitch / RunningMode.
- **Idempotency**: per-client counter replay gate + receiver `commandMutex` + the executor's atomic
  consume-once slot keyed by `bolusId` (a re-sent commit finds the slot drained → never a double dose).

## Ack semantics

`commit` acks the **synchronous** outcome only (queue-accepted vs mode-rejected) — identical to a master-local
bolus. An eventual async pump failure surfaces the same way a master-local bolus does (notification / no record
syncs back), not via this ack. The decision-B ordering is the safety net precisely because the eventual pump
outcome is unknown at ack time.

## Entry points on this path

Insulin / Carbs / Treatment dialogs, manual Profile Switch (`ProfileManagementViewModel`), Running Mode
(`RunningModeManagementViewModel`), QuickWizard **INSULIN** / **CARBS** (`MainViewModel.executeFixedBatch`),
the QuickLaunch profile switch, and the wear handler (`DataHandlerMobile`). The **recompute** path (manual
wizard + quick-wizard WIZARD mode) is the sibling `WizardExecutor` — see `_docs/GENERAL_EXECUTION_PATH_PLAN.md`.
