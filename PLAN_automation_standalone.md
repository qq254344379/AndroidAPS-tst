# Migration Plan: Automation Plugin → Standalone Feature

Status: DRAFT for review. No code changes yet.

## Locked decisions

- **A** — System reminders (bolus/eat): master-only, objectives-independent, no extra gating.
- **B(i)** — Client `userAction` buttons hidden/disabled; no execution on client.
- **C** — Dynamic non-plugin permission source feeding the existing badge→sheet; refreshes on
  foreground (MVP, no immediate in-editor refresh).
- Execution master-only; client = editing + definition sync (scenes model).
- **Keep the `Automation` interface** as the stable public face — it already insulates ~10
  cross-module callers *and* NS sync (`ConfigExportImport`), so the blast radius stays small.

## Target shape

- `AutomationRuntime` `@Singleton` in `plugins/automation`, implements `Automation`, owns its
  `CoroutineScope` + loop + state. **Not** a `PluginBase` (in-place conversion of
  `AutomationPlugin`,
  not a parallel class).
- Two generic "non-plugin" provider sets to replace what the machinery did by iterating
  `getPluginsList()`: `PermissionProvider` and `PreferenceScreenProvider`.
- New nav route + `ManageBottomSheet` entry for the UI.

---

## Phase 1 — Non-plugin infrastructure (core + impl). DONE ✅ (additive, zero behavior change, verified compiling).

Done: `PermissionProvider` interface (core/interfaces); `@Multibinds Set<PermissionProvider>` in
`ImplementationModule.Bindings`; `PluginStore` injects the set and unions it into
`collectMissingPermissions` (dynamic, via `isPermissionMissing`) + `collectAllPermissions`. Empty
set
today → no-op. Verified: `:implementation` and `:app` compile (Hilt graph assembles).

1. `core/interfaces/.../plugin/PermissionProvider.kt` (new):
   `interface PermissionProvider { fun requiredPermissions(): List<PermissionGroup> }`.
2. `implementation/.../plugin/PluginStore.kt` (`collectMissingPermissions` ~311,
   `collectAllPermissions` ~328): inject the providers and union their (dynamic) groups into both
   results. Surfaces automatically via the existing badge→`PermissionsSheet` (re-evaluated on every
   foreground in `ComposeMainActivity.refreshOnResume()`).
   IMPORTANT: inject as `dagger.Lazy<Set<PermissionProvider>>`, NOT eager — `AutomationRuntime` (a
   provider) transitively depends on `ActivePlugin` (= PluginStore), so eager injection forms a
   Dagger
   dependency cycle. This only surfaces at the Hilt/javac step (`assembleFullDebug` /
   `compileFullDebugJavaWithJavac`), NOT at `compileFullDebugKotlin` — always run the full assemble
   after DI-graph changes.

NOTE (settings subscreen): NO new provider needed. A generic, non-plugin route already exists —
`AppRoute.PreferenceScreen` keyed by subscreen `key`, resolved by `findScreenDef(key)` which already
searches BOTH `BuiltInSearchables` and plugin screens. Automation's `PreferenceSubScreenDef`
(key `"automation_settings"`) is registered in `BuiltInSearchables` → searchable + navigable for
free.
(Earlier draft's `PreferenceScreenProvider` set is dropped.)

Risk: low.

---

## Phase 2 — Convert `AutomationPlugin` →
`AutomationRuntime`. DONE ✅ (with Phase 3, verified compiling + tests green).

Done: `AutomationPlugin.kt` → `AutomationRuntime.kt`, dropped `PluginBaseWithPreferences` (now
`@Singleton … : Automation, PermissionProvider`); `onStart/onStop` → `start/stop`; loop + RxBus
subs +
LocationService gated behind `if (config.APS)` (client = load/edit/persist/sync only);
LocationService
now reactive via `_events` + `usesLocationTrigger()`; `requiredPermissions()` dynamic
(`config.APS && usesLocationTrigger()`);
`preferences.registerPreferences(AutomationStringKey::class.java)`
in `init`; removed `pluginDescription`/`composeContent`/`specialEnableCondition`/
`getPreferenceScreenContent`
and the unused `uel`/`profileRepository`/`sceneApi` ctor params. In-module refs renamed
(`TriggerBTDevice`, `AutomationComposeContent`, `AutomationStateHolder`). Tests migrated:
`AutomationPluginTest`→`AutomationRuntimeTest` (+ dynamic-permission cases), timer tests +
`TriggerTestBase` updated. `:plugins:automation` + `:app` compile; automation unit suite green.

### Original step list:

File: `plugins/automation/.../AutomationPlugin.kt` → `AutomationRuntime.kt`.

1. Drop `PluginBaseWithPreferences`; add `@Singleton`; keep `: Automation`. Implement
   `PermissionProvider`.
2. Lifecycle: `onStart()`→`start()`, `onStop()`→`stop()` (plain functions). Move scope/loop/sub
   bodies.
3. Master gating: wrap the `processActions` loop launch AND `LocationService` startup in
   `if (config.APS)`. On client, `start()` wires only `_events` StateFlow + persistence.
4. LocationService becomes reactive (replaces unconditional `deferredStart.start{}`): collect
   `_events`; start when an enabled event contains a `TriggerLocation`, stop when the last
   disappears.
   Master-only.
5. Permissions: `requiredPermissions()` returns location groups only when
   `config.APS && (any enabled event uses TriggerLocation)`.
6. Pref-key registration: replace lost `ownPreferences` auto-register with
   `preferences.registerPreferences(AutomationStringKey::class.java)` in `init`.
7. Settings subscreen: keep the `PreferenceSubScreenDef` body (key `"automation_settings"`), supply
   icon directly (no `pluginDescription.icon`); register it in `BuiltInSearchables` (Phase 4), not
   via
   a plugin hook.
8. Remove `specialEnableCondition`, `specialShowInListCondition`, `pluginDescription`,
   `requiredPermissions` override, `composeContent`. Keep `events`, `schedule*`,
   `removeAutomation*`,
   `processEvent`, `processActions`, `findEventById`, `syncedKeys`, `reloadInternalState`,
   `loadFromSP`/`storeToSP`.
9. Keep `(loop as PluginBase).isEnabled()` inside `processActions` — Loop is still a plugin.

Risk: high (heart of the feature).

---

## Phase 3 — DI rewire + eager start. DONE ✅ (done together with Phase 2 — compilation-coupled).

Done: `AutomationModule` binds `Automation` → `AutomationRuntime` +
`@Binds @IntoSet PermissionProvider`;
`PluginsListModule` `@IntoMap @IntKey(250) … PluginBase` binding removed (key retired); `MainApp`
injects `AutomationRuntime` and calls `automationRuntime.start()` after
`configBuilder.initialize()`.

### Original step list:

1. `plugins/automation/.../di/AutomationModule.kt`: keep `@Binds Automation` → `AutomationRuntime`.
   Add `@Provides @IntoSet PermissionProvider` (→ runtime).
2. `app/.../di/PluginsListModule.kt`: delete
   `@Binds @IntoMap @IntKey(250) … AutomationPlugin → PluginBase`.
3. `app/.../MainApp.kt` (~after `configBuilder.initialize()`, line 222):
   `@Inject lateinit var automationRuntime: AutomationRuntime` + call `automationRuntime.start()`.

Risk: medium. Automation no longer in the plugin list.

---

## Phase 4 — UI entry + settings rehome. DONE ✅ (verified compiling + tests green).

Done: `AppRoute.AutomationList`; `AutomationContentRoute` host in `AppNavGraph` (mirrors
`PluginContentRoute`, content from `automationRuntime.composeContent()`, gear → `PreferenceScreen`
route `"automation_settings"`); `AutomationRuntime.composeContent()` factory (re-added `uel`/
`profileRepository`/`sceneApi` ctor deps + fixed the 3 test sites).
`ElementType.AUTOMATION_MANAGEMENT`
added to all 4 `ElementTypeStyle` whens + new `automation_management_desc` string (core/ui).
`ManageBottomSheet` tile; `ComposeMainActivity` injects `AutomationRuntime`, threads it to
`AppNavGraph`, maps `AUTOMATION_MANAGEMENT` → `AutomationList`. `BuiltInSearchables.automation`
subscreen (key `automation_settings`) + registered for search. `AllPreferencesScreen` switched from
the plugin-interface lookup to `builtInSearchables.automation` (master-gated).
`:plugins:automation` +
`:app` compile; automation unit suite green. Internal-route model + 3-dots overflow + BackHandlers
carried over untouched.

### Original notes:

UI is a REHOST, not a rewrite. The screen tree is self-contained: internal `sealed AutomationRoute`
(List/Edit/EditTrigger/MapPicker) dispatched by `AutomationComposeContent.Render()`, with per-route
`BackHandler`s (AutomationComposeContent.kt:221/296/358) that pop the internal stack. The 3-dots
overflow (`AutomationOverflow` = Run/Remove/Sort, AutomationComposeContent.kt:524) is
SCREEN-provided
inside `ToolbarConfig.actions` — the host never added an overflow. `AapsTopAppBar` just renders the
supplied `actions`. Automation does NOT use `LocalPluginNavigationRequest`. So everything carries
over; we only reproduce the host (Scaffold + `AapsTopAppBar` driven by `toolbarConfig`) and feed it
two callbacks: `onNavigateBack` + `onSettings`.

UI dependency/lifecycle approach — Approach 1 (minimal, chosen for MVP): `plugins/automation` is
dagger-android only (no Hilt VMs). Do NOT add Hilt. `AutomationRuntime` exposes
`fun composeContent(): ComposablePluginContent` (the old descriptor-lambda body with its
`injector.get(...)` lookups). `AutomationStateHolder` stays `remember`-scoped inside `Render()` (
same
as today). Approach 2 (Hilt `@HiltViewModel` for config-change survival) = optional later polish.

Steps:

1. `AppRoute.kt`: add `AutomationList`. (Keep internal-route model — do NOT split Edit/EditTrigger/
   MapPicker into nav destinations; they share a mutable `workingEvent` + dirty/discard guards.)
2. `AppNavGraph.kt`: add `composable(AutomationList.route)` that copies the `PluginContentRoute`
   host
   (Scaffold + `AapsTopAppBar` + `toolbarConfig` state) but gets content from
   `automationRuntime.composeContent()` instead of `plugin.getComposeContent()`. Supply
   `onNavigateBack = { navController.safePopBackStack() }` and `onSettings = { withProtection(...) {
   navController.navigate(AppRoute.PreferenceScreen.createRoute("automation_settings")) } }`.
   KEEP the generic `PluginContent` route + `ComposablePluginContent` interface — still used by
   Loop,
   Autotune, OpenAPS, Calibration, Objectives, BgSource, etc. Automation just stops registering
   `composeContent`.
3. `ElementType.kt`: add `AUTOMATION_MANAGEMENT(category = MANAGEMENT, searchable = true,
   protection = PREFERENCES)`. Then update EVERY exhaustive `when(ElementType)` (compiler-forced):
    - `ElementTypeStyle.color()`, `.icon()` (reuse `IcAutomation`), `.labelResId()`,
      `.descriptionResId()`  (4 sites in `core/ui/.../ElementTypeStyle.kt`)
    - `ComposeMainActivity.navigateToElement()` → `navigate(AutomationList.route)`
    - `ManageBottomSheet` grid → add `ManageGridItem(AUTOMATION_MANAGEMENT, ...)`
    - searchable provider → register `SearchableItem.Dialog(AUTOMATION_MANAGEMENT)`
    - New strings `automation_management` / `automation_management_desc`.
4. Settings subscreen: register Automation's `PreferenceSubScreenDef` (key `"automation_settings"`)
   in `BuiltInSearchables` → searchable + reachable via the existing generic `PreferenceScreen`
   route
   (no new route). Gear retargeted in step 2.
5. `AllPreferencesScreen.kt:67`: replace `getSpecificPluginsListByInterface(Automation::class.java)`
   with the `BuiltInSearchables` subscreen, so automation settings still appear in the
   All-Preferences
   tree. (Verify how AllPreferences aggregates built-in vs plugin subscreens.)
6. Client gating of the overflow: the "Run automations" item (and userAction surfaces, Phase 5)
   calls
   `processActions()` — hide/disable on client (`config.AAPSCLIENT`).
7. Entry shown on all flavors (enables client editing); execution already gated in Phase 2.

Sanity check: automation leaves `getPluginsList()` → disappears from the Config/plugins screen and
from search `SearchableItem.Plugin` (intended — replaced by the management tile). Confirm nothing
else
assumes it's in the plugin list.

MODULE-DEP CHECK — RESOLVED ✅ (no new project dependency). `BuiltInSearchables` is in the `ui`
module (`ui/src/main/kotlin/app/aaps/ui/search/BuiltInSearchables.kt`). Everything the subscreen
needs
is already on `ui`'s dependency graph:

- key `StringKey.AutomationLocation` lives in `core/keys` (NOT in `plugins/automation`) — `ui`
  already
  `implementation(project(":core:keys"))` and already imports `StringKey`.
- title `R.string.automation` (core/ui) + the location-mode strings (core/keys) — both already deps.
- icon `IcPluginAutomation` (core/ui) — already a dep.
  Recipe: add `val automation = PreferenceSubScreenDef(key = "automation_settings", titleResId =
core.ui.R.string.automation, items = listOf(StringKey.AutomationLocation), icon = IcPluginAutomation)`
  and `SearchableItem.Category(automation)` to `getSearchableItems()`. The `AutomationStringKey`
  enum
  in `plugins/automation` is NOT referenced by the subscreen, so it stays put.

Risk: medium.

---

## Phase 5 — Hide
`userAction` surfaces on client (decision B(i)). DONE ✅ (verified: assembleFullDebug + tests green).

Done — defense in depth: RUNTIME GUARDS in `AutomationRuntime` (`processActions()` +
`processEvent()`
early-return on `!config.APS`) guarantee no execution on client regardless of caller; plus
`executionEnabled` (= `config.APS`) for UI. UI hiding: `DataHandlerMobile.sendUserActions()` sends
an
empty tile list on client; `ScenesViewModel` items empty on client; `QuickLaunchConfigViewModel`
`availableAuto` empty on client (+`Config` inject); `QuickLaunchResolver.isValid` returns false for
automation actions on client (+`Config` inject) — covers the overview toolbar; `AutomationOverflow`
hides "Run automations" on client (`showRun = plugin.executionEnabled`). Editing/Remove/Sort stay
available on client.

### Original step list:

1. `DataHandlerMobile.sendUserActions()` (~1645): gate on `config.APS`.
2. `QuickLaunchConfigViewModel.kt` (~80): exclude automation actions when `config.AAPSCLIENT`.
3. `ScenesViewModel.kt` (~157) / `MainViewModel.executeConfirmableAction`: drop automation items /
   block `processEvent` on client.

Risk: low–medium.

---

## Phase 6 — Trigger rewiring (narrow interfaces).

1. `BtConnectionSource` (new, internal to module): `val btConnects: MutableList<EventBTChange>`
   (or `drainBtEvents()`), implemented by `AutomationRuntime`. `TriggerBTDevice.kt` injects this
   instead of concrete `AutomationPlugin`.
2. If `TriggerLocation` reads runtime/location state, give it a similarly narrow interface. Do not
   widen the public `Automation` interface.

Risk: low.

---

## Phase 7 — Tests.

1. `AutomationPluginTest.kt` → `AutomationRuntimeTest.kt`: construct the `@Singleton` directly.
2. `DataHandlerMobileUserActionTest.kt`: adjust for client gating.
3. New: dynamic `PermissionProvider`; loop does not start on client.

---

## Phase 8 — Client editing + client→master sync (scenes parity). LAST PHASE, separate follow-up PR (PR 3). Deferred until PRs 1–2 land + verified.

1. New `ClientControlMessage.AutomationDefinitionsUpdate` (wire type).
2. `AutomationDefinitionsClientPublisher` (`config.AAPSCLIENT`-guarded) observing
   `AutomationStringKey.AutomationEvents`, sending via existing `clientControl` sender — mirror
   `SceneDefinitionsClientPublisher`.
3. `ClientControlReceiver.onVerifiedAutomationUpdate` on master: LWW merge into
   `AutomationStringKey.AutomationEvents`, then `automationRuntime.reloadInternalState()`.
4. Optional: master-reachability gate on the client editor (scenes' `MasterReachableFlow`).

Risk: high, additive.

---

## Suggested PR slicing

- PR 1: Phases 1–3 (infra + conversion + DI). Master parity; automation gone from plugin list; no UI
  entry yet → land behind verification.
- PR 2: Phases 4–6 + Phase 7 tests. Full standalone parity, client = view/edit-local only.
- PR 3 (LAST): Phase 8 (client editing → master sync). Only after PRs 1–2 are merged and verified.

## Code-review fixes (post Phase 5)

- #1/#7 LocationService stuck-off after a late permission grant:
  `LocationServiceHelper.startService`
  now returns `Boolean`; `updateLocationService()` only latches `locationServiceRunning` when the
  service actually started; `processActions()` reconciles the service each tick (retries after a
  grant); flag `@Volatile` + method `@Synchronized`.
- #2 provider-change restart routed through `deferredStart` + the flag (no background-FGS crash).
- #3 `BuiltInSearchables.automation` search entry gated on `config.APS` (matches AllPreferences).
- #4 `start()` idempotency guard (`if (scope != null) return`).
- #5 `composeContent()` `remember`ed in the AutomationList route.
- #9 single source of truth: `Automation.executionEnabled` added to the interface;
  DataHandlerMobile/
  ScenesViewModel/QuickLaunchResolver/QuickLaunchConfigViewModel use it (reverted the Phase-5
  `Config`
  injections in the two QuickLaunch types).
- Tests added: `processEvent` no-op on client, `executionEnabled` on master.
- DEFERRED (rationale): #6 unneeded (init order guarantees loadFromSP before any permission pass,
  gated by `config.appInitialized`); #8 route dedup NOT done (PluginContentRoute vs
  AutomationContentRoute
  differ in settings mechanism + `LocalPluginNavigationRequest`; sharing risks regressing the host
  for
  ~10 plugins); #10 UI-dep ctor left as-is (moving `composeContent()` out would push 7 deps into the
  nav graph — worse).
- Verified: `:app:assembleFullDebug`, automation tests 6/6, sync DataHandlerMobile test 8/8.

## Resolved decisions

- A: system reminders master-only, objectives-independent, no extra gating. ✅
- B: client `userAction` buttons → (i) hidden/disabled (no execution on client). ✅
- C: dynamic non-plugin `PermissionProvider` feeding the existing badge→sheet; refresh on foreground
  (accept-it MVP — no immediate intra-activity refresh). ✅
- Settings: register `PreferenceSubScreenDef` in `BuiltInSearchables`; reach via existing generic
  `PreferenceScreen` route. (No `PreferenceScreenProvider` set.) ✅
- UI: Approach 1 (no Hilt, no ViewModel; `composeContent()` factory + remember-scoped holder); keep
  internal-route model. ✅
- Runtime class name: `AutomationRuntime`. ✅
- `BuiltInSearchables` settings registration needs NO new project dependency
  (`StringKey.AutomationLocation` is in `core/keys`; strings + icon in `core/ui`). ✅

## Open items

None — ready to start PR 1 (Phases 1–3).
