# Multi-action apply path — `BatchApply` (DESIGN, hardened)

> Revised after an adversarial design review (`harden-batch-design`) + the "collapse into
> single-action batches, default on master too" decisions.

## Problem (the *real* defect)

On a client, a multi-action dialog applies its actions on **two different devices**:

- **Carbs dialog**: carbs → master (`FixedBolusPrepare`); TT (activity/eating-soon/hypo) → **local
  DB write on the client** (`insertAndCancelCurrentTemporaryTarget`, unconditional —
  `CarbsDialogViewModel:438`).
- **Insulin dialog**: bolus → master; eating-soon TT → **local DB write on the client** (
  `InsulinDialogViewModel:391`).

So the master's loop **doses without the eating-soon/hypo target the user intended**, until NS sync
converges (and even then timing/attribution differ). That is the live correctness defect.

The same dialogs are *also* not atomic **on the master** (TT and dose are two independent
`appScope.launch` blocks) — so this is the multi-action apply path for **both roles**, not a
client-only feature.

> Note: the "two NS-doc commands race the monotonic counter / shared-ACK" problem is **not live
today** (the TT isn't a command yet). It would only become real if we migrated the TT onto its own
`TempTargetSet` command — which is exactly what bundling avoids. The bundle's value is
*correctness* (one device, one transaction), and it sidesteps that race by construction.

## Design — one command, role-transparent

Split by the only question that matters — **does the master recompute the dose?**

- **No** (FIXED amounts + records + TTs) → **`BatchApply(actions)`** — *one-step*, fire-and-commit.
  Subsumes `FixedBolusPrepare` **and** `RecordTreatment`. A single bolus is a one-action batch; a
  pen-log is `[Bolus(recordOnly)]`; carbs+TT is `[Bolus(0,carbs), TempTarget]`.
- **Yes** (wizard / quick-wizard) → existing **`WizardPrepare`/`BolusPrepare` → `BolusCommit`** (
  two-step, keeps the consume-once slot + `BolusPreview`). Unchanged; no embedded TT.

**Why one-step for FIXED:** the master only *constraint-caps* fixed amounts (caps can only **reduce
** = safe) — it never recomputes. So the client confirms its own number **once** (the dialog), sends
one command, the master re-caps (a no-op when prefs are synced) + applies + acks. This kills the
double-confirm wart, needs **no consume-once slot** (idempotency = per-client counter + the receiver
`commandMutex`), and dissolves the earlier fire-and-ack-vs-prepare tension.

**Role-transparent facade** (`BatchExecutor.apply(actions): BatchResult`): client → the signed
`BatchApply` round-trip; master → the local executor — same pattern as `InsulinActions`/
`TempTargetActions`. **The dialog is role-agnostic** (build action list → confirm → `apply`); no
`if (AAPSCLIENT)` in the dialog. The apply logic lives **once** in the executor; a client's
`BatchApply` lands in that same executor on the master.

## Actions (`BatchActionDto` — flat, type-tagged via a `type` discriminator)

- **`Bolus`** =
  `insulin, carbs, carbsTimeOffsetMinutes, carbsDurationHours, recordOnly, notes, iCfgJson?` —
  exactly `prepareFixedBolus`'s shape **plus `recordOnly`** (deliver vs. log) and `iCfgJson` (
  record-only's logged insulin). **No eCarbs fields** (the FIXED path doesn't process them; the
  Carbs dialog models "extended" as a single `carbsDurationHours`). **No advisor** (FIXED never
  triggers `needsBolusAdvisor`). At most **one** per batch.
- **`TempTarget`** = `reason, lowMgdl, highMgdl, durationMinutes, startOffsetMinutes` — *
  *`startOffsetMinutes` is required**: an eating-soon/activity TT starts **now** even when the bolus
  is back-dated (decoupled from bolus event time, matching today's local `now()` behavior). At most
  **one** per batch (the dialogs enforce TT mutual-exclusivity; two TTs would silently cancel each
  other via insert-and-cancel — master `applyBatch` MUST reject >1 TempTarget).
- *(future, Fill: `TherapyEvent`, `ProfileSwitch`.)*

## Master-side `applyBatch(actions, source = NSClient)`

Extract two reusable cores from the existing code (don't call `prepareFixedBolus`/`confirm` — they
self-park / early-return / are single-bolus):

- `capFixed(insulin, carbs) → (cappedInsulin, cappedCarbs, eventType, lines)` — the cap +
  eventType + line-building of `prepareFixedBolus`, **without** its no-action early-return and *
  *without** parking.
- `deliverFixed(insulin, carbs, carbsTime, carbsDuration, eventType, recordOnly, notes, iCfg)` — the
  FIXED delivery body (`insulin==0 && carbs>0` → `deliverECarbs`; else `deliver`; `recordOnly` → the
  record path). Called by both `applyBatch` and the existing `confirm()`/single-action handlers so
  it isn't duplicated.
- TT apply → the **bare** `persistenceLayer.insertAndCancelCurrentTemporaryTarget(...)` call (the
  dialog-free path `onVerifiedTempTargetSet` uses, **not** `TempTargetActions` which pops a dialog),
  `source = Sources.NSClient` (deliberate, intended attribution change vs the local path).

**Apply order** (per Decision B): a **target-raising** TT (hypo, activity) applies **first,
unconditionally**; then the Bolus delivers; then a **target-lowering** (eating-soon) TT applies *
*only if the Bolus succeeded** (queue-accepted, or there's no insulin to fail —
carbs-only/record-only). Never apply an eating-soon TT before a deliver-bolus that could
mode-reject. (A batch carries ≤1 TT, so it's raising-first **or** conditional-after, never both.)

**Idempotency** is the per-client **counter replay gate + `commandMutex`** (one-step → no slot).
Neither `insertAndCancelCurrentTemporaryTarget` nor the bolus persist is *independently*
idempotent (Room auto-id insert), so the counter dedup is **load-bearing** — do NOT add a path that
bypasses it. Tests: a double-delivered `BatchApply` → exactly one TT row + one bolus.

## Ack — honest about what it guarantees

`BatchApply` acks **`Applied`/`Failed`** with the **synchronous** outcome only: queue-accepted vs.
immediately mode-gate-rejected. It does **not** report eventual pump success (the bolus is async on
`commandQueue`; `confirm`/`deliver` return before the pump finishes, and the 8 s round-trip TTL
can't await it) — **identical to today's single-action bolus**. An async pump failure surfaces the
same way as for a master-local bolus (notification / no bolus record syncs back), not via this ack.
No preview payload (one-step).

## Resolved decisions

- **A — ack = SYNCHRONOUS outcome only** (queue-accepted vs mode-rejected), same as the existing
  bolus. An eventual pump failure surfaces like a master-local bolus (no bolus record syncs back /
  notification), not via this ack. Records-first/conditional ordering (B) is the real safety net
  precisely because the eventual bolus outcome is unknown at ack time.
- **B — eating-soon TT is conditional on the bolus.** See Apply order above: target-raising (
  hypo/activity) TT first + unconditional; the Bolus delivers; a target-lowering (eating-soon) TT
  applies only if the Bolus succeeded (or no insulin to fail). On an insulin mode-reject the
  eating-soon TT is **skipped** — avoids the loop chasing a low target with no insulin.
- **C — offline → BLOCK the whole batch.** The facade gates on `masterReachable`;
  `!masterReachable` → reject + tell the user, applying **nothing** (pure SSOT, consistent with the
  record-only insulin's offline block). The offline eating-soon TT no longer applies locally —
  accepted as the cost of one atomic transaction.

## Implementation checklist (compile-forced by exhaustive `when`s)

- nssdk: **1** new `ClientControlMessage.BatchApply` variant (`@SerialName`); **1**
  `BatchActionDto` (flat, `type`-tagged). *(Wizard messages unchanged.)*
- interfaces: **1** `Command.BatchApply`; **1** `BatchExecutor` facade (`apply`).
- round-trip: **1** dispatch branch (`Command.BatchApply → Message`).
- publisher: **1** identifier branch (same `IDENTIFIER_CMD_PREFIX+type`; per-type latest-wins is
  correct).
- receiver: **1** `when` arm → `onVerifiedBatchApply` → `applyBatch`.
- executor: `applyBatch` + the extracted `capFixed`/`deliverFixed` helpers.
- dialogs: Carbs/Insulin build the action list + call the role-transparent facade. **The TT MUST
  move OUT of the local `appScope.launch` INTO the action list** (else applied twice).

## Channel selection by composition (degenerate cases)

The dialog picks the channel by what's checked: **records-only** (TT only, no carbs/bolus) → keep
the existing single `TempTargetSet` fire-and-ack; **bolus/carbs only** → a one-action `BatchApply`;
**bolus/carbs + TT** → a multi-action `BatchApply`. Empty batch → short-circuit (mirror
`hasAction()`).

## Rollout: collapse, but phase it

End-state is the collapse you asked for — `BatchApply` subsumes `FixedBolusPrepare` +
`RecordTreatment`. **Phase the migration**: ship `BatchApply` *beside* the proven single-action
commands (additive `@SerialName`s, additive `when` arms — zero risk to shipped paths), migrate
Carbs → Insulin one viewmodel method at a time, and **delete `FixedBolusPrepare`/`RecordTreatment`
only once `BatchApply` has real-device mileage.**

## Validated by the review (no change needed)

Slot-drain (drain-first) idempotency model; `insertAndCancelCurrentTemporaryTarget` is dialog-free +
headless-safe; in-flight is released across the human confirm (parked state covered by the 10-min
TTL, not by holding the dispatcher); carbs-only → `deliverECarbs` event-type convention;
`BolusPreview` shape (wizard path) is reused verbatim; coexist-then-subsume.
