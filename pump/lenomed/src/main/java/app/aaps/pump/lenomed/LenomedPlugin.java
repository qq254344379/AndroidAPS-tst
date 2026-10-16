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
import app.aaps.core.interfaces.objects.Instantiator;
import app.aaps.core.interfaces.plugin.OwnDatabasePlugin;
import app.aaps.core.interfaces.plugin.PluginDescription;
import app.aaps.core.interfaces.profile.Profile;
import app.aaps.core.interfaces.profile.ProfileFunction;
import app.aaps.core.interfaces.pump.DetailedBolusInfo;
import app.aaps.core.interfaces.pump.DetailedBolusInfoStorage;
import app.aaps.core.interfaces.pump.Lenomed;
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
import app.aaps.core.interfaces.rx.events.EventDismissNotification;
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
import ch.qos.logback.core.CoreConstants;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.socket.client.Socket;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import kotlin.Metadata;
import kotlin.NotImplementedError;
import kotlin.jvm.internal.Intrinsics;
import dev.zacsweers.metro.binding;
import dev.zacsweers.metro.SingleIn;
import dev.zacsweers.metro.MetroIntKey;
import dev.zacsweers.metro.ContributesIntoMap;
import dev.zacsweers.metro.AppScope;
import app.aaps.core.keys.interfaces.TextRef;
import app.aaps.core.interfaces.notifications.NotificationManager;
import app.aaps.core.interfaces.di.PumpDriver;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: compiled from: LenomedPlugin.kt */
/* JADX INFO: loaded from: classes4.dex */
@ContributesIntoMap(AppScope::class, binding = binding<PluginBase>())
@PumpDriver
@MetroIntKey(1200)
@SingleIn(AppScope::class)
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
    private final Instantiator instantiator;
    private final boolean isFakingTempsByExtendedBoluses;
    private final LenomedPump lenomedPump;
    private LenomedService lenomedService;
    private final ServiceConnection mConnection;
    private String mDeviceAddress;
    private String mDeviceName;
    private final ProfileFunction profileFunction;
    private final PumpDescription pumpDescription;
    private final PumpSync pumpSync;
    private final RxBus rxBus;
    private final SP sp;
    private final TemporaryBasalStorage temporaryBasalStorage;
    private final UiInteraction uiInteraction;

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
    @Inject
    public LenomedPlugin(final AAPSLogger aapsLogger, RxBus rxBus, Context context, ResourceHelper rh, Preferences preferences, ConstraintsChecker constraintChecker, ProfileFunction profileFunction, SP sp, CommandQueue commandQueue, LenomedPump lenomedPump, PumpSync pumpSync, DetailedBolusInfoStorage detailedBolusInfoStorage, TemporaryBasalStorage temporaryBasalStorage, FabricPrivacy fabricPrivacy, DateUtil dateUtil, AapsSchedulers aapsSchedulers, UiInteraction uiInteraction, LenomedHistoryDatabase diaconnHistoryDatabase, DecimalFormatter decimalFormatter, Instantiator instantiator, NotificationManager notificationManager) {        super(PluginDescription()
            .mainType(PluginType.PUMP)
            .pluginName(TextRef.AndroidRes(R.string.lenomed_pump))
            .description(TextRef.AndroidRes(R.string.description_pump_lenomed)),
            emptyList(), aapsLogger, rh, preferences, commandQueue, notificationManager);
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
        Intrinsics.checkNotNullParameter(instantiator, "instantiator");
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
        this.instantiator = instantiator;
        this.disposable = new CompositeDisposable();
        this.mDeviceAddress = "";
        this.mDeviceName = "";
        this.pumpDescription = PumpDescriptionExtensionKt.fillFor(new PumpDescription(), PumpType.LENOMED);
        this.mConnection = new ServiceConnection() { // from class: app.aaps.pump.lenomed.LenomedPlugin$mConnection$1
            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                Intrinsics.checkNotNullParameter(name, "name");
                aapsLogger.debug(LTag.PUMP, "Service is disconnected");
                this.lenomedService = null;
            }

            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                Intrinsics.checkNotNullParameter(name, "name");
                Intrinsics.checkNotNullParameter(service, "service");
                aapsLogger.debug(LTag.PUMP, "Service is connected");
                this.lenomedService = ((LenomedService.LocalBinder) service).getThis$0();
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
    public void onStart() {
        super.onStart();
        this.context.bindService(new Intent(this.context, (Class<?>) LenomedService.class), this.mConnection, 1);
        this.disposable.add(this.rxBus.toObservable(EventAppExit.class).observeOn(this.aapsSchedulers.getIo()).subscribe(new Consumer() { // from class: app.aaps.pump.lenomed.LenomedPlugin.onStart.1
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(EventAppExit it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedPlugin.this.context.unbindService(LenomedPlugin.this.mConnection);
            }
        }, new Consumer() { // from class: app.aaps.pump.lenomed.LenomedPlugin.onStart.2
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(Throwable it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedPlugin.this.fabricPrivacy.logException(it);
            }
        }));
        this.disposable.add(this.rxBus.toObservable(EventConfigBuilderChange.class).observeOn(this.aapsSchedulers.getIo()).subscribe(new Consumer() { // from class: app.aaps.pump.lenomed.LenomedPlugin.onStart.3
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(EventConfigBuilderChange it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedPlugin.this.lenomedPump.reset();
            }
        }));
        this.disposable.add(this.rxBus.toObservable(EventEmbectaDeviceChange.class).observeOn(this.aapsSchedulers.getIo()).subscribe(new Consumer() { // from class: app.aaps.pump.lenomed.LenomedPlugin.onStart.4
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(EventEmbectaDeviceChange it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedPlugin.this.changePump();
            }
        }, new Consumer() { // from class: app.aaps.pump.lenomed.LenomedPlugin.onStart.5
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(Throwable it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedPlugin.this.fabricPrivacy.logException(it);
            }
        }));
        changePump();
    }

    @Override // app.aaps.core.interfaces.pump.PumpPluginBase, app.aaps.core.interfaces.plugin.PluginBase
    public void onStop() {
        this.context.unbindService(this.mConnection);
        this.disposable.clear();
        super.onStop();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void changePump() {
        this.mDeviceAddress = this.sp.getString(app.aaps.core.utils.R.string.key_pump_lenomed_address, "");
        this.mDeviceName = this.sp.getString(app.aaps.core.utils.R.string.key_pump_lenomed_name, "");
        this.lenomedPump.reset();
        getCommandQueue().readStatus(getRh().gs(app.aaps.core.ui.R.string.device_changed), null);
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
    public void getPumpStatus(String reason) {
        Intrinsics.checkNotNullParameter(reason, "reason");
        LenomedService lenomedService = this.lenomedService;
        if (lenomedService != null) {
            lenomedService.readPumpStatus();
        }
        getPumpDescription().setBasalStep(this.lenomedPump.getBasalStep());
        getPumpDescription().setBolusStep(this.lenomedPump.getBolusStep());
        getPumpDescription().setBasalMaximumRate(4.0d);
    }

    @Override // app.aaps.core.interfaces.pump.Lenomed
    public PumpEnactResult loadHistory(byte type) {
        PumpEnactResult pumpEnactResultLoadHistory$default;
        LenomedService lenomedService = this.lenomedService;
        return (lenomedService == null || (pumpEnactResultLoadHistory$default = LenomedService.loadHistory$default(lenomedService, type, false, 2, null)) == null) ? this.instantiator.providePumpEnactResult().success(false) : pumpEnactResultLoadHistory$default;
    }

    @Override // app.aaps.core.interfaces.pump.Lenomed
    public PumpEnactResult loadBolusHistory() {
        PumpEnactResult pumpEnactResultLoadBolusHistory;
        LenomedService lenomedService = this.lenomedService;
        return (lenomedService == null || (pumpEnactResultLoadBolusHistory = lenomedService.loadBolusHistory()) == null) ? this.instantiator.providePumpEnactResult().success(false) : pumpEnactResultLoadBolusHistory;
    }

    @Override // app.aaps.core.interfaces.pump.Lenomed
    public PumpEnactResult setUserOptions() {
        PumpEnactResult userSettings;
        LenomedService lenomedService = this.lenomedService;
        return (lenomedService == null || (userSettings = lenomedService.setUserSettings()) == null) ? this.instantiator.providePumpEnactResult().success(false) : userSettings;
    }

    @Override // app.aaps.core.interfaces.constraints.PluginConstraints
    public Constraint<Double> applyBasalConstraints(Constraint<Double> absoluteRate, Profile profile) {
        Intrinsics.checkNotNullParameter(absoluteRate, "absoluteRate");
        Intrinsics.checkNotNullParameter(profile, "profile");
        absoluteRate.setIfSmaller(Double.valueOf(this.lenomedPump.getMaxBasal()), getRh().gs(app.aaps.core.ui.R.string.limitingbasalratio, Double.valueOf(this.lenomedPump.getMaxBasal()), getRh().gs(app.aaps.core.ui.R.string.pumplimit)), this);
        return absoluteRate;
    }

    @Override // app.aaps.core.interfaces.constraints.PluginConstraints
    public Constraint<Integer> applyBasalPercentConstraints(Constraint<Integer> percentRate, Profile profile) {
        Intrinsics.checkNotNullParameter(percentRate, "percentRate");
        Intrinsics.checkNotNullParameter(profile, "profile");
        percentRate.setIfGreater((Comparable) 0, getRh().gs(app.aaps.core.ui.R.string.limitingpercentrate, 0, getRh().gs(app.aaps.core.ui.R.string.itmustbepositivevalue)), this);
        percentRate.setIfSmaller(Integer.valueOf(getPumpDescription().getMaxTempPercent()), getRh().gs(app.aaps.core.ui.R.string.limitingpercentrate, Integer.valueOf(getPumpDescription().getMaxTempPercent()), getRh().gs(app.aaps.core.ui.R.string.pumplimit)), this);
        return percentRate;
    }

    @Override // app.aaps.core.interfaces.constraints.PluginConstraints
    public Constraint<Double> applyBolusConstraints(Constraint<Double> insulin) {
        Intrinsics.checkNotNullParameter(insulin, "insulin");
        insulin.setIfSmaller(Double.valueOf(this.lenomedPump.getMaxBolus()), getRh().gs(app.aaps.core.ui.R.string.limitingbolus, Double.valueOf(this.lenomedPump.getMaxBolus()), getRh().gs(app.aaps.core.ui.R.string.pumplimit)), this);
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
    public PumpEnactResult setNewBasalProfile(Profile profile) {
        Intrinsics.checkNotNullParameter(profile, "profile");
        PumpEnactResult pumpEnactResultProvidePumpEnactResult = this.instantiator.providePumpEnactResult();
        if (!isInitialized()) {
            this.uiInteraction.addNotification(5, getRh().gs(app.aaps.core.ui.R.string.pump_not_initialized_profile_not_set), 0);
            pumpEnactResultProvidePumpEnactResult.setComment(getRh().gs(app.aaps.core.ui.R.string.pump_not_initialized_profile_not_set));
            return pumpEnactResultProvidePumpEnactResult;
        }
        this.rxBus.send(new EventDismissNotification(5));
        LenomedService lenomedService = this.lenomedService;
        if (lenomedService == null || !lenomedService.updateBasalsInPump(profile)) {
            this.uiInteraction.addNotification(6, getRh().gs(app.aaps.core.ui.R.string.failed_update_basal_profile), 0);
            pumpEnactResultProvidePumpEnactResult.setComment(getRh().gs(app.aaps.core.ui.R.string.failed_update_basal_profile));
            return pumpEnactResultProvidePumpEnactResult;
        }
        this.rxBus.send(new EventDismissNotification(5));
        this.rxBus.send(new EventDismissNotification(6));
        this.uiInteraction.addNotificationValidFor(1, getRh().gs(app.aaps.core.ui.R.string.profile_set_ok), 3, 60);
        pumpEnactResultProvidePumpEnactResult.setSuccess(true);
        pumpEnactResultProvidePumpEnactResult.setEnacted(true);
        pumpEnactResultProvidePumpEnactResult.setComment("OK");
        return pumpEnactResultProvidePumpEnactResult;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public boolean isThisProfileSet(Profile profile) {
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
    public double getBaseBasalRate() {
        return this.lenomedPump.getBaseAmount();
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public double getReservoirLevel() {
        return this.lenomedPump.getSystemRemainInsulin();
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public Integer getBatteryLevel() {
        return Integer.valueOf(this.lenomedPump.getSystemRemainBattery());
    }

    /* JADX WARN: Removed duplicated region for block: B:34:0x0134 A[Catch: all -> 0x0198, TryCatch #0 {, blocks: (B:4:0x0009, B:6:0x007a, B:8:0x0080, B:10:0x00af, B:12:0x00b6, B:14:0x00bc, B:15:0x00c9, B:19:0x00df, B:32:0x011e, B:34:0x0134, B:35:0x0137, B:37:0x013d, B:39:0x0154, B:38:0x0147, B:26:0x0104, B:28:0x0108, B:11:0x00b4, B:42:0x017c, B:43:0x0189, B:44:0x018a, B:45:0x0197), top: B:49:0x0009 }] */
    /* JADX WARN: Removed duplicated region for block: B:37:0x013d A[Catch: all -> 0x0198, TryCatch #0 {, blocks: (B:4:0x0009, B:6:0x007a, B:8:0x0080, B:10:0x00af, B:12:0x00b6, B:14:0x00bc, B:15:0x00c9, B:19:0x00df, B:32:0x011e, B:34:0x0134, B:35:0x0137, B:37:0x013d, B:39:0x0154, B:38:0x0147, B:26:0x0104, B:28:0x0108, B:11:0x00b4, B:42:0x017c, B:43:0x0189, B:44:0x018a, B:45:0x0197), top: B:49:0x0009 }] */
    /* JADX WARN: Removed duplicated region for block: B:38:0x0147 A[Catch: all -> 0x0198, TryCatch #0 {, blocks: (B:4:0x0009, B:6:0x007a, B:8:0x0080, B:10:0x00af, B:12:0x00b6, B:14:0x00bc, B:15:0x00c9, B:19:0x00df, B:32:0x011e, B:34:0x0134, B:35:0x0137, B:37:0x013d, B:39:0x0154, B:38:0x0147, B:26:0x0104, B:28:0x0108, B:11:0x00b4, B:42:0x017c, B:43:0x0189, B:44:0x018a, B:45:0x0197), top: B:49:0x0009 }] */
    @Override // app.aaps.core.interfaces.pump.Pump
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized PumpEnactResult deliverTreatment(DetailedBolusInfo detailedBolusInfo) {
        boolean z;
        EventOverviewBolusProgress.Treatment treatment;
        boolean z2;
        PumpEnactResult pumpEnactResultProvidePumpEnactResult;
        Intrinsics.checkNotNullParameter(detailedBolusInfo, "detailedBolusInfo");
        getAapsLogger().debug(LTag.PUMP, "deliverTreatment: Starting bolus delivery. Insulin: " + detailedBolusInfo.insulin + " U, BolusType: " + detailedBolusInfo.getBolusType() + ", Carbs: " + detailedBolusInfo.carbs + " g, Timestamp: " + this.dateUtil.dateAndTimeString(detailedBolusInfo.timestamp) + ", BolusBlocked: " + this.lenomedPump.getBolusBlocked() + ", BolusStep: " + this.lenomedPump.getBolusStep());
        if (detailedBolusInfo.carbs != 0.0d) {
            throw new IllegalArgumentException(detailedBolusInfo.toString().toString());
        }
        if (detailedBolusInfo.insulin <= 0.0d) {
            throw new IllegalArgumentException(detailedBolusInfo.toString().toString());
        }
        detailedBolusInfo.insulin = ((Number) this.constraintChecker.applyBolusConstraints(new ConstraintObject(Double.valueOf(detailedBolusInfo.insulin), getAapsLogger())).value()).doubleValue();
        double d = detailedBolusInfo.carbs;
        detailedBolusInfo.carbs = 0.0d;
        Long carbsTimestamp = detailedBolusInfo.getCarbsTimestamp();
        long jLongValue = carbsTimestamp != null ? carbsTimestamp.longValue() : detailedBolusInfo.timestamp;
        if (jLongValue == detailedBolusInfo.timestamp) {
            jLongValue -= T.INSTANCE.mins(1L).msecs();
        }
        this.detailedBolusInfoStorage.add(detailedBolusInfo);
        EventOverviewBolusProgress.Treatment treatment2 = new EventOverviewBolusProgress.Treatment(0.0d, 0, detailedBolusInfo.getBolusType() == BS.Type.SMB, Long.valueOf(detailedBolusInfo.getId()));
        if (detailedBolusInfo.insulin > 0.0d || d > 0.0d) {
            LenomedService lenomedService = this.lenomedService;
            if (lenomedService != null) {
                z = true;
                treatment = treatment2;
                if (lenomedService.bolus(detailedBolusInfo.insulin, (int) d, jLongValue, treatment)) {
                    z2 = true;
                }
                pumpEnactResultProvidePumpEnactResult = this.instantiator.providePumpEnactResult();
                pumpEnactResultProvidePumpEnactResult.setSuccess(z2);
                pumpEnactResultProvidePumpEnactResult.setBolusDelivered(treatment.getInsulin());
                if (pumpEnactResultProvidePumpEnactResult.getSuccess()) {
                    pumpEnactResultProvidePumpEnactResult.setEnacted(z);
                }
                if (pumpEnactResultProvidePumpEnactResult.getSuccess()) {
                    setErrorMsg(this.lenomedPump.getResultErrorCode(), pumpEnactResultProvidePumpEnactResult);
                } else {
                    pumpEnactResultProvidePumpEnactResult.setComment(getRh().gs(app.aaps.core.ui.R.string.ok));
                }
                getAapsLogger().debug(LTag.PUMP, "deliverTreatment: OK. Asked: " + detailedBolusInfo.insulin + " Delivered: " + pumpEnactResultProvidePumpEnactResult.getBolusDelivered());
            } else {
                z = true;
                treatment = treatment2;
            }
        } else {
            z = true;
            treatment = treatment2;
        }
        z2 = false;
        pumpEnactResultProvidePumpEnactResult = this.instantiator.providePumpEnactResult();
        pumpEnactResultProvidePumpEnactResult.setSuccess(z2);
        pumpEnactResultProvidePumpEnactResult.setBolusDelivered(treatment.getInsulin());
        if (pumpEnactResultProvidePumpEnactResult.getSuccess()) {
        }
        if (pumpEnactResultProvidePumpEnactResult.getSuccess()) {
        }
        getAapsLogger().debug(LTag.PUMP, "deliverTreatment: OK. Asked: " + detailedBolusInfo.insulin + " Delivered: " + pumpEnactResultProvidePumpEnactResult.getBolusDelivered());
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
    public synchronized PumpEnactResult setTempBasalAbsolute(double absoluteRate, int durationInMinutes, Profile profile, boolean enforceNew, PumpSync.TemporaryBasalType tbrType) {
        boolean z;
        boolean z2;
        Intrinsics.checkNotNullParameter(profile, "profile");
        Intrinsics.checkNotNullParameter(tbrType, "tbrType");
        PumpEnactResult pumpEnactResultProvidePumpEnactResult = this.instantiator.providePumpEnactResult();
        double dDoubleValue = ((Number) this.constraintChecker.applyBasalConstraints(new ConstraintObject(Double.valueOf(absoluteRate), getAapsLogger()), profile).value()).doubleValue();
        boolean z3 = getBaseBasalRate() - dDoubleValue == 0.0d;
        boolean z4 = dDoubleValue < getBaseBasalRate();
        boolean z5 = dDoubleValue > getBaseBasalRate();
        if (z3) {
            if (this.lenomedPump.isTempBasalInProgress()) {
                getAapsLogger().debug(LTag.PUMP, "setTempBasalAbsolute: Stopping temp basal (doTempOff)");
                return cancelTempBasal(false);
            }
            pumpEnactResultProvidePumpEnactResult.setSuccess(true);
            pumpEnactResultProvidePumpEnactResult.setEnacted(false);
            pumpEnactResultProvidePumpEnactResult.setAbsolute(getBaseBasalRate());
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
            this.temporaryBasalStorage.add(new PumpSync.PumpState.TemporaryBasal(this.dateUtil.now(), T.INSTANCE.mins(durationInMinutes).msecs(), absoluteRate, true, tbrType, 0L, 0L, null, null, 384, null));
            getAapsLogger().debug(LTag.PUMP, "setTempBasalAbsolute: Setting temp basal " + dDoubleValue + " U for " + durationInMinutes + " mins (doLowTemp || doHighTemp)");
            if (durationInMinutes == 15 || durationInMinutes == 30) {
                LenomedService lenomedService = this.lenomedService;
                z = lenomedService != null && lenomedService.tempBasalShortDuration(dDoubleValue, durationInMinutes);
            } else {
                double dMax = Math.max(((double) durationInMinutes) / 60.0d, 1.0d);
                LenomedService lenomedService2 = this.lenomedService;
                if (lenomedService2 != null && lenomedService2.tempBasal(dDoubleValue, dMax)) {
                }
            }
            getAapsLogger().debug(LTag.PUMP, "setTempBasalAbsolute: Validation check - isTempBasalInProgress=" + this.lenomedPump.isTempBasalInProgress() + ", tempBasalAbsoluteRate=" + this.lenomedPump.getTempBasalAbsoluteRate() + ", expected rate=" + dDoubleValue);
            if (z && (Math.abs(dDoubleValue) < 0.1d || (this.lenomedPump.isTempBasalInProgress() && Math.abs(this.lenomedPump.getTempBasalAbsoluteRate() - dDoubleValue) < 0.1d))) {
                pumpEnactResultProvidePumpEnactResult.setEnacted(true);
                pumpEnactResultProvidePumpEnactResult.setSuccess(true);
                pumpEnactResultProvidePumpEnactResult.setComment(getRh().gs(app.aaps.core.ui.R.string.ok));
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
        pumpEnactResultProvidePumpEnactResult.setComment(getRh().gs(app.aaps.core.ui.R.string.temp_basal_delivery_error));
        getAapsLogger().error("setTempBasalAbsolute: Failed to set temp basal");
        return pumpEnactResultProvidePumpEnactResult;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public synchronized PumpEnactResult setTempBasalPercent(int percent, int durationInMinutes, Profile profile, boolean enforceNew, PumpSync.TemporaryBasalType tbrType) throws Throwable {
        LenomedPlugin lenomedPlugin;
        LenomedPlugin lenomedPlugin2;
        PumpEnactResult tempBasalAbsolute;
        try {
            try {
                Intrinsics.checkNotNullParameter(profile, "profile");
                Intrinsics.checkNotNullParameter(tbrType, "tbrType");
                if (percent == 0) {
                    try {
                        tempBasalAbsolute = setTempBasalAbsolute(0.0d, durationInMinutes, profile, enforceNew, tbrType);
                        lenomedPlugin2 = this;
                    } catch (Throwable th) {
                        th = th;
                        lenomedPlugin = this;
                        Throwable th2 = th;
                        throw th2;
                    }
                } else {
                    lenomedPlugin2 = this;
                    double dDetermineCorrectBasalSize = PumpTypeExtensionKt.determineCorrectBasalSize(lenomedPlugin2.getPumpDescription().getPumpType(), profile.getBasal() * (((double) percent) / 100.0d));
                    lenomedPlugin2.getAapsLogger().warn(LTag.PUMP, "setTempBasalPercent [DiaconnG8Plugin] - You are trying to use setTempBasalPercent with percent other then 0% (" + percent + "). This will start setTempBasalAbsolute, with calculated value (" + dDetermineCorrectBasalSize + "). Result might not be 100% correct.");
                    tempBasalAbsolute = lenomedPlugin2.setTempBasalAbsolute(dDetermineCorrectBasalSize, durationInMinutes, profile, enforceNew, tbrType);
                }
                return tempBasalAbsolute;
            } catch (Throwable th3) {
                th = th3;
                lenomedPlugin = this;
            }
        } catch (Throwable th4) {
            th = th4;
        }
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public synchronized PumpEnactResult setExtendedBolus(double insulin, int durationInMinutes) {
        double dRoundTo = Round.INSTANCE.roundTo(((Number) this.constraintChecker.applyExtendedBolusConstraints(new ConstraintObject(Double.valueOf(insulin), getAapsLogger())).value()).doubleValue(), getPumpDescription().getExtendedBolusStep());
        PumpEnactResult pumpEnactResultProvidePumpEnactResult = this.instantiator.providePumpEnactResult();
        if (this.lenomedPump.isExtendedInProgress() && Math.abs(this.lenomedPump.getExtendedBolusAmount() - dRoundTo) < getPumpDescription().getExtendedBolusStep()) {
            pumpEnactResultProvidePumpEnactResult.setEnacted(false);
            pumpEnactResultProvidePumpEnactResult.setSuccess(true);
            pumpEnactResultProvidePumpEnactResult.setComment(getRh().gs(app.aaps.core.ui.R.string.ok));
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
            pumpEnactResultProvidePumpEnactResult.setComment(getRh().gs(app.aaps.core.ui.R.string.ok));
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

    @Override // app.aaps.core.interfaces.pump.Pump
    public PumpEnactResult setSquareWaveBolus(double insulin, int durationInMinutes, int durationBloodInMinutes) {
        throw new NotImplementedError("An operation is not implemented: Not yet implemented");
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public PumpEnactResult setDoubleWaveBolus(double insulin, int durationInMinutes, int durationBloodInMinutes) {
        throw new NotImplementedError("An operation is not implemented: Not yet implemented");
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public PumpEnactResult setSyncPumpTime() {
        throw new NotImplementedError("An operation is not implemented: Not yet implemented");
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public PumpEnactResult setPauseResumePump(int type) {
        throw new NotImplementedError("An operation is not implemented: Not yet implemented");
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public synchronized PumpEnactResult cancelTempBasal(boolean enforceNew) {
        PumpEnactResult pumpEnactResultProvidePumpEnactResult;
        pumpEnactResultProvidePumpEnactResult = this.instantiator.providePumpEnactResult();
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
            pumpEnactResultProvidePumpEnactResult.setComment(getRh().gs(app.aaps.core.ui.R.string.ok));
            getAapsLogger().debug(LTag.PUMP, "cancelRealTempBasal: OK");
        }
        return pumpEnactResultProvidePumpEnactResult;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public synchronized PumpEnactResult cancelExtendedBolus() {
        PumpEnactResult pumpEnactResultProvidePumpEnactResult;
        pumpEnactResultProvidePumpEnactResult = this.instantiator.providePumpEnactResult();
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
            pumpEnactResultProvidePumpEnactResult.setComment(getRh().gs(app.aaps.core.ui.R.string.ok));
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
            PumpSync.PumpState.TemporaryBasal temporaryBasal = this.pumpSync.expectedPumpState().getTemporaryBasal();
            if (temporaryBasal != null) {
                jSONObject4.put("TempBasalAbsoluteRate", temporaryBasal.convertedToAbsolute(jCurrentTimeMillis, profile));
                jSONObject4.put("TempBasalStart", this.dateUtil.dateAndTimeString(temporaryBasal.getTimestamp()));
                jSONObject4.put("TempBasalRemaining", temporaryBasal.getPlannedRemainingMinutes());
            }
            PumpSync.PumpState.ExtendedBolus extendedBolus = this.pumpSync.expectedPumpState().getExtendedBolus();
            if (extendedBolus != null) {
                jSONObject4.put("ExtendedBolusAbsoluteRate", extendedBolus.getRate());
                jSONObject4.put("ExtendedBolusStart", this.dateUtil.dateAndTimeString(extendedBolus.getTimestamp()));
                jSONObject4.put("ExtendedBolusRemaining", extendedBolus.getPlannedRemainingMinutes());
            }
            jSONObject4.put("BaseBasalRate", getBaseBasalRate());
            try {
                jSONObject4.put("ActiveProfile", this.profileFunction.getProfileName());
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
    public long getLastDataTime() {
        return this.lenomedPump.getLastConnection();
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public Long getLastBolusTime() {
        Long lValueOf = Long.valueOf(this.lenomedPump.getLastBolusTime());
        if (lValueOf.longValue() != 0) {
            return lValueOf;
        }
        return null;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public Double getLastBolusAmount() {
        Double dValueOf = Double.valueOf(this.lenomedPump.getLastBolusAmount());
        if (dValueOf.doubleValue() == 0.0d) {
            return null;
        }
        return dValueOf;
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

    public final String shortStatus(boolean veryShort) {
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
    /* JADX INFO: renamed from: isFakingTempsByExtendedBoluses, reason: from getter */
    public boolean getIsFakingTempsByExtendedBoluses() {
        return this.isFakingTempsByExtendedBoluses;
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public PumpEnactResult loadTDDs() {
        return loadHistory((byte) 0);
    }

    @Override // app.aaps.core.interfaces.pump.Pump
    public boolean isBatteryChangeLoggingEnabled() {
        return this.sp.getBoolean(R.string.key_lenomed_logbatterychange, false);
    }

    public final synchronized void setErrorMsg(int errorCode, PumpEnactResult result) {
        Intrinsics.checkNotNullParameter(result, "result");
        if (errorCode == 1) {
            result.setComment(getRh().gs(R.string.lenomed_errorcode_1));
        } else if (errorCode == 2) {
            result.setComment(getRh().gs(R.string.lenomed_errorcode_2));
        } else if (errorCode == 3) {
            result.setComment(getRh().gs(R.string.lenomed_errorcode_3));
        } else if (errorCode == 4) {
            result.setComment(getRh().gs(R.string.lenomed_errorcode_4));
        } else {
            switch (errorCode) {
                case 6:
                    result.setComment(getRh().gs(R.string.lenomed_errorcode_6));
                    break;
                case 7:
                    result.setComment(getRh().gs(R.string.lenomed_errorcode_7));
                    break;
                case 8:
                    result.setComment(getRh().gs(R.string.lenomed_errorcode_8));
                    break;
                case 9:
                    result.setComment(getRh().gs(R.string.lenomed_errorcode_9));
                    break;
                case 10:
                    result.setComment(getRh().gs(R.string.lenomed_errorcode_10));
                    break;
                case 11:
                    result.setComment(getRh().gs(R.string.lenomed_errorcode_11));
                    break;
                case 12:
                    result.setComment(getRh().gs(R.string.lenomed_errorcode_12));
                    break;
                case 13:
                    result.setComment(getRh().gs(R.string.lenomed_errorcode_13));
                    break;
                case 14:
                    result.setComment(getRh().gs(R.string.lenomed_errorcode_14));
                    break;
                case 15:
                    result.setComment(getRh().gs(R.string.lenomed_errorcode_15));
                    break;
                default:
                    switch (errorCode) {
                        case 32:
                            result.setComment(getRh().gs(R.string.lenomed_errorcode_32));
                            break;
                        case 33:
                            result.setComment(getRh().gs(R.string.lenomed_errorcode_33));
                            break;
                        case 34:
                            result.setComment(getRh().gs(R.string.lenomed_errorcode_34));
                            break;
                        case 35:
                            result.setComment(getRh().gs(R.string.lenomed_errorcode_35));
                            break;
                        case 36:
                            result.setComment(getRh().gs(R.string.lenomed_errorcode_36));
                            break;
                        default:
                            result.setComment("not defined Error code: " + errorCode);
                            break;
                    }
                    break;
            }
        }
    }

    @Override // app.aaps.core.interfaces.plugin.PluginBase
    public void preprocessPreferences(PreferenceFragmentCompat preferenceFragment) {
        Intrinsics.checkNotNullParameter(preferenceFragment, "preferenceFragment");
        Preference preferenceFindPreference = preferenceFragment.findPreference(getRh().gs(R.string.key_lenomed_bolusspeed));
        if (preferenceFindPreference != null) {
            preferenceFindPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: app.aaps.pump.lenomed.LenomedPlugin$$ExternalSyntheticLambda0
                @Override // androidx.preference.Preference.OnPreferenceChangeListener
                public final boolean onPreferenceChange(Preference preference, Object obj) {
                    return LenomedPlugin.preprocessPreferences$lambda$0(this.f$0, preference, obj);
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