package app.aaps.pump.lenomed;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.format.DateFormat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import app.aaps.core.data.model.BS;
import app.aaps.core.data.plugin.PluginType;
import app.aaps.core.data.pump.defs.ManufacturerType;
import app.aaps.core.data.pump.defs.PumpDescription;
import app.aaps.core.data.pump.defs.PumpType;
import app.aaps.core.data.time.T;
import app.aaps.core.interfaces.constraints.Constraint;
import app.aaps.core.interfaces.constraints.ConstraintsChecker;
import app.aaps.core.interfaces.constraints.PluginConstraints;
import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
import javax.inject.Provider;
import app.aaps.core.interfaces.plugin.OwnDatabasePlugin;
import app.aaps.core.interfaces.plugin.PluginDescription;
import app.aaps.core.interfaces.profile.Profile;
import app.aaps.core.interfaces.profile.ProfileFunction;
import app.aaps.core.interfaces.pump.DetailedBolusInfo;
import app.aaps.core.interfaces.pump.DetailedBolusInfoStorage;
import app.aaps.core.interfaces.pump.Pump;
import app.aaps.core.interfaces.pump.PumpEnactResult;
import app.aaps.core.interfaces.pump.PumpPluginBase;
import app.aaps.core.interfaces.pump.PumpSync;
import app.aaps.core.interfaces.pump.TemporaryBasalStorage;
import app.aaps.core.interfaces.pump.actions.CustomAction;
import app.aaps.core.interfaces.pump.actions.CustomActionType;
import app.aaps.core.interfaces.pump.defs.PumpDescriptionExtensionKt;
import app.aaps.core.interfaces.pump.defs.PumpTypeExtensionKt;
import app.aaps.core.interfaces.queue.CommandQueue;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.rx.AapsSchedulers;
import app.aaps.core.interfaces.rx.bus.RxBus;
import app.aaps.core.interfaces.rx.events.EventAppExit;
import app.aaps.core.interfaces.rx.events.EventConfigBuilderChange;
import app.aaps.core.interfaces.rx.events.EventOverviewBolusProgress;
import app.aaps.core.interfaces.sharedPreferences.SP;
import app.aaps.core.interfaces.ui.UiInteraction;
import app.aaps.core.interfaces.utils.DateUtil;
import app.aaps.core.interfaces.utils.DecimalFormatter;
import app.aaps.core.interfaces.utils.Round;
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy;
import app.aaps.core.keys.interfaces.Preferences;
import app.aaps.core.objects.constraints.ConstraintObject;
import app.aaps.core.ui.toast.ToastUtils;
import app.aaps.pump.lenomed.database.LenomedHistoryDatabase;
import app.aaps.pump.lenomed.events.EventEmbectaDeviceChange;
import app.aaps.pump.lenomed.service.LenomedService;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.socket.client.Socket;
import java.util.List;
import javax.inject.Singleton;
import kotlin.NotImplementedError;
import kotlin.jvm.internal.Intrinsics;
import dev.zacsweers.metro.IntKey;
import app.aaps.core.keys.interfaces.TextRef;
import app.aaps.core.interfaces.notifications.NotificationManager;
import app.aaps.core.interfaces.di.PumpDriver;
import org.json.JSONException;
import org.json.JSONObject;
import app.aaps.core.interfaces.pump.PumpInsulin;
import app.aaps.core.interfaces.pump.PumpProfile;
import app.aaps.core.interfaces.pump.PumpRate;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import kotlinx.coroutines.flow.StateFlow;
import kotlin.jvm.internal.Reflection;

/* JADX INFO: compiled from: LenomedPlugin.kt */
/* JADX INFO: loaded from: classes4.dex */
@PumpDriver
@IntKey(1200)
public final class LenomedPlugin extends PumpPluginBase implements Pump, PluginConstraints, OwnDatabasePlugin {
    private final AapsSchedulers aapsSchedulers;
    private final ConstraintsChecker constraintChecker;
    private final Context context;
    private final DateUtil dateUtil;
    private final DecimalFormatter decimalFormatter;
    private final DetailedBolusInfoStorage detailedBolusInfoStorage;
    private final LenomedHistoryDatabase diaconnHistoryDatabase;
    private final CompositeDisposable disposable;
    private final FabricPrivacy fabricPrivacy;
    private final Provider<PumpEnactResult> pumpEnactResultProvider;
    private final boolean isFakingTempsByExtendedBoluses;
    private final LenomedPump lenomedPump;
    private LenomedService lenomedService;
    private final ServiceConnection mConnection;
    private String mDeviceAddress;
    private String mDeviceName;
    private CoroutineScope eventScope;
    private final ProfileFunction profileFunction;
    private final PumpDescription pumpDescription;
    private final PumpSync pumpSync;
    private final RxBus rxBus;
    private final SP sp;
    private final TemporaryBasalStorage temporaryBasalStorage;
    private final UiInteraction uiInteraction;
    private final MutableStateFlow<Long> lastDataTimeFlow = StateFlowKt.MutableStateFlow(0L);
    private final MutableStateFlow<Long> lastBolusTimeFlow = StateFlowKt.MutableStateFlow(null);
    private final MutableStateFlow<PumpInsulin> lastBolusAmountFlow = StateFlowKt.MutableStateFlow(null);
    private final MutableStateFlow<PumpInsulin> reservoirLevelFlow = StateFlowKt.MutableStateFlow(new PumpInsulin(0.0d));
    private final MutableStateFlow<Integer> batteryLevelFlow = StateFlowKt.MutableStateFlow(null);

    @Override // app.aaps.core.interfaces.pump.Pump
    public boolean canHandleDST() {
        return false;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public void executeCustomAction(CustomActionType customActionType) {
        Intrinsics.checkNotNullParameter(customActionType, "customActionType");
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public List<CustomAction> getCustomActions() {
        return null;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public boolean isBusy() {
        return false;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public boolean isHandshakeInProgress() {
        return false;
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public LenomedPlugin(final AAPSLogger aapsLogger, RxBus rxBus, Context context, ResourceHelper rh, Preferences preferences, ConstraintsChecker constraintChecker, ProfileFunction profileFunction, SP sp, CommandQueue commandQueue, LenomedPump lenomedPump, PumpSync pumpSync, DetailedBolusInfoStorage detailedBolusInfoStorage, TemporaryBasalStorage temporaryBasalStorage, FabricPrivacy fabricPrivacy, DateUtil dateUtil, AapsSchedulers aapsSchedulers, UiInteraction uiInteraction, LenomedHistoryDatabase diaconnHistoryDatabase, DecimalFormatter decimalFormatter, Provider<PumpEnactResult> pumpEnactResultProvider, NotificationManager notificationManager) {
        super(new PluginDescription()
            .mainType(PluginType.PUMP)
            .pluginName(new TextRef.AndroidRes(R.string.lenomed_pump, kotlin.collections.CollectionsKt.emptyList()))
            .description(new TextRef.AndroidRes(R.string.description_pump_lenomed, kotlin.collections.CollectionsKt.emptyList())),
            kotlin.collections.CollectionsKt.emptyList(), aapsLogger, rh, preferences, commandQueue, notificationManager);
        Intrinsics.checkNotNullParameter(aapsLogger, "aapsLogger");
        Intrinsics.checkNotNullParameter(rxBus, "rxBus");
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(rh, "rh");
        Intrinsics.checkNotNullParameter(preferences, "preferences");
        Intrinsics.checkNotNullParameter(constraintChecker, "constraintChecker");
        Intrinsics.checkNotNullParameter(profileFunction, "profileFunction");
        Intrinsics.checkNotNullParameter(sp, "sp");
        Intrinsics.checkNotNullParameter(commandQueue, "commandQueue");
        Intrinsics.checkNotNullParameter(lenomedPump, "lenomedPump");
        Intrinsics.checkNotNullParameter(pumpSync, "pumpSync");
        Intrinsics.checkNotNullParameter(detailedBolusInfoStorage, "detailedBolusInfoStorage");
        Intrinsics.checkNotNullParameter(temporaryBasalStorage, "temporaryBasalStorage");
        Intrinsics.checkNotNullParameter(fabricPrivacy, "fabricPrivacy");
        Intrinsics.checkNotNullParameter(dateUtil, "dateUtil");
        Intrinsics.checkNotNullParameter(aapsSchedulers, "aapsSchedulers");
        Intrinsics.checkNotNullParameter(uiInteraction, "uiInteraction");
        Intrinsics.checkNotNullParameter(diaconnHistoryDatabase, "diaconnHistoryDatabase");
        Intrinsics.checkNotNullParameter(decimalFormatter, "decimalFormatter");
        Intrinsics.checkNotNullParameter(pumpEnactResultProvider, "pumpEnactResultProvider");
        this.rxBus = rxBus;
        this.context = context;
        this.constraintChecker = constraintChecker;
        this.profileFunction = profileFunction;
        this.sp = sp;
        this.lenomedPump = lenomedPump;
        this.pumpSync = pumpSync;
        this.detailedBolusInfoStorage = detailedBolusInfoStorage;
        this.temporaryBasalStorage = temporaryBasalStorage;
        this.fabricPrivacy = fabricPrivacy;
        this.dateUtil = dateUtil;
        this.aapsSchedulers = aapsSchedulers;
        this.uiInteraction = uiInteraction;
        this.diaconnHistoryDatabase = diaconnHistoryDatabase;
        this.decimalFormatter = decimalFormatter;
        this.pumpEnactResultProvider = pumpEnactResultProvider;
        this.isFakingTempsByExtendedBoluses = false;
        this.disposable = new CompositeDisposable();
        this.mDeviceAddress = "";
        this.mDeviceName = "";
        this.pumpDescription = PumpDescriptionExtensionKt.fillFor(new PumpDescription(), PumpType.LENOMED);
        this.mConnection = new ServiceConnection() { // from class: app.aaps.pump.lenomed.LenomedPlugin$mConnection$1
            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                Intrinsics.checkNotNullParameter(name, "name");
                aapsLogger.debug(LTag.PUMP, "Service is disconnected");
                LenomedPlugin.this.lenomedService = null;
            }

            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                Intrinsics.checkNotNullParameter(name, "name");
                Intrinsics.checkNotNullParameter(service, "service");
                aapsLogger.debug(LTag.PUMP, "Service is connected");
                LenomedPlugin.this.lenomedService = ((LenomedService.LocalBinder) service).getThis$0();
            }
        };
    }

    public final String getMDeviceName() {
        return this.mDeviceName;
    }

    public final void setMDeviceName(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.mDeviceName = str;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public PumpDescription getPumpDescription() {
        return this.pumpDescription;
    }

    @Override // app.aaps.core.interfaces.pump.PumpPluginBase, app.aaps.core.interfaces.plugin.PluginBase
    public Object onStart(Continuation<? super Unit> continuation) {
        Object result = super.onStart(continuation);
        if (IntrinsicsKt.getCOROUTINE_SUSPENDED() == result) {
            return result;
        }
        this.context.bindService(new Intent(this.context, (Class<?>) LenomedService.class), this.mConnection, 1);
        // RxBus is Flow-based on dev; see LenomedSupport.kt. UNDISPATCHED because the bus has
        // no replay and a scheduled collector could miss an event sent right after subscribe.
        CoroutineScope newScope = LenomedSupportKt.lenomedIoScope();
        this.eventScope = newScope;
        LenomedSupportKt.subscribeEvent(newScope, this.rxBus, getAapsLogger(),
            Reflection.getOrCreateKotlinClass(EventAppExit.class),
            (Function1<EventAppExit, Unit>) it -> {
                Intrinsics.checkNotNullParameter(it, "it");
                this.context.unbindService(this.mConnection);
                return Unit.INSTANCE;
            });
        LenomedSupportKt.subscribeEvent(newScope, this.rxBus, getAapsLogger(),
            Reflection.getOrCreateKotlinClass(EventConfigBuilderChange.class),
            (Function1<EventConfigBuilderChange, Unit>) it -> {
                Intrinsics.checkNotNullParameter(it, "it");
                this.lenomedPump.reset();
                return Unit.INSTANCE;
            });
        LenomedSupportKt.subscribeEvent(newScope, this.rxBus, getAapsLogger(),
            Reflection.getOrCreateKotlinClass(EventEmbectaDeviceChange.class),
            (Function1<EventEmbectaDeviceChange, Unit>) it -> {
                Intrinsics.checkNotNullParameter(it, "it");
                this.changePump();
                return Unit.INSTANCE;
            });
        changePump();
        return Unit.INSTANCE;
    }

    @Override // app.aaps.core.interfaces.pump.PumpPluginBase, app.aaps.core.interfaces.plugin.PluginBase
    public Object onStop(Continuation<? super Unit> continuation) {
        Object result = super.onStop(continuation);
        if (IntrinsicsKt.getCOROUTINE_SUSPENDED() == result) {
            return result;
        }
        CoroutineScope scope = this.eventScope;
        if (scope != null) {
            CoroutineScopeKt.cancel(scope, null);
            this.eventScope = null;
        }
        this.disposable.clear();
        this.context.unbindService(this.mConnection);
        this.lenomedService = null;
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void changePump() {
        this.mDeviceAddress = this.sp.getString(app.aaps.pump.lenomed.R.string.key_pump_lenomed_address, "");
        this.mDeviceName = this.sp.getString(app.aaps.pump.lenomed.R.string.key_pump_lenomed_name, "");
        this.lenomedPump.reset();
        // commandQueue.readStatus() is suspend on dev; fire-and-forget on the plugin scope
        // (mirrors DiaconnG8Plugin.changePump) so this stays a plain method.
        final String reason = ((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.device_changed);
        BuildersKt.launch(this.getPluginScope(), Dispatchers.INSTANCE.getIO(), null, new Function2<CoroutineScope, Continuation<? super Unit>, Object>() {
            @Override
            public Object invoke(CoroutineScope scope, Continuation<? super Unit> cont) {
                Object r = LenomedPlugin.this.getCommandQueue().readStatus(reason, (kotlin.coroutines.Continuation) cont);
                if (IntrinsicsKt.getCOROUTINE_SUSPENDED() == r) {
                    return r;
                }
                return Unit.INSTANCE;
            }
        });
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public void connect(String reason) {
        Intrinsics.checkNotNullParameter(reason, "reason");
        getAapsLogger().debug(LTag.PUMP, "Lenomed connect from: " + reason);
        if (this.lenomedService == null || Intrinsics.areEqual(this.mDeviceAddress, "") || Intrinsics.areEqual(this.mDeviceName, "")) {
            return;
        }
        LenomedService lenomedService = this.lenomedService;
        if (lenomedService == null || !lenomedService.connect(reason, this.mDeviceAddress)) {
            ToastUtils.INSTANCE.errorToast(this.context, app.aaps.core.ui.R.string.ble_not_supported);
        }
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public boolean isConnected() {
        LenomedService lenomedService = this.lenomedService;
        return lenomedService != null && lenomedService.isConnected();
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public boolean isConnecting() {
        LenomedService lenomedService = this.lenomedService;
        return lenomedService != null && lenomedService.isConnecting();
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public void disconnect(String reason) {
        Intrinsics.checkNotNullParameter(reason, "reason");
        getAapsLogger().debug(LTag.PUMP, "Lenomed disconnect from: " + reason);
        LenomedService lenomedService = this.lenomedService;
        if (lenomedService != null) {
            lenomedService.disconnect(reason);
        }
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public void stopConnecting() {
        LenomedService lenomedService = this.lenomedService;
        if (lenomedService != null) {
            lenomedService.stopConnecting();
        }
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public Object getPumpStatus(String reason, Continuation<? super Unit> continuation) {
        Intrinsics.checkNotNullParameter(reason, "reason");
        LenomedService lenomedService = this.lenomedService;
        if (lenomedService != null) {
            lenomedService.readPumpStatus();
        }
        getPumpDescription().setBasalStep(this.lenomedPump.getBasalStep());
        getPumpDescription().setBolusStep(this.lenomedPump.getBolusStep());
        getPumpDescription().setBasalMaximumRate(4.0d);
        this.lastDataTimeFlow.setValue(this.lenomedPump.getLastConnection());
        this.lastBolusTimeFlow.setValue(this.lenomedPump.getLastBolusTime() != 0 ? Long.valueOf(this.lenomedPump.getLastBolusTime()) : null);
        this.lastBolusAmountFlow.setValue(this.lenomedPump.getLastBolusAmount() > 0.0d ? new PumpInsulin(this.lenomedPump.getLastBolusAmount()) : null);
        this.reservoirLevelFlow.setValue(new PumpInsulin(this.lenomedPump.getSystemRemainInsulin()));
        this.batteryLevelFlow.setValue(Integer.valueOf(this.lenomedPump.getSystemRemainBattery()));
        return Unit.INSTANCE;
    }

    public PumpEnactResult loadHistory(byte type) {
        PumpEnactResult pumpEnactResultLoadHistory$default;
        LenomedService lenomedService = this.lenomedService;
        return (lenomedService == null || (pumpEnactResultLoadHistory$default = LenomedService.loadHistory$default(lenomedService, type, false, 2, null)) == null) ? this.pumpEnactResultProvider.get().success(false) : pumpEnactResultLoadHistory$default;
    }

    public PumpEnactResult loadBolusHistory() {
        PumpEnactResult pumpEnactResultLoadBolusHistory;
        LenomedService lenomedService = this.lenomedService;
        return (lenomedService == null || (pumpEnactResultLoadBolusHistory = lenomedService.loadBolusHistory()) == null) ? this.pumpEnactResultProvider.get().success(false) : pumpEnactResultLoadBolusHistory;
    }

    public PumpEnactResult setUserOptions() {
        PumpEnactResult userSettings;
        LenomedService lenomedService = this.lenomedService;
        return (lenomedService == null || (userSettings = lenomedService.setUserSettings()) == null) ? this.pumpEnactResultProvider.get().success(false) : userSettings;
    }

    @Override // app.aaps.core.interfaces.constraints.PluginConstraints
    public Constraint<Double> applyBasalConstraints(Constraint<Double> absoluteRate, Profile profile) {
        Intrinsics.checkNotNullParameter(absoluteRate, "absoluteRate");
        Intrinsics.checkNotNullParameter(profile, "profile");
        absoluteRate.setIfSmaller(Double.valueOf(this.lenomedPump.getMaxBasal()), ((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.limitingbasalratio, Double.valueOf(this.lenomedPump.getMaxBasal()), ((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.pumplimit)), this);
        return absoluteRate;
    }

    @Override // app.aaps.core.interfaces.constraints.PluginConstraints
    public Constraint<Integer> applyBasalPercentConstraints(Constraint<Integer> percentRate, Profile profile) {
        Intrinsics.checkNotNullParameter(percentRate, "percentRate");
        Intrinsics.checkNotNullParameter(profile, "profile");
        percentRate.setIfGreater(Integer.valueOf(0), ((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.limitingpercentrate, 0, ((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.itmustbepositivevalue)), this);
        percentRate.setIfSmaller(Integer.valueOf(getPumpDescription().getMaxTempPercent()), ((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.limitingpercentrate, Integer.valueOf(getPumpDescription().getMaxTempPercent()), ((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.pumplimit)), this);
        return percentRate;
    }

    @Override // app.aaps.core.interfaces.constraints.PluginConstraints
    public Constraint<Double> applyBolusConstraints(Constraint<Double> insulin) {
        Intrinsics.checkNotNullParameter(insulin, "insulin");
        insulin.setIfSmaller(Double.valueOf(this.lenomedPump.getMaxBolus()), ((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.limitingbolus, Double.valueOf(this.lenomedPump.getMaxBolus()), ((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.pumplimit)), this);
        return insulin;
    }

    @Override // app.aaps.core.interfaces.constraints.PluginConstraints
    public Constraint<Double> applyExtendedBolusConstraints(Constraint<Double> insulin) {
        Intrinsics.checkNotNullParameter(insulin, "insulin");
        return applyBolusConstraints(insulin);
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public boolean isInitialized() {
        return this.lenomedPump.getLastConnection() > 0 && this.lenomedPump.getMaxBasal() > 0.0d;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public boolean isSuspended() {
        return this.lenomedPump.getBasePauseStatus() == 1;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public Object setNewBasalProfile(PumpProfile profile, Continuation<? super PumpEnactResult> continuation) {
        Intrinsics.checkNotNullParameter(profile, "profile");
        PumpEnactResult pumpEnactResultProvidePumpEnactResult = this.pumpEnactResultProvider.get();
        if (!isInitialized()) {
            getAapsLogger().error(LTag.PUMP, "setNewBasalProfile: " + ((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.pump_not_initialized_profile_not_set));
            pumpEnactResultProvidePumpEnactResult.setComment(((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.pump_not_initialized_profile_not_set));
            return pumpEnactResultProvidePumpEnactResult;
        }
        LenomedService lenomedService = this.lenomedService;
        if (lenomedService == null || !lenomedService.updateBasalsInPump(profile)) {
            getAapsLogger().error(LTag.PUMP, "setNewBasalProfile: " + ((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.failed_update_basal_profile));
            pumpEnactResultProvidePumpEnactResult.setComment(((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.failed_update_basal_profile));
            return pumpEnactResultProvidePumpEnactResult;
        }
        pumpEnactResultProvidePumpEnactResult.setSuccess(true);
        pumpEnactResultProvidePumpEnactResult.setEnacted(true);
        pumpEnactResultProvidePumpEnactResult.setComment("OK");
        return pumpEnactResultProvidePumpEnactResult;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public boolean isThisProfileSet(PumpProfile profile) {
        Intrinsics.checkNotNullParameter(profile, "profile");
        if (!isInitialized() || this.lenomedPump.getPumpProfiles() == null) {
            return true;
        }
        for (int i = 0; i < 24; i++) {
            Double[][] pumpProfiles = this.lenomedPump.getPumpProfiles();
            Intrinsics.checkNotNull(pumpProfiles);
            double dDoubleValue = pumpProfiles[this.lenomedPump.getActiveProfile()][i].doubleValue();
            double basalTimeFromMidnight = profile.getBasalTimeFromMidnight(i * 3600);
            if (Math.abs(dDoubleValue - basalTimeFromMidnight) > getPumpDescription().getBasalStep()) {
                getAapsLogger().debug(LTag.PUMP, "Diff found. Hour: " + i + " Pump: " + dDoubleValue + " Profile: " + basalTimeFromMidnight);
                return false;
            }
        }
        return true;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public PumpRate getBaseBasalRate() {
        return new PumpRate(this.lenomedPump.getBaseAmount());
    }

    private double getBaseBasalRateValue() {
        return this.lenomedPump.getBaseAmount();
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public StateFlow<PumpInsulin> getReservoirLevel() {
        return this.reservoirLevelFlow;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public StateFlow<Integer> getBatteryLevel() {
        return this.batteryLevelFlow;
    }

    /* JADX WARN: Removed duplicated region for block: B:34:0x0134 A[Catch: all -> 0x0198, TryCatch #0 {, blocks: (B:4:0x0009, B:6:0x007a, B:8:0x0080, B:10:0x00af, B:12:0x00b6, B:14:0x00bc, B:15:0x00c9, B:19:0x00df, B:32:0x011e, B:34:0x0134, B:35:0x0137, B:37:0x013d, B:39:0x0154, B:38:0x0147, B:26:0x0104, B:28:0x0108, B:11:0x00b4, B:42:0x017c, B:43:0x0189, B:44:0x018a, B:45:0x0197), top: B:49:0x0009 }] */
    /* JADX WARN: Removed duplicated region for block: B:37:0x013d A[Catch: all -> 0x0198, TryCatch #0 {, blocks: (B:4:0x0009, B:6:0x007a, B:8:0x0080, B:10:0x00af, B:12:0x00b6, B:14:0x00bc, B:15:0x00c9, B:19:0x00df, B:32:0x011e, B:34:0x0134, B:35:0x0137, B:37:0x013d, B:39:0x0154, B:38:0x0147, B:26:0x0104, B:28:0x0108, B:11:0x00b4, B:42:0x017c, B:43:0x0189, B:44:0x018a, B:45:0x0197), top: B:49:0x0009 }] */
    /* JADX WARN: Removed duplicated region for block: B:38:0x0147 A[Catch: all -> 0x0198, TryCatch #0 {, blocks: (B:4:0x0009, B:6:0x007a, B:8:0x0080, B:10:0x00af, B:12:0x00b6, B:14:0x00bc, B:15:0x00c9, B:19:0x00df, B:32:0x011e, B:34:0x0134, B:35:0x0137, B:37:0x013d, B:39:0x0154, B:38:0x0147, B:26:0x0104, B:28:0x0108, B:11:0x00b4, B:42:0x017c, B:43:0x0189, B:44:0x018a, B:45:0x0197), top: B:49:0x0009 }] */
    @Override // app.aaps.core.interfaces.pump.Pump
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized Object deliverTreatment(DetailedBolusInfo detailedBolusInfo, Continuation<? super PumpEnactResult> continuation) {
        boolean z;
        EventOverviewBolusProgress.Treatment treatment;
        boolean z2 = false;
        PumpEnactResult pumpEnactResultProvidePumpEnactResult;
        Intrinsics.checkNotNullParameter(detailedBolusInfo, "detailedBolusInfo");
        getAapsLogger().debug(LTag.PUMP, "deliverTreatment: Starting bolus delivery. Insulin: " + detailedBolusInfo.getInsulin() + " U, BolusType: " + detailedBolusInfo.getBolusType() + ", Carbs: " + detailedBolusInfo.getCarbs() + " g, Timestamp: " + this.dateUtil.dateAndTimeString(detailedBolusInfo.getTimestamp()) + ", BolusBlocked: " + this.lenomedPump.getBolusBlocked() + ", BolusStep: " + this.lenomedPump.getBolusStep());
        if (detailedBolusInfo.getCarbs() != 0.0d) {
            throw new IllegalArgumentException(detailedBolusInfo.toString().toString());
        }
        if (detailedBolusInfo.getInsulin() <= 0.0d) {
            throw new IllegalArgumentException(detailedBolusInfo.toString().toString());
        }
        detailedBolusInfo.setInsulin(detailedBolusInfo.getInsulin());
        double d = detailedBolusInfo.getCarbs();
        detailedBolusInfo.setCarbs(0.0d);
        Long carbsTimestamp = detailedBolusInfo.getCarbsTimestamp();
        long jLongValue = carbsTimestamp != null ? carbsTimestamp.longValue() : detailedBolusInfo.getTimestamp();
        if (jLongValue == detailedBolusInfo.getTimestamp()) {
            jLongValue -= T.Companion.mins(1L).msecs();
        }
        this.detailedBolusInfoStorage.add(detailedBolusInfo);
        EventOverviewBolusProgress.Treatment treatment2 = new EventOverviewBolusProgress.Treatment(0.0d, 0, detailedBolusInfo.getBolusType() == BS.Type.SMB, Long.valueOf(detailedBolusInfo.getId()));
        if (detailedBolusInfo.getInsulin() > 0.0d || d > 0.0d) {
            LenomedService lenomedService = this.lenomedService;
            if (lenomedService != null) {
                z = true;
                treatment = treatment2;
                if (lenomedService.bolus(detailedBolusInfo.getInsulin(), (int) d, jLongValue, treatment)) {
                    z2 = true;
                }
                pumpEnactResultProvidePumpEnactResult = this.pumpEnactResultProvider.get();
                pumpEnactResultProvidePumpEnactResult.setSuccess(z2);
                pumpEnactResultProvidePumpEnactResult.setBolusDelivered(treatment.getInsulin());
                if (pumpEnactResultProvidePumpEnactResult.getSuccess()) {
                    pumpEnactResultProvidePumpEnactResult.setEnacted(z);
                }
                if (pumpEnactResultProvidePumpEnactResult.getSuccess()) {
                    setErrorMsg(this.lenomedPump.getResultErrorCode(), pumpEnactResultProvidePumpEnactResult);
                } else {
                    pumpEnactResultProvidePumpEnactResult.setComment(((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.ok));
                }
                getAapsLogger().debug(LTag.PUMP, "deliverTreatment: OK. Asked: " + detailedBolusInfo.getInsulin() + " Delivered: " + pumpEnactResultProvidePumpEnactResult.getBolusDelivered());
            } else {
                z = true;
                treatment = treatment2;
            }
        } else {
            z = true;
            treatment = treatment2;
        }
        z2 = false;
        pumpEnactResultProvidePumpEnactResult = this.pumpEnactResultProvider.get();
        pumpEnactResultProvidePumpEnactResult.setSuccess(z2);
        pumpEnactResultProvidePumpEnactResult.setBolusDelivered(treatment.getInsulin());
        if (pumpEnactResultProvidePumpEnactResult.getSuccess()) {
        }
        if (pumpEnactResultProvidePumpEnactResult.getSuccess()) {
        }
        getAapsLogger().debug(LTag.PUMP, "deliverTreatment: OK. Asked: " + detailedBolusInfo.getInsulin() + " Delivered: " + pumpEnactResultProvidePumpEnactResult.getBolusDelivered());
        return pumpEnactResultProvidePumpEnactResult;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public void stopBolusDelivering() {
        LenomedService lenomedService = this.lenomedService;
        if (lenomedService != null) {
            lenomedService.bolusStop();
        }
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public synchronized Object setTempBasalAbsolute(double absoluteRate, int durationInMinutes, boolean enforceNew, PumpSync.TemporaryBasalType tbrType, Continuation<? super PumpEnactResult> continuation) {
        boolean z;
        boolean z2;
        Intrinsics.checkNotNullParameter(tbrType, "tbrType");
        PumpEnactResult pumpEnactResultProvidePumpEnactResult = this.pumpEnactResultProvider.get();
        double dDoubleValue = absoluteRate;
        boolean z3 = getBaseBasalRateValue() - dDoubleValue == 0.0d;
        boolean z4 = dDoubleValue < getBaseBasalRateValue();
        boolean z5 = dDoubleValue > getBaseBasalRateValue();
        if (z3) {
            if (this.lenomedPump.isTempBasalInProgress()) {
                getAapsLogger().debug(LTag.PUMP, "setTempBasalAbsolute: Stopping temp basal (doTempOff)");
                return cancelTempBasal(false, continuation);
            }
            pumpEnactResultProvidePumpEnactResult.setSuccess(true);
            pumpEnactResultProvidePumpEnactResult.setEnacted(false);
            pumpEnactResultProvidePumpEnactResult.setAbsolute(getBaseBasalRateValue());
            pumpEnactResultProvidePumpEnactResult.setPercent(false);
            pumpEnactResultProvidePumpEnactResult.setTempCancel(true);
            getAapsLogger().debug(LTag.PUMP, "setTempBasalAbsolute: doTempOff OK");
            return pumpEnactResultProvidePumpEnactResult;
        }
        if (z4 || z5) {
            if (this.lenomedPump.isTempBasalInProgress()) {
                getAapsLogger().debug(LTag.PUMP, "setTempBasalAbsolute: currently running");
                if (this.lenomedPump.getTempBasalAbsoluteRate() == dDoubleValue && this.lenomedPump.getTempBasalRemainingMin() > 4 && !enforceNew) {
                    pumpEnactResultProvidePumpEnactResult.setSuccess(true);
                    pumpEnactResultProvidePumpEnactResult.setAbsolute(dDoubleValue);
                    pumpEnactResultProvidePumpEnactResult.setEnacted(false);
                    pumpEnactResultProvidePumpEnactResult.setDuration(this.lenomedPump.getTempBasalRemainingMin());
                    pumpEnactResultProvidePumpEnactResult.setPercent(false);
                    pumpEnactResultProvidePumpEnactResult.setTempCancel(false);
                    getAapsLogger().debug(LTag.PUMP, "setTempBasalAbsolute: Correct temp basal already set (doLowTemp || doHighTemp)");
                    return pumpEnactResultProvidePumpEnactResult;
                }
            }
            this.temporaryBasalStorage.add(new PumpSync.PumpState.TemporaryBasal(this.dateUtil.now(), T.Companion.mins((long) durationInMinutes).msecs(), absoluteRate, true, tbrType, 0L, null, PumpType.USER, ""));
            getAapsLogger().debug(LTag.PUMP, "setTempBasalAbsolute: Setting temp basal " + dDoubleValue + " U for " + durationInMinutes + " mins (doLowTemp || doHighTemp)");
            if (durationInMinutes == 15 || durationInMinutes == 30) {
                LenomedService lenomedService = this.lenomedService;
                z = lenomedService != null && lenomedService.tempBasalShortDuration(dDoubleValue, durationInMinutes);
            } else {
                double dMax = Math.max(((double) durationInMinutes) / 60.0d, 1.0d);
                LenomedService lenomedService2 = this.lenomedService;
                z = lenomedService2 != null && lenomedService2.tempBasal(dDoubleValue, dMax);
            }
            getAapsLogger().debug(LTag.PUMP, "setTempBasalAbsolute: Validation check - isTempBasalInProgress=" + this.lenomedPump.isTempBasalInProgress() + ", tempBasalAbsoluteRate=" + this.lenomedPump.getTempBasalAbsoluteRate() + ", expected rate=" + dDoubleValue);
            if (z && (Math.abs(dDoubleValue) < 0.1d || (this.lenomedPump.isTempBasalInProgress() && Math.abs(this.lenomedPump.getTempBasalAbsoluteRate() - dDoubleValue) < 0.1d))) {
                pumpEnactResultProvidePumpEnactResult.setEnacted(true);
                pumpEnactResultProvidePumpEnactResult.setSuccess(true);
                pumpEnactResultProvidePumpEnactResult.setComment(((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.ok));
                pumpEnactResultProvidePumpEnactResult.setTempCancel(false);
                pumpEnactResultProvidePumpEnactResult.setDuration(this.lenomedPump.getTempBasalRemainingMin());
                pumpEnactResultProvidePumpEnactResult.setAbsolute(this.lenomedPump.getTempBasalAbsoluteRate());
                pumpEnactResultProvidePumpEnactResult.setPercent(false);
                getAapsLogger().debug(LTag.PUMP, "setTempBasalAbsolute: OK");
                return pumpEnactResultProvidePumpEnactResult;
            }
            z2 = false;
            getAapsLogger().error(LTag.PUMP, "setTempBasalAbsolute: Validation failed - connectionOK=" + z + ", isTempBasalInProgress=" + this.lenomedPump.isTempBasalInProgress() + ", tempBasalAbsoluteRate=" + this.lenomedPump.getTempBasalAbsoluteRate() + ", expected rate=" + dDoubleValue);
        } else {
            z2 = false;
        }
        pumpEnactResultProvidePumpEnactResult.setEnacted(z2);
        pumpEnactResultProvidePumpEnactResult.setSuccess(z2);
        pumpEnactResultProvidePumpEnactResult.setComment(((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.temp_basal_delivery_error));
        getAapsLogger().error("setTempBasalAbsolute: Failed to set temp basal");
        return pumpEnactResultProvidePumpEnactResult;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public synchronized Object setTempBasalPercent(int percent, int durationInMinutes, boolean enforceNew, PumpSync.TemporaryBasalType tbrType, Continuation<? super PumpEnactResult> continuation) {
        Intrinsics.checkNotNullParameter(tbrType, "tbrType");
        if (percent == 0) {
            return setTempBasalAbsolute(0.0d, durationInMinutes, enforceNew, tbrType, continuation);
        }
        double dDetermineCorrectBasalSize = PumpTypeExtensionKt.determineCorrectBasalSize(getPumpDescription().getPumpType(), getBaseBasalRateValue() * (((double) percent) / 100.0d));
        getAapsLogger().warn(LTag.PUMP, "setTempBasalPercent [LenomedPlugin] - You are trying to use setTempBasalPercent with percent other then 0% (" + percent + "). This will start setTempBasalAbsolute, with calculated value (" + dDetermineCorrectBasalSize + "). Result might not be 100% correct.");
        return setTempBasalAbsolute(dDetermineCorrectBasalSize, durationInMinutes, enforceNew, tbrType, continuation);
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public synchronized Object setExtendedBolus(double insulin, int durationInMinutes, Continuation<? super PumpEnactResult> continuation) {
        double dRoundTo = Round.INSTANCE.roundTo(insulin, getPumpDescription().getExtendedBolusStep());
        PumpEnactResult pumpEnactResultProvidePumpEnactResult = this.pumpEnactResultProvider.get();
        if (this.lenomedPump.isExtendedInProgress() && Math.abs(this.lenomedPump.getExtendedBolusAmount() - dRoundTo) < getPumpDescription().getExtendedBolusStep()) {
            pumpEnactResultProvidePumpEnactResult.setEnacted(false);
            pumpEnactResultProvidePumpEnactResult.setSuccess(true);
            pumpEnactResultProvidePumpEnactResult.setComment(((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.ok));
            pumpEnactResultProvidePumpEnactResult.setDuration(this.lenomedPump.getExtendedBolusRemainingMinutes());
            pumpEnactResultProvidePumpEnactResult.setAbsolute(this.lenomedPump.getExtendedBolusAbsoluteRate());
            pumpEnactResultProvidePumpEnactResult.setPercent(false);
            pumpEnactResultProvidePumpEnactResult.setTempCancel(false);
            getAapsLogger().debug(LTag.PUMP, "setExtendedBolus: Correct extended bolus already set. Current: " + this.lenomedPump.getExtendedBolusAmount() + " Asked: " + dRoundTo);
            return pumpEnactResultProvidePumpEnactResult;
        }
        LenomedService lenomedService = this.lenomedService;
        if (lenomedService != null && lenomedService.extendedBolus(dRoundTo, durationInMinutes)) {
            pumpEnactResultProvidePumpEnactResult.setEnacted(true);
            pumpEnactResultProvidePumpEnactResult.setSuccess(true);
            pumpEnactResultProvidePumpEnactResult.setComment(((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.ok));
            pumpEnactResultProvidePumpEnactResult.setTempCancel(false);
            pumpEnactResultProvidePumpEnactResult.setDuration(this.lenomedPump.getExtendedBolusRemainingMinutes());
            pumpEnactResultProvidePumpEnactResult.setAbsolute(this.lenomedPump.getExtendedBolusAbsoluteRate());
            pumpEnactResultProvidePumpEnactResult.setBolusDelivered(this.lenomedPump.getExtendedBolusAmount());
            pumpEnactResultProvidePumpEnactResult.setPercent(false);
            getAapsLogger().debug(LTag.PUMP, "setExtendedBolus: OK");
            return pumpEnactResultProvidePumpEnactResult;
        }
        pumpEnactResultProvidePumpEnactResult.setEnacted(false);
        pumpEnactResultProvidePumpEnactResult.setSuccess(false);
        setErrorMsg(this.lenomedPump.getResultErrorCode(), pumpEnactResultProvidePumpEnactResult);
        getAapsLogger().error("setExtendedBolus: Failed to extended bolus");
        return pumpEnactResultProvidePumpEnactResult;
    }

    public PumpEnactResult setSquareWaveBolus(double insulin, int durationInMinutes, int durationBloodInMinutes) {
        throw new NotImplementedError("An operation is not implemented: Not yet implemented");
    }

    public PumpEnactResult setDoubleWaveBolus(double insulin, int durationInMinutes, int durationBloodInMinutes) {
        throw new NotImplementedError("An operation is not implemented: Not yet implemented");
    }

    public PumpEnactResult setSyncPumpTime() {
        throw new NotImplementedError("An operation is not implemented: Not yet implemented");
    }

    public PumpEnactResult setPauseResumePump(int type) {
        throw new NotImplementedError("An operation is not implemented: Not yet implemented");
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public synchronized Object cancelTempBasal(boolean enforceNew, Continuation<? super PumpEnactResult> continuation) {
        PumpEnactResult pumpEnactResultProvidePumpEnactResult;
        pumpEnactResultProvidePumpEnactResult = this.pumpEnactResultProvider.get();
        if (this.lenomedPump.isTempBasalInProgress()) {
            LenomedService lenomedService = this.lenomedService;
            if (lenomedService != null) {
                lenomedService.tempBasalStop();
            }
            pumpEnactResultProvidePumpEnactResult.setSuccess(!this.lenomedPump.isTempBasalInProgress());
            pumpEnactResultProvidePumpEnactResult.setEnacted(true);
            pumpEnactResultProvidePumpEnactResult.setTempCancel(true);
            if (!pumpEnactResultProvidePumpEnactResult.getSuccess()) {
                setErrorMsg(this.lenomedPump.getResultErrorCode(), pumpEnactResultProvidePumpEnactResult);
            }
        } else {
            pumpEnactResultProvidePumpEnactResult.setSuccess(true);
            pumpEnactResultProvidePumpEnactResult.setEnacted(false);
            pumpEnactResultProvidePumpEnactResult.setTempCancel(true);
            pumpEnactResultProvidePumpEnactResult.setComment(((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.ok));
            getAapsLogger().debug(LTag.PUMP, "cancelRealTempBasal: OK");
        }
        return pumpEnactResultProvidePumpEnactResult;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public synchronized Object cancelExtendedBolus(Continuation<? super PumpEnactResult> continuation) {
        PumpEnactResult pumpEnactResultProvidePumpEnactResult;
        pumpEnactResultProvidePumpEnactResult = this.pumpEnactResultProvider.get();
        if (this.lenomedPump.isExtendedInProgress()) {
            LenomedService lenomedService = this.lenomedService;
            if (lenomedService != null) {
                lenomedService.extendedBolusStop();
            }
            pumpEnactResultProvidePumpEnactResult.setSuccess(!this.lenomedPump.isExtendedInProgress());
            pumpEnactResultProvidePumpEnactResult.setEnacted(true);
            if (!pumpEnactResultProvidePumpEnactResult.getSuccess()) {
                setErrorMsg(this.lenomedPump.getResultErrorCode(), pumpEnactResultProvidePumpEnactResult);
                LenomedService lenomedService2 = this.lenomedService;
                if (lenomedService2 != null) {
                    lenomedService2.readPumpStatus();
                }
            }
        } else {
            pumpEnactResultProvidePumpEnactResult.setSuccess(true);
            pumpEnactResultProvidePumpEnactResult.setEnacted(false);
            pumpEnactResultProvidePumpEnactResult.setComment(((ResourceHelper) getRh()).gs(app.aaps.core.ui.R.string.ok));
            getAapsLogger().debug(LTag.PUMP, "cancelExtendedBolus: OK");
        }
        return pumpEnactResultProvidePumpEnactResult;
    }

    public final JSONObject getJSONStatus(Profile profile, String profileName, String version) {
        Intrinsics.checkNotNullParameter(profile, "profile");
        Intrinsics.checkNotNullParameter(profileName, "profileName");
        Intrinsics.checkNotNullParameter(version, "version");
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (this.lenomedPump.getLastConnection() + 3600000 < System.currentTimeMillis()) {
            return new JSONObject();
        }
        JSONObject jSONObject = new JSONObject();
        JSONObject jSONObject2 = new JSONObject();
        JSONObject jSONObject3 = new JSONObject();
        JSONObject jSONObject4 = new JSONObject();
        try {
            jSONObject2.put("percent", this.lenomedPump.getSystemRemainBattery());
            jSONObject3.put("status", this.lenomedPump.getPumpSuspended() ? "suspended" : "normal");
            jSONObject3.put("timestamp", this.dateUtil.toISOString(this.lenomedPump.getLastConnection()));
            jSONObject4.put("Version", version);
            if (this.lenomedPump.getLastBolusTime() != 0) {
                jSONObject4.put("LastBolus", this.dateUtil.dateAndTimeString(this.lenomedPump.getLastBolusTime()));
                jSONObject4.put("LastBolusAmount", this.lenomedPump.getLastBolusAmount());
            }
            final Profile finalProfile = profile;
            // expectedPumpState() is suspend on dev; the Kotlin bridge runBlocking's it so the
            // checked InterruptedException never surfaces here (this try only catches JSONException).
            PumpSync.PumpState pumpState = LenomedSupportKt.expectedPumpStateBlocking(this.pumpSync);
            PumpSync.PumpState.TemporaryBasal temporaryBasal = pumpState != null ? pumpState.getTemporaryBasal() : null;
            if (temporaryBasal != null) {
                jSONObject4.put("TempBasalAbsoluteRate", temporaryBasal.convertedToAbsolute(jCurrentTimeMillis, finalProfile));
                jSONObject4.put("TempBasalStart", this.dateUtil.dateAndTimeString(temporaryBasal.getTimestamp()));
                jSONObject4.put("TempBasalRemaining", temporaryBasal.getPlannedRemainingMinutes());
            }
            PumpSync.PumpState.ExtendedBolus extendedBolus = pumpState != null ? pumpState.getExtendedBolus() : null;
            if (extendedBolus != null) {
                jSONObject4.put("ExtendedBolusAbsoluteRate", extendedBolus.getRate());
                jSONObject4.put("ExtendedBolusStart", this.dateUtil.dateAndTimeString(extendedBolus.getTimestamp()));
                jSONObject4.put("ExtendedBolusRemaining", extendedBolus.getPlannedRemainingMinutes());
            }
            jSONObject4.put("BaseBasalRate", getBaseBasalRateValue());
            try {
                jSONObject4.put("ActiveProfile", "");
            } catch (Exception e) {
                getAapsLogger().error("Unhandled exception", e);
            }
            jSONObject.put("battery", jSONObject2);
            jSONObject.put("status", jSONObject3);
            jSONObject.put("extended", jSONObject4);
            jSONObject.put("reservoir", (int) this.lenomedPump.getSystemRemainInsulin());
            jSONObject.put("clock", this.dateUtil.toISOString(jCurrentTimeMillis));
        } catch (JSONException e2) {
            getAapsLogger().error("Unhandled exception", e2);
        }
        return jSONObject;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public StateFlow<Long> getLastDataTime() {
        return this.lastDataTimeFlow;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public StateFlow<Long> getLastBolusTime() {
        return this.lastBolusTimeFlow;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public StateFlow<PumpInsulin> getLastBolusAmount() {
        return this.lastBolusAmountFlow;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public ManufacturerType manufacturer() {
        return ManufacturerType.G2e;
    }

    @Override // app.aaps.core.interfaces.pump.Pump, app.aaps.pump.common.sync.PumpSyncEntriesCreator
    public PumpType model() {
        return PumpType.LENOMED;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public String serialNumber() {
        return this.lenomedPump.getSerialNo();
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public String pumpSpecificShortStatus(boolean veryShort) {
        String str;
        if (this.lenomedPump.getLastConnection() == 0) {
            str = "";
        } else {
            str = "LastConn: " + ((int) (((System.currentTimeMillis() - this.lenomedPump.getLastConnection()) / 60.0d) / 1000.0d)) + " minago\n";
        }
        if (this.lenomedPump.getLastBolusTime() != 0) {
            str = str + "LastBolus: " + this.decimalFormatter.to2Decimal(this.lenomedPump.getLastBolusAmount()) + "U @" + ((Object) DateFormat.format("HH:mm", this.lenomedPump.getLastBolusTime()));
        }
        if (this.lenomedPump.isTempBasalInProgress()) {
            str = str + "Temp: " + this.lenomedPump.temporaryBasalToString();
        }
        if (this.lenomedPump.isExtendedInProgress()) {
            str = str + "Extended: " + this.lenomedPump.extendedBolusToString() + "\n";
        }
        if (!veryShort) {
            str = str + "TDD: " + this.decimalFormatter.to0Decimal(this.lenomedPump.getDailyTotalUnits()) + " / " + this.lenomedPump.getMaxDailyTotalUnits() + " U";
        }
        return (str + "Reserv: " + this.decimalFormatter.to0Decimal(this.lenomedPump.getSystemRemainInsulin()) + " U") + "Batt: " + this.lenomedPump.getSystemRemainBattery();
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public boolean isFakingTempsByExtendedBoluses() {
        return this.isFakingTempsByExtendedBoluses;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public Object loadTDDs(Continuation<? super PumpEnactResult> continuation) {
        return loadHistory((byte) 0);
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public boolean isBatteryChangeLoggingEnabled() {
        return this.sp.getBoolean(R.string.key_lenomed_logbatterychange, false);
    }

    public final synchronized void setErrorMsg(int errorCode, PumpEnactResult result) {
        Intrinsics.checkNotNullParameter(result, "result");
        if (errorCode == 1) {
            result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_1));
        } else if (errorCode == 2) {
            result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_2));
        } else if (errorCode == 3) {
            result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_3));
        } else if (errorCode == 4) {
            result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_4));
        } else {
            switch (errorCode) {
                case 6:
                    result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_6));
                    break;
                case 7:
                    result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_7));
                    break;
                case 8:
                    result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_8));
                    break;
                case 9:
                    result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_9));
                    break;
                case 10:
                    result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_10));
                    break;
                case 11:
                    result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_11));
                    break;
                case 12:
                    result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_12));
                    break;
                case 13:
                    result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_13));
                    break;
                case 14:
                    result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_14));
                    break;
                case 15:
                    result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_15));
                    break;
                default:
                    switch (errorCode) {
                        case 32:
                            result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_32));
                            break;
                        case 33:
                            result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_33));
                            break;
                        case 34:
                            result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_34));
                            break;
                        case 35:
                            result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_35));
                            break;
                        case 36:
                            result.setComment(((ResourceHelper) getRh()).gs(R.string.lenomed_errorcode_36));
                            break;
                        default:
                            result.setComment("not defined Error code: " + errorCode);
                            break;
                    }
                    break;
            }
        }
    }

    public void preprocessPreferences(PreferenceFragmentCompat preferenceFragment) {
        Intrinsics.checkNotNullParameter(preferenceFragment, "preferenceFragment");
        Preference preferenceFindPreference = preferenceFragment.findPreference(((ResourceHelper) getRh()).gs(R.string.key_lenomed_bolusspeed));
        if (preferenceFindPreference != null) {
            preferenceFindPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: app.aaps.pump.lenomed.LenomedPlugin$$ExternalSyntheticLambda0
                @Override // androidx.preference.Preference.OnPreferenceChangeListener
                public final boolean onPreferenceChange(Preference preference, Object obj) {
                    return LenomedPlugin.preprocessPreferences$lambda$0(LenomedPlugin.this, preference, obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean preprocessPreferences$lambda$0(LenomedPlugin lenomedPlugin, Preference preference, Object obj) {
        Intrinsics.checkNotNullParameter(preference, "<unused var>");
        int i = Integer.parseInt(obj.toString());
        lenomedPlugin.lenomedPump.setBolusSpeed(i);
        lenomedPlugin.lenomedPump.setSpeed(i);
        lenomedPlugin.lenomedPump.setSetUserOptionType(3);
        lenomedPlugin.sp.putBoolean(R.string.key_lenomed_is_bolus_speed_sync, false);
        return true;
    }

    @Override // app.aaps.core.interfaces.plugin.OwnDatabasePlugin
    public void clearAllTables() {
        this.diaconnHistoryDatabase.clearAllTables();
    }
}