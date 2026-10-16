package app.aaps.pump.lenomed.service;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import app.aaps.core.data.pump.defs.PumpType;
import app.aaps.core.data.time.T;
import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.objects.Instantiator;
import app.aaps.core.interfaces.plugin.ActivePlugin;
import app.aaps.core.interfaces.profile.Profile;
import app.aaps.core.interfaces.profile.ProfileFunction;
import app.aaps.core.interfaces.pump.BolusProgressData;
import app.aaps.core.interfaces.pump.Pump;
import app.aaps.core.interfaces.pump.PumpEnactResult;
import app.aaps.core.interfaces.pump.PumpSync;
import app.aaps.core.interfaces.queue.Command;
import app.aaps.core.interfaces.queue.CommandQueue;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.rx.AapsSchedulers;
import app.aaps.core.interfaces.rx.bus.RxBus;
import app.aaps.core.interfaces.rx.events.EventAppExit;
import app.aaps.core.interfaces.rx.events.EventInitializationChanged;
import app.aaps.core.interfaces.rx.events.EventOverviewBolusProgress;
import app.aaps.core.interfaces.rx.events.EventProfileSwitchChanged;
import app.aaps.core.interfaces.rx.events.EventPumpStatusChanged;
import app.aaps.core.interfaces.sharedPreferences.SP;
import app.aaps.core.interfaces.ui.UiInteraction;
import app.aaps.core.interfaces.utils.DateUtil;
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy;
import app.aaps.database.entities.TableNamesKt;
import app.aaps.pump.lenomed.LenomedPlugin;
import app.aaps.pump.lenomed.LenomedPump;
import app.aaps.pump.lenomed.R;
import app.aaps.pump.lenomed.api.LenomedLogUploader;
import app.aaps.pump.lenomed.database.LenomedHistoryRecordDao;
import app.aaps.pump.lenomed.events.EventEmbectaNewStatus;
import app.aaps.pump.lenomed.packet.BasalLowerHalfSettingPacket;
import app.aaps.pump.lenomed.packet.BasalUpperHalfSettingPacket;
import app.aaps.pump.lenomed.packet.BigAPSMainInfoInquirePacket;
import app.aaps.pump.lenomed.packet.BigLogInquirePacket;
import app.aaps.pump.lenomed.packet.BluetoothSwitchPacket;
import app.aaps.pump.lenomed.packet.InjectionCancelSettingPacket;
import app.aaps.pump.lenomed.packet.InjectionExtendedBolusSettingPacket;
import app.aaps.pump.lenomed.packet.InjectionSnackSettingPacket;
import app.aaps.pump.lenomed.packet.LenomedInfusionSwitchPacket;
import app.aaps.pump.lenomed.packet.LenomedPacket;
import app.aaps.pump.lenomed.packet.TempBasalInquirePacket;
import app.aaps.pump.lenomed.packet.TempBasalSettingPacket;
import app.aaps.pump.lenomed.packet.ThresholdSettingPacket;
import app.aaps.pump.lenomed.packet.TimeSettingPacket;
import ch.qos.logback.core.CoreConstants;
import dagger.android.DaggerService;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.socket.client.Socket;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.math.MathKt;
import kotlin.ranges.RangesKt;
import net.glxn.qrgen.core.scheme.EnterpriseWifi;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/* JADX INFO: compiled from: LenomedService.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000\u0092\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0005\n\u0002\b\u0005\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001:\u0002¸\u0001B\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\n\u0010\u0082\u0001\u001a\u00030\u0083\u0001H\u0016J\u0013\u0010\u0084\u0001\u001a\u00020\u007f2\b\u0010\u0085\u0001\u001a\u00030\u0086\u0001H\u0016J(\u0010\u0087\u0001\u001a\u00030\u0088\u00012\b\u0010\u0085\u0001\u001a\u00030\u0086\u00012\b\u0010\u0089\u0001\u001a\u00030\u0088\u00012\b\u0010\u008a\u0001\u001a\u00030\u0088\u0001H\u0016J\n\u0010\u008b\u0001\u001a\u00030\u0083\u0001H\u0016J\u001c\u0010\u0090\u0001\u001a\u00030\u008d\u00012\b\u0010\u0091\u0001\u001a\u00030\u0092\u00012\b\u0010\u0093\u0001\u001a\u00030\u0092\u0001J\b\u0010\u0094\u0001\u001a\u00030\u0083\u0001J\u0012\u0010\u0095\u0001\u001a\u00030\u0083\u00012\b\u0010\u0091\u0001\u001a\u00030\u0092\u0001J\u001c\u0010\u0096\u0001\u001a\u00030\u0083\u00012\b\u0010\u0097\u0001\u001a\u00030\u0098\u00012\b\u0010\u0099\u0001\u001a\u00030\u0081\u0001J\u0014\u0010\u0096\u0001\u001a\u00030\u0083\u00012\b\u0010\u0097\u0001\u001a\u00030\u0098\u0001H\u0002J\n\u0010\u009a\u0001\u001a\u00030\u0083\u0001H\u0002J\b\u0010\u009b\u0001\u001a\u00030\u0083\u0001J \u0010\u009c\u0001\u001a\u00030\u009d\u00012\n\b\u0002\u0010\u009e\u0001\u001a\u00030\u009f\u00012\n\b\u0002\u0010 \u0001\u001a\u00030\u008d\u0001J\b\u0010¡\u0001\u001a\u00030\u009d\u0001J\b\u0010¢\u0001\u001a\u00030\u009d\u0001J2\u0010£\u0001\u001a\u00030\u008d\u00012\b\u0010¤\u0001\u001a\u00030¥\u00012\b\u0010¦\u0001\u001a\u00030\u0088\u00012\b\u0010§\u0001\u001a\u00030\u0081\u00012\b\u0010¨\u0001\u001a\u00030©\u0001H\u0007J\b\u0010ª\u0001\u001a\u00030\u0083\u0001J\u001c\u0010«\u0001\u001a\u00030\u008d\u00012\b\u0010¬\u0001\u001a\u00030¥\u00012\b\u0010\u00ad\u0001\u001a\u00030¥\u0001J\u001c\u0010®\u0001\u001a\u00030\u008d\u00012\b\u0010¬\u0001\u001a\u00030¥\u00012\b\u0010¯\u0001\u001a\u00030\u0088\u0001J\b\u0010°\u0001\u001a\u00030\u008d\u0001J\u001c\u0010±\u0001\u001a\u00030\u008d\u00012\b\u0010¤\u0001\u001a\u00030¥\u00012\b\u0010¯\u0001\u001a\u00030\u0088\u0001J\b\u0010²\u0001\u001a\u00030\u008d\u0001J\u0012\u0010³\u0001\u001a\u00030\u008d\u00012\b\u0010´\u0001\u001a\u00030µ\u0001J\u0014\u0010¶\u0001\u001a\u00030\u008d\u00012\b\u0010·\u0001\u001a\u00030\u009f\u0001H\u0002R\u001e\u0010\u0004\u001a\u00020\u00058\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\tR\u001e\u0010\n\u001a\u00020\u000b8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u001e\u0010\u0010\u001a\u00020\u00118\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u001e\u0010\u0016\u001a\u00020\u00178\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0018\u0010\u0019\"\u0004\b\u001a\u0010\u001bR\u001e\u0010\u001c\u001a\u00020\u001d8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u001e\u0010\u001f\"\u0004\b \u0010!R\u001e\u0010\"\u001a\u00020#8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b$\u0010%\"\u0004\b&\u0010'R\u001e\u0010(\u001a\u00020)8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b*\u0010+\"\u0004\b,\u0010-R\u001e\u0010.\u001a\u00020/8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b0\u00101\"\u0004\b2\u00103R\u001e\u00104\u001a\u0002058\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b6\u00107\"\u0004\b8\u00109R\u001e\u0010:\u001a\u00020;8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b<\u0010=\"\u0004\b>\u0010?R\u001e\u0010@\u001a\u00020A8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\bB\u0010C\"\u0004\bD\u0010ER\u001e\u0010F\u001a\u00020G8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\bH\u0010I\"\u0004\bJ\u0010KR\u001e\u0010L\u001a\u00020M8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\bN\u0010O\"\u0004\bP\u0010QR\u001e\u0010R\u001a\u00020S8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\bT\u0010U\"\u0004\bV\u0010WR\u001e\u0010X\u001a\u00020Y8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\bZ\u0010[\"\u0004\b\\\u0010]R\u001e\u0010^\u001a\u00020_8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b`\u0010a\"\u0004\bb\u0010cR\u001e\u0010d\u001a\u00020e8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\bf\u0010g\"\u0004\bh\u0010iR\u001e\u0010j\u001a\u00020k8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\bl\u0010m\"\u0004\bn\u0010oR\u001e\u0010p\u001a\u00020q8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\br\u0010s\"\u0004\bt\u0010uR\u001e\u0010v\u001a\u00020w8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\bx\u0010y\"\u0004\bz\u0010{R\u000e\u0010|\u001a\u00020}X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010~\u001a\u00020\u007fX\u0082\u0004¢\u0006\u0002\n\u0000R\u0010\u0010\u0080\u0001\u001a\u00030\u0081\u0001X\u0082\u000e¢\u0006\u0002\n\u0000R\u0015\u0010\u008c\u0001\u001a\u00030\u008d\u00018F¢\u0006\b\u001a\u0006\b\u008c\u0001\u0010\u008e\u0001R\u0015\u0010\u008f\u0001\u001a\u00030\u008d\u00018F¢\u0006\b\u001a\u0006\b\u008f\u0001\u0010\u008e\u0001¨\u0006¹\u0001"}, d2 = {"Lapp/aaps/pump/lenomed/service/LenomedService;", "Ldagger/android/DaggerService;", "<init>", "()V", "injector", "Ldagger/android/HasAndroidInjector;", "getInjector", "()Ldagger/android/HasAndroidInjector;", "setInjector", "(Ldagger/android/HasAndroidInjector;)V", "aapsLogger", "Lapp/aaps/core/interfaces/logging/AAPSLogger;", "getAapsLogger", "()Lapp/aaps/core/interfaces/logging/AAPSLogger;", "setAapsLogger", "(Lapp/aaps/core/interfaces/logging/AAPSLogger;)V", "rxBus", "Lapp/aaps/core/interfaces/rx/bus/RxBus;", "getRxBus", "()Lapp/aaps/core/interfaces/rx/bus/RxBus;", "setRxBus", "(Lapp/aaps/core/interfaces/rx/bus/RxBus;)V", "sp", "Lapp/aaps/core/interfaces/sharedPreferences/SP;", "getSp", "()Lapp/aaps/core/interfaces/sharedPreferences/SP;", "setSp", "(Lapp/aaps/core/interfaces/sharedPreferences/SP;)V", "rh", "Lapp/aaps/core/interfaces/resources/ResourceHelper;", "getRh", "()Lapp/aaps/core/interfaces/resources/ResourceHelper;", "setRh", "(Lapp/aaps/core/interfaces/resources/ResourceHelper;)V", "profileFunction", "Lapp/aaps/core/interfaces/profile/ProfileFunction;", "getProfileFunction", "()Lapp/aaps/core/interfaces/profile/ProfileFunction;", "setProfileFunction", "(Lapp/aaps/core/interfaces/profile/ProfileFunction;)V", "commandQueue", "Lapp/aaps/core/interfaces/queue/CommandQueue;", "getCommandQueue", "()Lapp/aaps/core/interfaces/queue/CommandQueue;", "setCommandQueue", "(Lapp/aaps/core/interfaces/queue/CommandQueue;)V", CoreConstants.CONTEXT_SCOPE_VALUE, "Landroid/content/Context;", "getContext", "()Landroid/content/Context;", "setContext", "(Landroid/content/Context;)V", "lenomedPlugin", "Lapp/aaps/pump/lenomed/LenomedPlugin;", "getLenomedPlugin", "()Lapp/aaps/pump/lenomed/LenomedPlugin;", "setLenomedPlugin", "(Lapp/aaps/pump/lenomed/LenomedPlugin;)V", "lenomedPump", "Lapp/aaps/pump/lenomed/LenomedPump;", "getLenomedPump", "()Lapp/aaps/pump/lenomed/LenomedPump;", "setLenomedPump", "(Lapp/aaps/pump/lenomed/LenomedPump;)V", "activePlugin", "Lapp/aaps/core/interfaces/plugin/ActivePlugin;", "getActivePlugin", "()Lapp/aaps/core/interfaces/plugin/ActivePlugin;", "setActivePlugin", "(Lapp/aaps/core/interfaces/plugin/ActivePlugin;)V", "bleCommonService", "Lapp/aaps/pump/lenomed/service/BLECommonService;", "getBleCommonService", "()Lapp/aaps/pump/lenomed/service/BLECommonService;", "setBleCommonService", "(Lapp/aaps/pump/lenomed/service/BLECommonService;)V", "fabricPrivacy", "Lapp/aaps/core/interfaces/utils/fabric/FabricPrivacy;", "getFabricPrivacy", "()Lapp/aaps/core/interfaces/utils/fabric/FabricPrivacy;", "setFabricPrivacy", "(Lapp/aaps/core/interfaces/utils/fabric/FabricPrivacy;)V", "pumpSync", "Lapp/aaps/core/interfaces/pump/PumpSync;", "getPumpSync", "()Lapp/aaps/core/interfaces/pump/PumpSync;", "setPumpSync", "(Lapp/aaps/core/interfaces/pump/PumpSync;)V", "dateUtil", "Lapp/aaps/core/interfaces/utils/DateUtil;", "getDateUtil", "()Lapp/aaps/core/interfaces/utils/DateUtil;", "setDateUtil", "(Lapp/aaps/core/interfaces/utils/DateUtil;)V", "aapsSchedulers", "Lapp/aaps/core/interfaces/rx/AapsSchedulers;", "getAapsSchedulers", "()Lapp/aaps/core/interfaces/rx/AapsSchedulers;", "setAapsSchedulers", "(Lapp/aaps/core/interfaces/rx/AapsSchedulers;)V", "lenomedLogUploader", "Lapp/aaps/pump/lenomed/api/LenomedLogUploader;", "getLenomedLogUploader", "()Lapp/aaps/pump/lenomed/api/LenomedLogUploader;", "setLenomedLogUploader", "(Lapp/aaps/pump/lenomed/api/LenomedLogUploader;)V", "lenomedHistoryRecordDao", "Lapp/aaps/pump/lenomed/database/LenomedHistoryRecordDao;", "getLenomedHistoryRecordDao", "()Lapp/aaps/pump/lenomed/database/LenomedHistoryRecordDao;", "setLenomedHistoryRecordDao", "(Lapp/aaps/pump/lenomed/database/LenomedHistoryRecordDao;)V", "uiInteraction", "Lapp/aaps/core/interfaces/ui/UiInteraction;", "getUiInteraction", "()Lapp/aaps/core/interfaces/ui/UiInteraction;", "setUiInteraction", "(Lapp/aaps/core/interfaces/ui/UiInteraction;)V", "instantiator", "Lapp/aaps/core/interfaces/objects/Instantiator;", "getInstantiator", "()Lapp/aaps/core/interfaces/objects/Instantiator;", "setInstantiator", "(Lapp/aaps/core/interfaces/objects/Instantiator;)V", "disposable", "Lio/reactivex/rxjava3/disposables/CompositeDisposable;", "mBinder", "Landroid/os/IBinder;", "lastApproachingDailyLimit", "", "onCreate", "", "onBind", "intent", "Landroid/content/Intent;", "onStartCommand", "", "flags", "startId", "onDestroy", "isConnected", "", "()Z", "isConnecting", Socket.EVENT_CONNECT, TypedValues.TransitionType.S_FROM, "", "address", "stopConnecting", Socket.EVENT_DISCONNECT, "sendMessage", "message", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "waitMillis", "setBleMtu", "readPumpStatus", "loadHistory", "Lapp/aaps/core/interfaces/pump/PumpEnactResult;", "type", "", "forceLatest", "loadBolusHistory", "setUserSettings", "bolus", "insulin", "", TableNamesKt.TABLE_CARBS, "carbTime", "t", "Lapp/aaps/core/interfaces/rx/events/EventOverviewBolusProgress$Treatment;", "bolusStop", "tempBasal", "absoluteRate", "durationInHours", "tempBasalShortDuration", "durationInMinutes", "tempBasalStop", "extendedBolus", "extendedBolusStop", "updateBasalsInPump", "profile", "Lapp/aaps/core/interfaces/profile/Profile;", "processConfirm", "msgType", "LocalBinder", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LenomedService extends DaggerService {

    @Inject
    public AAPSLogger aapsLogger;

    @Inject
    public AapsSchedulers aapsSchedulers;

    @Inject
    public ActivePlugin activePlugin;

    @Inject
    public BLECommonService bleCommonService;

    @Inject
    public CommandQueue commandQueue;

    @Inject
    public Context context;

    @Inject
    public DateUtil dateUtil;

    @Inject
    public FabricPrivacy fabricPrivacy;

    @Inject
    public HasAndroidInjector injector;

    @Inject
    public Instantiator instantiator;
    private long lastApproachingDailyLimit;

    @Inject
    public LenomedHistoryRecordDao lenomedHistoryRecordDao;

    @Inject
    public LenomedLogUploader lenomedLogUploader;

    @Inject
    public LenomedPlugin lenomedPlugin;

    @Inject
    public LenomedPump lenomedPump;

    @Inject
    public ProfileFunction profileFunction;

    @Inject
    public PumpSync pumpSync;

    @Inject
    public ResourceHelper rh;

    @Inject
    public RxBus rxBus;

    @Inject
    public SP sp;

    @Inject
    public UiInteraction uiInteraction;
    private final CompositeDisposable disposable = new CompositeDisposable();
    private final IBinder mBinder = new LocalBinder();

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intrinsics.checkNotNullParameter(intent, "intent");
        return 1;
    }

    public final HasAndroidInjector getInjector() {
        HasAndroidInjector hasAndroidInjector = this.injector;
        if (hasAndroidInjector != null) {
            return hasAndroidInjector;
        }
        Intrinsics.throwUninitializedPropertyAccessException("injector");
        return null;
    }

    public final void setInjector(HasAndroidInjector hasAndroidInjector) {
        Intrinsics.checkNotNullParameter(hasAndroidInjector, "<set-?>");
        this.injector = hasAndroidInjector;
    }

    public final AAPSLogger getAapsLogger() {
        AAPSLogger aAPSLogger = this.aapsLogger;
        if (aAPSLogger != null) {
            return aAPSLogger;
        }
        Intrinsics.throwUninitializedPropertyAccessException("aapsLogger");
        return null;
    }

    public final void setAapsLogger(AAPSLogger aAPSLogger) {
        Intrinsics.checkNotNullParameter(aAPSLogger, "<set-?>");
        this.aapsLogger = aAPSLogger;
    }

    public final RxBus getRxBus() {
        RxBus rxBus = this.rxBus;
        if (rxBus != null) {
            return rxBus;
        }
        Intrinsics.throwUninitializedPropertyAccessException("rxBus");
        return null;
    }

    public final void setRxBus(RxBus rxBus) {
        Intrinsics.checkNotNullParameter(rxBus, "<set-?>");
        this.rxBus = rxBus;
    }

    public final SP getSp() {
        SP sp = this.sp;
        if (sp != null) {
            return sp;
        }
        Intrinsics.throwUninitializedPropertyAccessException("sp");
        return null;
    }

    public final void setSp(SP sp) {
        Intrinsics.checkNotNullParameter(sp, "<set-?>");
        this.sp = sp;
    }

    public final ResourceHelper getRh() {
        ResourceHelper resourceHelper = this.rh;
        if (resourceHelper != null) {
            return resourceHelper;
        }
        Intrinsics.throwUninitializedPropertyAccessException("rh");
        return null;
    }

    public final void setRh(ResourceHelper resourceHelper) {
        Intrinsics.checkNotNullParameter(resourceHelper, "<set-?>");
        this.rh = resourceHelper;
    }

    public final ProfileFunction getProfileFunction() {
        ProfileFunction profileFunction = this.profileFunction;
        if (profileFunction != null) {
            return profileFunction;
        }
        Intrinsics.throwUninitializedPropertyAccessException("profileFunction");
        return null;
    }

    public final void setProfileFunction(ProfileFunction profileFunction) {
        Intrinsics.checkNotNullParameter(profileFunction, "<set-?>");
        this.profileFunction = profileFunction;
    }

    public final CommandQueue getCommandQueue() {
        CommandQueue commandQueue = this.commandQueue;
        if (commandQueue != null) {
            return commandQueue;
        }
        Intrinsics.throwUninitializedPropertyAccessException("commandQueue");
        return null;
    }

    public final void setCommandQueue(CommandQueue commandQueue) {
        Intrinsics.checkNotNullParameter(commandQueue, "<set-?>");
        this.commandQueue = commandQueue;
    }

    public final Context getContext() {
        Context context = this.context;
        if (context != null) {
            return context;
        }
        Intrinsics.throwUninitializedPropertyAccessException(CoreConstants.CONTEXT_SCOPE_VALUE);
        return null;
    }

    public final void setContext(Context context) {
        Intrinsics.checkNotNullParameter(context, "<set-?>");
        this.context = context;
    }

    public final LenomedPlugin getLenomedPlugin() {
        LenomedPlugin lenomedPlugin = this.lenomedPlugin;
        if (lenomedPlugin != null) {
            return lenomedPlugin;
        }
        Intrinsics.throwUninitializedPropertyAccessException("lenomedPlugin");
        return null;
    }

    public final void setLenomedPlugin(LenomedPlugin lenomedPlugin) {
        Intrinsics.checkNotNullParameter(lenomedPlugin, "<set-?>");
        this.lenomedPlugin = lenomedPlugin;
    }

    public final LenomedPump getLenomedPump() {
        LenomedPump lenomedPump = this.lenomedPump;
        if (lenomedPump != null) {
            return lenomedPump;
        }
        Intrinsics.throwUninitializedPropertyAccessException("lenomedPump");
        return null;
    }

    public final void setLenomedPump(LenomedPump lenomedPump) {
        Intrinsics.checkNotNullParameter(lenomedPump, "<set-?>");
        this.lenomedPump = lenomedPump;
    }

    public final ActivePlugin getActivePlugin() {
        ActivePlugin activePlugin = this.activePlugin;
        if (activePlugin != null) {
            return activePlugin;
        }
        Intrinsics.throwUninitializedPropertyAccessException("activePlugin");
        return null;
    }

    public final void setActivePlugin(ActivePlugin activePlugin) {
        Intrinsics.checkNotNullParameter(activePlugin, "<set-?>");
        this.activePlugin = activePlugin;
    }

    public final BLECommonService getBleCommonService() {
        BLECommonService bLECommonService = this.bleCommonService;
        if (bLECommonService != null) {
            return bLECommonService;
        }
        Intrinsics.throwUninitializedPropertyAccessException("bleCommonService");
        return null;
    }

    public final void setBleCommonService(BLECommonService bLECommonService) {
        Intrinsics.checkNotNullParameter(bLECommonService, "<set-?>");
        this.bleCommonService = bLECommonService;
    }

    public final FabricPrivacy getFabricPrivacy() {
        FabricPrivacy fabricPrivacy = this.fabricPrivacy;
        if (fabricPrivacy != null) {
            return fabricPrivacy;
        }
        Intrinsics.throwUninitializedPropertyAccessException("fabricPrivacy");
        return null;
    }

    public final void setFabricPrivacy(FabricPrivacy fabricPrivacy) {
        Intrinsics.checkNotNullParameter(fabricPrivacy, "<set-?>");
        this.fabricPrivacy = fabricPrivacy;
    }

    public final PumpSync getPumpSync() {
        PumpSync pumpSync = this.pumpSync;
        if (pumpSync != null) {
            return pumpSync;
        }
        Intrinsics.throwUninitializedPropertyAccessException("pumpSync");
        return null;
    }

    public final void setPumpSync(PumpSync pumpSync) {
        Intrinsics.checkNotNullParameter(pumpSync, "<set-?>");
        this.pumpSync = pumpSync;
    }

    public final DateUtil getDateUtil() {
        DateUtil dateUtil = this.dateUtil;
        if (dateUtil != null) {
            return dateUtil;
        }
        Intrinsics.throwUninitializedPropertyAccessException("dateUtil");
        return null;
    }

    public final void setDateUtil(DateUtil dateUtil) {
        Intrinsics.checkNotNullParameter(dateUtil, "<set-?>");
        this.dateUtil = dateUtil;
    }

    public final AapsSchedulers getAapsSchedulers() {
        AapsSchedulers aapsSchedulers = this.aapsSchedulers;
        if (aapsSchedulers != null) {
            return aapsSchedulers;
        }
        Intrinsics.throwUninitializedPropertyAccessException("aapsSchedulers");
        return null;
    }

    public final void setAapsSchedulers(AapsSchedulers aapsSchedulers) {
        Intrinsics.checkNotNullParameter(aapsSchedulers, "<set-?>");
        this.aapsSchedulers = aapsSchedulers;
    }

    public final LenomedLogUploader getLenomedLogUploader() {
        LenomedLogUploader lenomedLogUploader = this.lenomedLogUploader;
        if (lenomedLogUploader != null) {
            return lenomedLogUploader;
        }
        Intrinsics.throwUninitializedPropertyAccessException("lenomedLogUploader");
        return null;
    }

    public final void setLenomedLogUploader(LenomedLogUploader lenomedLogUploader) {
        Intrinsics.checkNotNullParameter(lenomedLogUploader, "<set-?>");
        this.lenomedLogUploader = lenomedLogUploader;
    }

    public final LenomedHistoryRecordDao getLenomedHistoryRecordDao() {
        LenomedHistoryRecordDao lenomedHistoryRecordDao = this.lenomedHistoryRecordDao;
        if (lenomedHistoryRecordDao != null) {
            return lenomedHistoryRecordDao;
        }
        Intrinsics.throwUninitializedPropertyAccessException("lenomedHistoryRecordDao");
        return null;
    }

    public final void setLenomedHistoryRecordDao(LenomedHistoryRecordDao lenomedHistoryRecordDao) {
        Intrinsics.checkNotNullParameter(lenomedHistoryRecordDao, "<set-?>");
        this.lenomedHistoryRecordDao = lenomedHistoryRecordDao;
    }

    public final UiInteraction getUiInteraction() {
        UiInteraction uiInteraction = this.uiInteraction;
        if (uiInteraction != null) {
            return uiInteraction;
        }
        Intrinsics.throwUninitializedPropertyAccessException("uiInteraction");
        return null;
    }

    public final void setUiInteraction(UiInteraction uiInteraction) {
        Intrinsics.checkNotNullParameter(uiInteraction, "<set-?>");
        this.uiInteraction = uiInteraction;
    }

    public final Instantiator getInstantiator() {
        Instantiator instantiator = this.instantiator;
        if (instantiator != null) {
            return instantiator;
        }
        Intrinsics.throwUninitializedPropertyAccessException("instantiator");
        return null;
    }

    public final void setInstantiator(Instantiator instantiator) {
        Intrinsics.checkNotNullParameter(instantiator, "<set-?>");
        this.instantiator = instantiator;
    }

    @Override // dagger.android.DaggerService, android.app.Service
    public void onCreate() {
        super.onCreate();
        this.disposable.add(getRxBus().toObservable(EventAppExit.class).observeOn(getAapsSchedulers().getIo()).subscribe(new Consumer() { // from class: app.aaps.pump.lenomed.service.LenomedService.onCreate.1
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(EventAppExit it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedService.this.stopSelf();
            }
        }, new Consumer() { // from class: app.aaps.pump.lenomed.service.LenomedService.onCreate.2
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(Throwable it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedService.this.getFabricPrivacy().logException(it);
            }
        }));
    }

    /* JADX INFO: compiled from: LenomedService.kt */
    @Metadata(d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0004\u0018\u00002\u00020\u0001B\u0007¢\u0006\u0004\b\u0002\u0010\u0003R\u0011\u0010\u0004\u001a\u00020\u00058F¢\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007¨\u0006\b"}, d2 = {"Lapp/aaps/pump/lenomed/service/LenomedService$LocalBinder;", "Landroid/os/Binder;", "<init>", "(Lapp/aaps/pump/lenomed/service/LenomedService;)V", "serviceInstance", "Lapp/aaps/pump/lenomed/service/LenomedService;", "getServiceInstance", "()Lapp/aaps/pump/lenomed/service/LenomedService;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
    public final class LocalBinder extends Binder {
        public LocalBinder() {
        }

        /* JADX INFO: renamed from: getServiceInstance, reason: from getter */
        public final LenomedService getThis$0() {
            return LenomedService.this;
        }
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        Intrinsics.checkNotNullParameter(intent, "intent");
        return this.mBinder;
    }

    @Override // android.app.Service
    public void onDestroy() {
        this.disposable.clear();
        super.onDestroy();
    }

    public final boolean isConnected() {
        return getBleCommonService().getIsConnected();
    }

    public final boolean isConnecting() {
        return getBleCommonService().getIsConnecting();
    }

    public final boolean connect(String from, String address) {
        Intrinsics.checkNotNullParameter(from, "from");
        Intrinsics.checkNotNullParameter(address, "address");
        return getBleCommonService().connect(from, address);
    }

    public final void stopConnecting() {
        getBleCommonService().stopConnecting();
    }

    public final void disconnect(String from) {
        Intrinsics.checkNotNullParameter(from, "from");
        getBleCommonService().disconnect(from);
    }

    public final void sendMessage(LenomedPacket message, long waitMillis) {
        Intrinsics.checkNotNullParameter(message, "message");
        getBleCommonService().sendMessage(message, waitMillis);
    }

    private final void sendMessage(LenomedPacket message) {
        getBleCommonService().sendMessage(message, 5000L);
    }

    private final void setBleMtu() {
        getAapsLogger().debug(LTag.PUMPCOMM, "setting ble Mtu");
        getBleCommonService().setRequestMtu(150);
    }

    public final void readPumpStatus() {
        long pumpTime;
        getAapsLogger().debug(LTag.PUMPCOMM, "Entering readPumpStatus()");
        try {
            Pump activePump = getActivePlugin().getActivePump();
            getRxBus().send(new EventPumpStatusChanged(getRh().gs(R.string.gettingpumpsettings)));
            getAapsLogger().debug(LTag.PUMPCOMM, "pumpFirmwareVersion=" + getSp().getString(getRh().gs(R.string.pumpversion), ""));
            sendMessage(new BigAPSMainInfoInquirePacket(getInjector()), 1000L);
            getRxBus().send(new EventPumpStatusChanged(getRh().gs(R.string.gettingpumptime)));
            SystemClock.sleep(2000L);
            getRxBus().send(new EventPumpStatusChanged(getRh().gs(R.string.gettingpumpsettings)));
            getLenomedPump().setLastConnection(System.currentTimeMillis());
            Profile profile = getProfileFunction().getProfile();
            if (profile != null && Math.abs(getLenomedPump().getBaseAmount() - profile.getBasal()) >= activePump.getPumpDescription().getBasalStep()) {
                getRxBus().send(new EventPumpStatusChanged(getRh().gs(R.string.gettingpumpsettings)));
                if (!activePump.isThisProfileSet(profile) && !getCommandQueue().isRunning(Command.CommandType.BASAL_PROFILE)) {
                    getRxBus().send(new EventProfileSwitchChanged());
                }
            }
            getAapsLogger().debug(LTag.PUMPCOMM, "Pump time: " + getLenomedPump().getPumpTime());
            pumpTime = (getLenomedPump().getPumpTime() - System.currentTimeMillis()) / 1000;
        } catch (Exception e) {
            getAapsLogger().error(LTag.PUMPCOMM, "Unhandled exception", e);
        }
        if (getLenomedPump().getPumpTime() == 0) {
            getLenomedPump().reset();
            getRxBus().send(new EventEmbectaNewStatus());
            getRxBus().send(new EventInitializationChanged());
            return;
        }
        getAapsLogger().debug(LTag.PUMPCOMM, "Pump time difference: " + pumpTime + " seconds");
        int hours = (int) TimeUnit.MILLISECONDS.toHours(DateTimeZone.getDefault().getOffset(DateTime.now().getMillis()));
        if (pumpTime > 0 || Math.abs(pumpTime) > 90) {
            if (Math.abs(pumpTime) > 5400.0d) {
                getAapsLogger().debug(LTag.PUMPCOMM, "Pump time difference: " + pumpTime + " seconds - large difference");
                getUiInteraction().runAlarm(getRh().gs(R.string.largetimediff), getRh().gs(R.string.largetimedifftitle), app.aaps.core.ui.R.raw.error);
                getLenomedPump().reset();
                getRxBus().send(new EventEmbectaNewStatus());
                getRxBus().send(new EventInitializationChanged());
                return;
            }
            if (!getLenomedPump().isTempBasalInProgress()) {
                TimeSettingPacket timeSettingPacket = new TimeSettingPacket(getInjector(), getDateUtil().now(), hours);
                sendMessage(timeSettingPacket, 500L);
                if (!processConfirm(timeSettingPacket.getMsgType())) {
                    return;
                } else {
                    getAapsLogger().debug(LTag.PUMPCOMM, "Pump time difference: " + ((getLenomedPump().getPumpTime() - System.currentTimeMillis()) / 1000) + " seconds");
                }
            }
        }
        SystemClock.sleep(1000L);
        getLenomedPump().fromExtendedBolus(getPumpSync().expectedPumpState().getExtendedBolus());
        SystemClock.sleep(1000L);
        PumpSync.PumpState.TemporaryBasal temporaryBasal = getPumpSync().expectedPumpState().getTemporaryBasal();
        getAapsLogger().debug(LTag.PUMPCOMM, "TBR:  timestamp=" + (temporaryBasal != null ? Long.valueOf(temporaryBasal.getTimestamp()) : null) + ", duration=" + (temporaryBasal != null ? Long.valueOf(temporaryBasal.getDuration()) : null) + ", rate=" + (temporaryBasal != null ? Double.valueOf(temporaryBasal.getRate()) : null));
        getLenomedPump().fromTemporaryBasal(temporaryBasal);
        getRxBus().send(new EventEmbectaNewStatus());
        getRxBus().send(new EventInitializationChanged());
        if (getLenomedPump().getDailyTotalUnits() > ((double) getLenomedPump().getMaxDailyTotalUnits()) * 0.95d) {
            getAapsLogger().debug(LTag.PUMPCOMM, "Approaching daily limit: " + getLenomedPump().getDailyTotalUnits() + "/" + getLenomedPump().getMaxDailyTotalUnits());
            if (System.currentTimeMillis() > this.lastApproachingDailyLimit + ((long) 1800000)) {
                getUiInteraction().addNotification(12, getRh().gs(R.string.approachingdailylimit), 0);
                getPumpSync().insertAnnouncement(getRh().gs(R.string.approachingdailylimit) + ": " + getLenomedPump().getDailyTotalUnits() + "/" + getLenomedPump().getMaxDailyTotalUnits() + EnterpriseWifi.USER, null, PumpType.EMBECTA, getLenomedPump().getSerialNo());
                this.lastApproachingDailyLimit = System.currentTimeMillis();
            }
        }
        getAapsLogger().debug(LTag.PUMPCOMM, "Pump status loaded");
    }

    public static /* synthetic */ PumpEnactResult loadHistory$default(LenomedService lenomedService, byte b, boolean z, int i, Object obj) {
        if ((i & 1) != 0) {
            b = 0;
        }
        if ((i & 2) != 0) {
            z = false;
        }
        return lenomedService.loadHistory(b, z);
    }

    public final PumpEnactResult loadHistory(byte type, boolean forceLatest) {
        List listListOf;
        if (!getLenomedPlugin().isInitialized()) {
            PumpEnactResult pumpEnactResultSuccess = getInstantiator().providePumpEnactResult().success(false);
            pumpEnactResultSuccess.setComment("pump not initialized");
            return pumpEnactResultSuccess;
        }
        setBleMtu();
        PumpEnactResult pumpEnactResultProvidePumpEnactResult = getInstantiator().providePumpEnactResult();
        int i = 2;
        if (type == 0) {
            listListOf = CollectionsKt.listOf((Object[]) new Byte[]{(byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8});
        } else {
            listListOf = CollectionsKt.listOf(Byte.valueOf(type));
        }
        getAapsLogger().debug(LTag.PUMPCOMM, "Starting log sync: type=" + ((int) type) + ", forceLatest=" + forceLatest + ", pumpLogPageSize=4");
        Iterator it = listListOf.iterator();
        while (it.hasNext()) {
            byte bByteValue = ((Number) it.next()).byteValue();
            getAapsLogger().debug(LTag.PUMPCOMM, "Processing logType: " + ((int) bByteValue) + ", startPageIndex=1, forceLatest=" + forceLatest);
            sendMessage(new BigLogInquirePacket(getInjector(), bByteValue, 1), 500L);
            SystemClock.sleep(2000L);
            if (forceLatest) {
                getAapsLogger().debug(LTag.PUMPCOMM, "forceLatest=true, only requesting page 1 for logType " + ((int) bByteValue));
            } else {
                int totalPages = getLenomedPump().getTotalPages();
                getAapsLogger().debug(LTag.PUMPCOMM, "Total pages for logType " + ((int) bByteValue) + ": " + totalPages);
                if (i <= totalPages) {
                    int i2 = i;
                    while (true) {
                        getAapsLogger().debug(LTag.PUMPCOMM, "pumplog request: logType=" + ((int) bByteValue) + ", pageIndex=" + i2);
                        sendMessage(new BigLogInquirePacket(getInjector(), bByteValue, i2), 500L);
                        SystemClock.sleep(2000L);
                        if (i2 == totalPages) {
                            break;
                        }
                        i2++;
                    }
                    i = 2;
                }
            }
        }
        pumpEnactResultProvidePumpEnactResult.success(true);
        getLenomedPump().setLastConnection(System.currentTimeMillis());
        return pumpEnactResultProvidePumpEnactResult;
    }

    public final PumpEnactResult loadBolusHistory() {
        getAapsLogger().debug(LTag.PUMPCOMM, "Loading bolus history with type=4, forceLatest=true");
        return loadHistory((byte) 4, true);
    }

    public final PumpEnactResult setUserSettings() {
        LenomedInfusionSwitchPacket lenomedInfusionSwitchPacket;
        PumpEnactResult pumpEnactResultProvidePumpEnactResult = getInstantiator().providePumpEnactResult();
        getAapsLogger().debug(LTag.PUMPCOMM, "setUserSettings: Processing user setting type=" + getLenomedPump().getSetUserOptionType());
        switch (getLenomedPump().getSetUserOptionType()) {
            case 9:
                lenomedInfusionSwitchPacket = new LenomedInfusionSwitchPacket(getInjector(), getLenomedPump().getInfusionEnabled() ? 1 : 0);
                break;
            case 10:
                lenomedInfusionSwitchPacket = new BluetoothSwitchPacket(getInjector(), getLenomedPump().getBluetoothEnabled() ? 1 : 0);
                break;
            case 11:
                getAapsLogger().error(LTag.PUMPCOMM, "threshold values: maxBolus=" + getLenomedPump().getMaxBolus() + ", maxBolusePerDay=" + getLenomedPump().getMaxBolusePerDay() + ", maxBasal=" + getLenomedPump().getMaxBasal() + ", basalDailyTotalThreshold=" + getLenomedPump().getBasalDailyTotalThreshold() + ", fixedExhaustThreshold=" + getLenomedPump().getFixedExhaustThreshold());
                lenomedInfusionSwitchPacket = new ThresholdSettingPacket(getInjector(), (int) getLenomedPump().getMaxBolus(), (int) getLenomedPump().getMaxBolusePerDay(), (int) getLenomedPump().getMaxBasal(), (int) getLenomedPump().getBasalDailyTotalThreshold(), (int) getLenomedPump().getFixedExhaustThreshold());
                break;
            default:
                return pumpEnactResultProvidePumpEnactResult.success(false);
        }
        sendMessage(lenomedInfusionSwitchPacket, 500L);
        SystemClock.sleep(1000L);
        if (getLenomedPump().getOtpNumber() == 0) {
            getAapsLogger().error(LTag.PUMPCOMM, "otp is not received yet");
            pumpEnactResultProvidePumpEnactResult.success(false);
            pumpEnactResultProvidePumpEnactResult.comment("");
            return pumpEnactResultProvidePumpEnactResult;
        }
        getLenomedPump().setOtpNumber(0);
        SystemClock.sleep(1000L);
        return pumpEnactResultProvidePumpEnactResult.success(true);
    }

    public final boolean bolus(double insulin, int carbs, long carbTime, EventOverviewBolusProgress.Treatment t) {
        Intrinsics.checkNotNullParameter(t, "t");
        getAapsLogger().debug(LTag.PUMPBTCOMM, "bolus: Starting bolus delivery. Insulin: " + insulin + " U");
        if (!isConnected() || insulin <= 0.0d || insulin > 250.0d) {
            getAapsLogger().error(LTag.PUMPBTCOMM, "bolus: Invalid state or amount");
            return false;
        }
        if (carbs > 0) {
            getPumpSync().syncCarbsWithTimestamp(carbTime, carbs, null, PumpType.EMBECTA, getLenomedPump().getSerialNo());
        }
        getRxBus().send(new EventPumpStatusChanged(getRh().gs(R.string.startingbolus)));
        getLenomedPump().setBolusDone(false);
        getLenomedPump().setBolusingTreatment(t);
        getLenomedPump().setBolusAmountToBeDelivered(insulin);
        getLenomedPump().setBolusStopped(false);
        getLenomedPump().setBolusingInjAmount(0.0d);
        EventOverviewBolusProgress eventOverviewBolusProgress = new EventOverviewBolusProgress();
        eventOverviewBolusProgress.setT(t);
        sendMessage(new InjectionSnackSettingPacket(getInjector(), (int) (((double) 10) * insulin)), 10000L);
        long jCurrentTimeMillis = System.currentTimeMillis();
        while (!getLenomedPump().getBolusDone() && !getLenomedPump().getBolusStopped() && System.currentTimeMillis() - jCurrentTimeMillis <= 120000) {
            eventOverviewBolusProgress.setPercent(-1);
            eventOverviewBolusProgress.setStatus(getRh().gs(R.string.bolus_delivered, Double.valueOf(insulin)));
            getRxBus().send(eventOverviewBolusProgress);
            SystemClock.sleep(300L);
        }
        if (getLenomedPump().getBolusDone() && !getLenomedPump().getBolusStopped()) {
            eventOverviewBolusProgress.setPercent(100);
            eventOverviewBolusProgress.setStatus(getRh().gs(R.string.bolus_delivered, Double.valueOf(insulin)));
            getRxBus().send(eventOverviewBolusProgress);
        } else {
            eventOverviewBolusProgress.setPercent(0);
            eventOverviewBolusProgress.setStatus(getRh().gs(R.string.bolus_failed));
            getRxBus().send(eventOverviewBolusProgress);
        }
        getRxBus().send(new EventPumpStatusChanged(getRh().gs(app.aaps.core.interfaces.R.string.disconnecting)));
        return getLenomedPump().getBolusDone() && !getLenomedPump().getBolusStopped();
    }

    public final void bolusStop() {
        getAapsLogger().debug(LTag.PUMPBTCOMM, "bolusStop: User initiated bolus stop");
        InjectionCancelSettingPacket injectionCancelSettingPacket = new InjectionCancelSettingPacket(getInjector());
        getLenomedPump().setBolusStopForced(true);
        getLenomedPump().setOtpNumber(0);
        BolusProgressData.INSTANCE.setStopPressed(true);
        if (isConnected()) {
            sendMessage(injectionCancelSettingPacket, 1000L);
            if (processConfirm(injectionCancelSettingPacket.getMsgType())) {
                getLenomedPump().setBolusStopped(true);
                getLenomedPump().setBolusDone(true);
                getAapsLogger().debug(LTag.PUMPBTCOMM, "bolusStop: Cancel command successful");
                return;
            } else {
                getAapsLogger().error(LTag.PUMPBTCOMM, "bolusStop: Cancel command failed");
                getLenomedPump().setBolusStopped(true);
                getLenomedPump().setBolusDone(true);
                return;
            }
        }
        getLenomedPump().setBolusStopped(true);
        getLenomedPump().setBolusDone(true);
        getAapsLogger().error(LTag.PUMPBTCOMM, "bolusStop: Pump not connected, marked as stopped");
    }

    public final boolean tempBasal(double absoluteRate, double durationInHours) {
        Double[] dArr;
        if (!isConnected()) {
            getAapsLogger().error(LTag.PUMPCOMM, "Pump not connected");
            return false;
        }
        getLenomedPump().setOtpNumber(0);
        TempBasalInquirePacket tempBasalInquirePacket = new TempBasalInquirePacket(getInjector());
        sendMessage(tempBasalInquirePacket, 1000L);
        if (!processConfirm(tempBasalInquirePacket.getMsgType())) {
            getAapsLogger().error(LTag.PUMPBTCOMM, "Failed to confirm TBR setting");
            return false;
        }
        if (!tempBasalInquirePacket.success()) {
            getAapsLogger().error(LTag.PUMPCOMM, "TBR inquire failed");
            return false;
        }
        if (getLenomedPump().getTbTime() > 0) {
            TempBasalSettingPacket tempBasalSettingPacket = new TempBasalSettingPacket(getInjector(), (byte) 1, 0, 0, 0);
            getLenomedPump().setOtpNumber(0);
            sendMessage(tempBasalSettingPacket, 1000L);
            if (!processConfirm(tempBasalSettingPacket.getMsgType())) {
                getAapsLogger().error(LTag.PUMPBTCOMM, "Failed to confirm TBR stop");
                return false;
            }
            SystemClock.sleep(1000L);
            getLenomedPump().setTempBasalInProgress(false);
        }
        long jNow = getDateUtil().now();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(jNow);
        int i = (calendar.get(11) * 60) + calendar.get(12);
        double d = 60;
        double d2 = durationInHours * d;
        int i2 = i + ((int) d2);
        int i3 = calendar.get(11);
        Double[][] pumpProfiles = getLenomedPump().getPumpProfiles();
        double dDoubleValue = (pumpProfiles == null || (dArr = pumpProfiles[getLenomedPump().getActiveProfile()]) == null) ? 1.0d : dArr[i3].doubleValue();
        double d3 = dDoubleValue > 0.0d ? (absoluteRate / dDoubleValue) * ((double) 100) : 0.0d;
        int iCoerceIn = RangesKt.coerceIn(MathKt.roundToInt(d3), 0, 300);
        int i4 = iCoerceIn * 100;
        double d4 = (((double) iCoerceIn) / 100.0d) * dDoubleValue;
        getAapsLogger().debug(LTag.PUMPCOMM, "Converting TBR: currentHour=" + i3 + ", currentBasalRate=" + dDoubleValue + " U/h, absoluteRate=" + absoluteRate + " U/h, rawPercent=" + d3 + "%, rounded percent=" + iCoerceIn + "%, value=" + i4 + ", actualAbsoluteRate=" + d4 + " U/h");
        getRxBus().send(new EventPumpStatusChanged(getRh().gs(R.string.settingtempbasal)));
        getLenomedPump().setOtpNumber(0);
        TempBasalSettingPacket tempBasalSettingPacket2 = new TempBasalSettingPacket(getInjector(), (byte) 1, i, i2, i4);
        sendMessage(tempBasalSettingPacket2, 1000L);
        if (!processConfirm(tempBasalSettingPacket2.getMsgType())) {
            getAapsLogger().error(LTag.PUMPBTCOMM, "Failed to confirm TBR setting");
            return false;
        }
        if (!tempBasalSettingPacket2.success()) {
            getAapsLogger().error(LTag.PUMPCOMM, "TBR setting failed");
            return false;
        }
        getLenomedPump().setTempBasalStart(jNow);
        getLenomedPump().setTempBasalDuration((long) (d2 * d * ((double) 1000)));
        getLenomedPump().setTempBasalPercent(iCoerceIn);
        getLenomedPump().setTempBasalAbsoluteRate(d4);
        getLenomedPump().setTempBasalInProgress(true);
        loadHistory((byte) 8, true);
        SystemClock.sleep(5000L);
        PumpSync.PumpState.TemporaryBasal temporaryBasal = getPumpSync().expectedPumpState().getTemporaryBasal();
        getAapsLogger().debug(LTag.PUMPCOMM, "TBR set: percent=" + iCoerceIn + "%, actualAbsoluteRate=" + d4 + " U/h, timestamp=" + (temporaryBasal != null ? Long.valueOf(temporaryBasal.getTimestamp()) : null) + ", duration=" + (temporaryBasal != null ? Long.valueOf(temporaryBasal.getDuration()) : null) + ", rate=" + (temporaryBasal != null ? Double.valueOf(temporaryBasal.getRate()) : null));
        getLenomedPump().fromTemporaryBasal(temporaryBasal);
        getRxBus().send(new EventPumpStatusChanged(EventPumpStatusChanged.Status.DISCONNECTING));
        return true;
    }

    public final boolean tempBasalShortDuration(double absoluteRate, int durationInMinutes) {
        Double[] dArr;
        if (durationInMinutes != 15 && durationInMinutes != 30) {
            getAapsLogger().error(LTag.PUMPCOMM, "Wrong duration param: " + durationInMinutes);
            return false;
        }
        if (!isConnected()) {
            getAapsLogger().error(LTag.PUMPCOMM, "Pump not connected");
            return false;
        }
        getLenomedPump().setOtpNumber(0);
        TempBasalInquirePacket tempBasalInquirePacket = new TempBasalInquirePacket(getInjector());
        sendMessage(tempBasalInquirePacket, 1000L);
        if (!processConfirm(tempBasalInquirePacket.getMsgType())) {
            getAapsLogger().error(LTag.PUMPBTCOMM, "Failed to confirm TBR setting");
            return false;
        }
        if (!tempBasalInquirePacket.success()) {
            getAapsLogger().error(LTag.PUMPCOMM, "TBR inquire failed");
            return false;
        }
        if (getLenomedPump().getTbTime() > 0) {
            TempBasalSettingPacket tempBasalSettingPacket = new TempBasalSettingPacket(getInjector(), (byte) 1, 0, 0, 0);
            getLenomedPump().setOtpNumber(0);
            sendMessage(tempBasalSettingPacket, 1000L);
            if (!processConfirm(tempBasalSettingPacket.getMsgType())) {
                getAapsLogger().error(LTag.PUMPBTCOMM, "Failed to confirm TBR stop");
                return false;
            }
            SystemClock.sleep(1000L);
            getLenomedPump().setTempBasalInProgress(false);
        }
        long jNow = getDateUtil().now();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(jNow);
        int i = (calendar.get(11) * 60) + calendar.get(12);
        int i2 = i + durationInMinutes;
        int i3 = calendar.get(11);
        Double[][] pumpProfiles = getLenomedPump().getPumpProfiles();
        double dDoubleValue = (pumpProfiles == null || (dArr = pumpProfiles[getLenomedPump().getActiveProfile()]) == null) ? 1.0d : dArr[i3].doubleValue();
        double d = dDoubleValue > 0.0d ? (absoluteRate / dDoubleValue) * ((double) 100) : 0.0d;
        int iCoerceIn = RangesKt.coerceIn(MathKt.roundToInt(d), 0, 300);
        int i4 = iCoerceIn * 100;
        double d2 = (((double) iCoerceIn) / 100.0d) * dDoubleValue;
        getAapsLogger().debug(LTag.PUMPCOMM, "Converting TBR: currentHour=" + i3 + ", currentBasalRate=" + dDoubleValue + " U/h, absoluteRate=" + absoluteRate + " U/h, rawPercent=" + d + "%, rounded percent=" + iCoerceIn + "%, value=" + i4 + ", actualAbsoluteRate=" + d2 + " U/h");
        getRxBus().send(new EventPumpStatusChanged(getRh().gs(R.string.settingtempbasal)));
        getLenomedPump().setOtpNumber(0);
        TempBasalSettingPacket tempBasalSettingPacket2 = new TempBasalSettingPacket(getInjector(), (byte) 1, i, i2, i4);
        sendMessage(tempBasalSettingPacket2, 1000L);
        if (!processConfirm(tempBasalSettingPacket2.getMsgType())) {
            getAapsLogger().error(LTag.PUMPBTCOMM, "Failed to confirm TBR setting");
            return false;
        }
        if (!tempBasalSettingPacket2.success()) {
            getAapsLogger().error(LTag.PUMPCOMM, "TBR setting failed");
            return false;
        }
        getLenomedPump().setTempBasalStart(jNow);
        getLenomedPump().setTempBasalDuration(((long) (durationInMinutes * 60)) * 1000);
        getLenomedPump().setTempBasalPercent(iCoerceIn);
        getLenomedPump().setTempBasalAbsoluteRate(d2);
        getLenomedPump().setTempBasalInProgress(true);
        getLenomedPump().setTbTime(60000 * durationInMinutes);
        loadHistory((byte) 8, true);
        SystemClock.sleep(5000L);
        PumpSync.PumpState.TemporaryBasal temporaryBasal = getPumpSync().expectedPumpState().getTemporaryBasal();
        getAapsLogger().debug(LTag.PUMPCOMM, "TBR set: percent=" + iCoerceIn + "%, actualAbsoluteRate=" + d2 + " U/h, timestamp=" + (temporaryBasal != null ? Long.valueOf(temporaryBasal.getTimestamp()) : null) + ", duration=" + (temporaryBasal != null ? Long.valueOf(temporaryBasal.getDuration()) : null) + ", rate=" + (temporaryBasal != null ? Double.valueOf(temporaryBasal.getRate()) : null));
        getLenomedPump().fromTemporaryBasal(temporaryBasal);
        getRxBus().send(new EventPumpStatusChanged(EventPumpStatusChanged.Status.DISCONNECTING));
        return true;
    }

    public final boolean tempBasalStop() {
        if (!isConnected()) {
            getAapsLogger().error(LTag.PUMPCOMM, "Pump not connected");
            return false;
        }
        getRxBus().send(new EventPumpStatusChanged(getRh().gs(R.string.stoppingtempbasal)));
        sendMessage(new TempBasalInquirePacket(getInjector()), 1000L);
        if (getLenomedPump().getTbTime() > 0 || getLenomedPump().isTempBasalInProgress()) {
            TempBasalSettingPacket tempBasalSettingPacket = new TempBasalSettingPacket(getInjector(), (byte) 0, 0, 0, 0);
            getLenomedPump().setOtpNumber(0);
            sendMessage(tempBasalSettingPacket, 1000L);
            if (!processConfirm(tempBasalSettingPacket.getMsgType()) || !tempBasalSettingPacket.success()) {
                getAapsLogger().error(LTag.PUMPBTCOMM, "Unable to send, confirm, or execute TBR termination");
                return false;
            }
            SystemClock.sleep(1000L);
        } else {
            getAapsLogger().debug(LTag.PUMPCOMM, "No active TBR to stop");
        }
        getLenomedPump().setTempBasalInProgress(false);
        getAapsLogger().debug(LTag.PUMPCOMM, "Cleaning temporaryBasalStorage records older than " + (getDateUtil().now() - T.INSTANCE.mins(60L).msecs()));
        loadHistory((byte) 8, true);
        SystemClock.sleep(5000L);
        PumpSync.PumpState.TemporaryBasal temporaryBasal = getPumpSync().expectedPumpState().getTemporaryBasal();
        getAapsLogger().debug(LTag.PUMPCOMM, "TBR stopped: timestamp=" + (temporaryBasal != null ? Long.valueOf(temporaryBasal.getTimestamp()) : null) + ", duration=" + (temporaryBasal != null ? Long.valueOf(temporaryBasal.getDuration()) : null) + ", rate=" + (temporaryBasal != null ? Double.valueOf(temporaryBasal.getRate()) : null));
        getLenomedPump().fromTemporaryBasal(temporaryBasal);
        getRxBus().send(new EventPumpStatusChanged(EventPumpStatusChanged.Status.DISCONNECTING));
        return true;
    }

    public final boolean extendedBolus(double insulin, int durationInMinutes) {
        if (!isConnected()) {
            return false;
        }
        getRxBus().send(new EventPumpStatusChanged(getRh().gs(R.string.settingextendedbolus)));
        getAapsLogger().debug(LTag.PUMPCOMM, "insulin: " + insulin + " durationInMinutes: " + durationInMinutes);
        getLenomedPump().setOtpNumber(0);
        InjectionExtendedBolusSettingPacket injectionExtendedBolusSettingPacket = new InjectionExtendedBolusSettingPacket(getInjector(), (int) (insulin * ((double) 100)), durationInMinutes, getDateUtil().now());
        sendMessage(injectionExtendedBolusSettingPacket);
        if (!processConfirm(injectionExtendedBolusSettingPacket.getMsgType())) {
            return false;
        }
        loadHistory((byte) 4, true);
        SystemClock.sleep(2000L);
        getLenomedPump().fromExtendedBolus(getPumpSync().expectedPumpState().getExtendedBolus());
        getRxBus().send(new EventPumpStatusChanged(EventPumpStatusChanged.Status.DISCONNECTING));
        return injectionExtendedBolusSettingPacket.success();
    }

    public final boolean extendedBolusStop() {
        if (!isConnected()) {
            return false;
        }
        getRxBus().send(new EventPumpStatusChanged(getRh().gs(R.string.stoppingextendedbolus)));
        getLenomedPump().setOtpNumber(0);
        getLenomedPump().getDualStatus();
        InjectionCancelSettingPacket injectionCancelSettingPacket = new InjectionCancelSettingPacket(getInjector());
        sendMessage(injectionCancelSettingPacket);
        if (!processConfirm(injectionCancelSettingPacket.getMsgType())) {
            return false;
        }
        loadHistory((byte) 4, true);
        SystemClock.sleep(2000L);
        getLenomedPump().fromExtendedBolus(getPumpSync().expectedPumpState().getExtendedBolus());
        getRxBus().send(new EventPumpStatusChanged(EventPumpStatusChanged.Status.DISCONNECTING));
        return injectionCancelSettingPacket.success();
    }

    public final boolean updateBasalsInPump(Profile profile) {
        Intrinsics.checkNotNullParameter(profile, "profile");
        if (!isConnected()) {
            getAapsLogger().error(LTag.PUMPCOMM, "updateBasalsInPump: Pump not connected");
            return false;
        }
        getRxBus().send(new EventPumpStatusChanged(getRh().gs(R.string.updatingbasalrates)));
        Double[] dArrBuildEmbectaProfileRecord = getLenomedPump().buildEmbectaProfileRecord(profile);
        if (dArrBuildEmbectaProfileRecord.length != 24) {
            getAapsLogger().error(LTag.PUMPCOMM, "Invalid basal list size: " + dArrBuildEmbectaProfileRecord.length + ", expected 24");
            return false;
        }
        List listTake = ArraysKt.take(dArrBuildEmbectaProfileRecord, 12);
        List listDrop = ArraysKt.drop(dArrBuildEmbectaProfileRecord, 12);
        getAapsLogger().debug(LTag.PUMPCOMM, "Splitting basal rates: upper(0-11h) + lower(12-23h)");
        BasalUpperHalfSettingPacket basalUpperHalfSettingPacket = new BasalUpperHalfSettingPacket(getInjector(), 1, listTake);
        getAapsLogger().debug(LTag.PUMPCOMM, "Sending upper half basal setting (0-11h)");
        sendMessage(basalUpperHalfSettingPacket, 2000L);
        if (!processConfirm(basalUpperHalfSettingPacket.getMsgType())) {
            getAapsLogger().error(LTag.PUMPCOMM, "Upper half basal setting failed");
            return false;
        }
        BasalLowerHalfSettingPacket basalLowerHalfSettingPacket = new BasalLowerHalfSettingPacket(getInjector(), 1, listDrop);
        getAapsLogger().debug(LTag.PUMPCOMM, "Sending lower half basal setting (12-23h)");
        sendMessage(basalLowerHalfSettingPacket, 2000L);
        if (!processConfirm(basalLowerHalfSettingPacket.getMsgType())) {
            getAapsLogger().error(LTag.PUMPCOMM, "Lower half basal setting failed");
            return false;
        }
        getAapsLogger().debug(LTag.PUMPCOMM, "Waiting 5 seconds for pump to save basal settings...");
        SystemClock.sleep(5000L);
        readPumpStatus();
        getRxBus().send(new EventPumpStatusChanged(EventPumpStatusChanged.Status.DISCONNECTING));
        getAapsLogger().debug(LTag.PUMPCOMM, "Basal rates update completed successfully (24 segments split into upper/lower)");
        return true;
    }

    private final boolean processConfirm(byte msgType) {
        for (int i = 0; i < 30; i++) {
            if (getLenomedPump().getOtpNumber() == 0) {
                SystemClock.sleep(500L);
                getAapsLogger().error(LTag.PUMPCOMM, "OTP waiting 500ms " + i + " / 30");
            }
        }
        if (getLenomedPump().getOtpNumber() == 0) {
            getAapsLogger().error(LTag.PUMPCOMM, "otp is not received yet");
            return false;
        }
        getAapsLogger().error(LTag.PUMPCOMM, "process confirm: " + ((int) msgType));
        getLenomedPump().setOtpNumber(0);
        return true;
    }
}