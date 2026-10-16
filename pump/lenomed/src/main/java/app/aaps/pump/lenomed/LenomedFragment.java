package app.aaps.pump.lenomed;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;
import app.aaps.core.data.time.T;
import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.plugin.ActivePlugin;
import app.aaps.core.interfaces.pump.Pump;
import app.aaps.core.interfaces.pump.WarnColors;
import app.aaps.core.interfaces.queue.CommandQueue;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.rx.AapsSchedulers;
import app.aaps.core.interfaces.rx.bus.RxBus;
import app.aaps.core.interfaces.rx.events.EventExtendedBolusChange;
import app.aaps.core.interfaces.rx.events.EventInitializationChanged;
import app.aaps.core.interfaces.rx.events.EventPumpStatusChanged;
import app.aaps.core.interfaces.rx.events.EventQueueChanged;
import app.aaps.core.interfaces.rx.events.EventTempBasalChange;
import app.aaps.core.interfaces.sharedPreferences.SP;
import app.aaps.core.interfaces.ui.UiInteraction;
import app.aaps.core.interfaces.utils.DateUtil;
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy;
import app.aaps.pump.lenomed.activities.LenomedHistoryActivity;
import app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity;
import app.aaps.pump.lenomed.databinding.LenomedFragmentBinding;
import app.aaps.pump.lenomed.events.EventEmbectaNewStatus;
import dagger.android.support.DaggerFragment;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.kotlin.DisposableKt;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Reflection;
import org.apache.commons.lang3.StringUtils;

/* JADX INFO: compiled from: LenomedFragment.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000¦\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0006\u0018\u00002\u00020\u0001B\u0007¢\u0006\u0004\b\u0002\u0010\u0003J$\u0010W\u001a\u00020X2\u0006\u0010Y\u001a\u00020Z2\b\u0010[\u001a\u0004\u0018\u00010\\2\b\u0010]\u001a\u0004\u0018\u00010^H\u0016J\u001a\u0010_\u001a\u00020`2\u0006\u0010a\u001a\u00020X2\b\u0010]\u001a\u0004\u0018\u00010^H\u0016J\b\u0010b\u001a\u00020`H\u0016J\b\u0010c\u001a\u00020`H\u0016J\b\u0010d\u001a\u00020`H\u0016J\b\u0010e\u001a\u00020`H\u0007R\u001e\u0010\u0004\u001a\u00020\u00058\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\tR\u001e\u0010\n\u001a\u00020\u000b8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u001e\u0010\u0010\u001a\u00020\u00118\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u001e\u0010\u0016\u001a\u00020\u00178\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0018\u0010\u0019\"\u0004\b\u001a\u0010\u001bR\u001e\u0010\u001c\u001a\u00020\u001d8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u001e\u0010\u001f\"\u0004\b \u0010!R\u001e\u0010\"\u001a\u00020#8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b$\u0010%\"\u0004\b&\u0010'R\u001e\u0010(\u001a\u00020)8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b*\u0010+\"\u0004\b,\u0010-R\u001e\u0010.\u001a\u00020/8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b0\u00101\"\u0004\b2\u00103R\u001e\u00104\u001a\u0002058\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b6\u00107\"\u0004\b8\u00109R\u001e\u0010:\u001a\u00020;8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b<\u0010=\"\u0004\b>\u0010?R\u001e\u0010@\u001a\u00020A8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\bB\u0010C\"\u0004\bD\u0010ER\u001e\u0010F\u001a\u00020G8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\bH\u0010I\"\u0004\bJ\u0010KR\u000e\u0010L\u001a\u00020MX\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010N\u001a\u00020OX\u0082\u0004¢\u0006\u0002\n\u0000R\u0010\u0010P\u001a\u0004\u0018\u00010QX\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010R\u001a\u0004\u0018\u00010SX\u0082\u000e¢\u0006\u0002\n\u0000R\u0014\u0010T\u001a\u00020S8BX\u0082\u0004¢\u0006\u0006\u001a\u0004\bU\u0010V¨\u0006f"}, d2 = {"Lapp/aaps/pump/lenomed/LenomedFragment;", "Ldagger/android/support/DaggerFragment;", "<init>", "()V", "rxBus", "Lapp/aaps/core/interfaces/rx/bus/RxBus;", "getRxBus", "()Lapp/aaps/core/interfaces/rx/bus/RxBus;", "setRxBus", "(Lapp/aaps/core/interfaces/rx/bus/RxBus;)V", "aapsLogger", "Lapp/aaps/core/interfaces/logging/AAPSLogger;", "getAapsLogger", "()Lapp/aaps/core/interfaces/logging/AAPSLogger;", "setAapsLogger", "(Lapp/aaps/core/interfaces/logging/AAPSLogger;)V", "fabricPrivacy", "Lapp/aaps/core/interfaces/utils/fabric/FabricPrivacy;", "getFabricPrivacy", "()Lapp/aaps/core/interfaces/utils/fabric/FabricPrivacy;", "setFabricPrivacy", "(Lapp/aaps/core/interfaces/utils/fabric/FabricPrivacy;)V", "commandQueue", "Lapp/aaps/core/interfaces/queue/CommandQueue;", "getCommandQueue", "()Lapp/aaps/core/interfaces/queue/CommandQueue;", "setCommandQueue", "(Lapp/aaps/core/interfaces/queue/CommandQueue;)V", "activePlugin", "Lapp/aaps/core/interfaces/plugin/ActivePlugin;", "getActivePlugin", "()Lapp/aaps/core/interfaces/plugin/ActivePlugin;", "setActivePlugin", "(Lapp/aaps/core/interfaces/plugin/ActivePlugin;)V", "lenomedPump", "Lapp/aaps/pump/lenomed/LenomedPump;", "getLenomedPump", "()Lapp/aaps/pump/lenomed/LenomedPump;", "setLenomedPump", "(Lapp/aaps/pump/lenomed/LenomedPump;)V", "rh", "Lapp/aaps/core/interfaces/resources/ResourceHelper;", "getRh", "()Lapp/aaps/core/interfaces/resources/ResourceHelper;", "setRh", "(Lapp/aaps/core/interfaces/resources/ResourceHelper;)V", "sp", "Lapp/aaps/core/interfaces/sharedPreferences/SP;", "getSp", "()Lapp/aaps/core/interfaces/sharedPreferences/SP;", "setSp", "(Lapp/aaps/core/interfaces/sharedPreferences/SP;)V", "warnColors", "Lapp/aaps/core/interfaces/pump/WarnColors;", "getWarnColors", "()Lapp/aaps/core/interfaces/pump/WarnColors;", "setWarnColors", "(Lapp/aaps/core/interfaces/pump/WarnColors;)V", "dateUtil", "Lapp/aaps/core/interfaces/utils/DateUtil;", "getDateUtil", "()Lapp/aaps/core/interfaces/utils/DateUtil;", "setDateUtil", "(Lapp/aaps/core/interfaces/utils/DateUtil;)V", "aapsSchedulers", "Lapp/aaps/core/interfaces/rx/AapsSchedulers;", "getAapsSchedulers", "()Lapp/aaps/core/interfaces/rx/AapsSchedulers;", "setAapsSchedulers", "(Lapp/aaps/core/interfaces/rx/AapsSchedulers;)V", "uiInteraction", "Lapp/aaps/core/interfaces/ui/UiInteraction;", "getUiInteraction", "()Lapp/aaps/core/interfaces/ui/UiInteraction;", "setUiInteraction", "(Lapp/aaps/core/interfaces/ui/UiInteraction;)V", "disposable", "Lio/reactivex/rxjava3/disposables/CompositeDisposable;", "handler", "Landroid/os/Handler;", "refreshLoop", "Ljava/lang/Runnable;", "_binding", "Lapp/aaps/pump/lenomed/databinding/LenomedFragmentBinding;", "binding", "getBinding", "()Lapp/aaps/pump/lenomed/databinding/LenomedFragmentBinding;", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "savedInstanceState", "Landroid/os/Bundle;", "onViewCreated", "", "view", "onResume", "onPause", "onDestroyView", "updateGUI", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LenomedFragment extends DaggerFragment {
    private LenomedFragmentBinding _binding;

    @Inject
    public AAPSLogger aapsLogger;

    @Inject
    public AapsSchedulers aapsSchedulers;

    @Inject
    public ActivePlugin activePlugin;

    @Inject
    public CommandQueue commandQueue;

    @Inject
    public DateUtil dateUtil;
    private CompositeDisposable disposable = new CompositeDisposable();

    @Inject
    public FabricPrivacy fabricPrivacy;
    private final Handler handler;

    @Inject
    public LenomedPump lenomedPump;
    private Runnable refreshLoop;

    @Inject
    public ResourceHelper rh;

    @Inject
    public RxBus rxBus;

    @Inject
    public SP sp;

    @Inject
    public UiInteraction uiInteraction;

    @Inject
    public WarnColors warnColors;

    public LenomedFragment() {
        HandlerThread handlerThread = new HandlerThread(Reflection.getOrCreateKotlinClass(getClass()).getSimpleName() + "Handler");
        handlerThread.start();
        this.handler = new Handler(handlerThread.getLooper());
        this.refreshLoop = new Runnable() { // from class: app.aaps.pump.lenomed.LenomedFragment$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                LenomedFragment._init_$lambda$0(this.f$0);
            }
        };
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

    public final WarnColors getWarnColors() {
        WarnColors warnColors = this.warnColors;
        if (warnColors != null) {
            return warnColors;
        }
        Intrinsics.throwUninitializedPropertyAccessException("warnColors");
        return null;
    }

    public final void setWarnColors(WarnColors warnColors) {
        Intrinsics.checkNotNullParameter(warnColors, "<set-?>");
        this.warnColors = warnColors;
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

    /* JADX INFO: Access modifiers changed from: private */
    public final LenomedFragmentBinding getBinding() {
        LenomedFragmentBinding lenomedFragmentBinding = this._binding;
        Intrinsics.checkNotNull(lenomedFragmentBinding);
        return lenomedFragmentBinding;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void _init_$lambda$0(final LenomedFragment lenomedFragment) {
        FragmentActivity activity = lenomedFragment.getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() { // from class: app.aaps.pump.lenomed.LenomedFragment$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.updateGUI();
                }
            });
        }
        Runnable runnable = lenomedFragment.refreshLoop;
        if (runnable != null) {
            lenomedFragment.handler.postDelayed(runnable, T.INSTANCE.mins(1L).msecs());
        }
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Intrinsics.checkNotNullParameter(inflater, "inflater");
        this._binding = LenomedFragmentBinding.inflate(inflater, container, false);
        RelativeLayout root = getBinding().getRoot();
        Intrinsics.checkNotNullExpressionValue(root, "getRoot(...)");
        return root;
    }

    @Override // androidx.fragment.app.Fragment
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Intrinsics.checkNotNullParameter(view, "view");
        super.onViewCreated(view, savedInstanceState);
        getBinding().history.setOnClickListener(new View.OnClickListener() { // from class: app.aaps.pump.lenomed.LenomedFragment$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                LenomedFragment.onViewCreated$lambda$0(this.f$0, view2);
            }
        });
        getBinding().stats.setOnClickListener(new View.OnClickListener() { // from class: app.aaps.pump.lenomed.LenomedFragment$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                LenomedFragment.onViewCreated$lambda$1(this.f$0, view2);
            }
        });
        getBinding().userOptions.setOnClickListener(new View.OnClickListener() { // from class: app.aaps.pump.lenomed.LenomedFragment$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                LenomedFragment.onViewCreated$lambda$2(this.f$0, view2);
            }
        });
        getBinding().btconnection.setOnClickListener(new View.OnClickListener() { // from class: app.aaps.pump.lenomed.LenomedFragment$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                LenomedFragment.onViewCreated$lambda$3(this.f$0, view2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$0(LenomedFragment lenomedFragment, View view) {
        lenomedFragment.startActivity(new Intent(lenomedFragment.getContext(), (Class<?>) LenomedHistoryActivity.class));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$1(LenomedFragment lenomedFragment, View view) {
        lenomedFragment.startActivity(new Intent(lenomedFragment.getContext(), lenomedFragment.getUiInteraction().getTddStatsActivity()));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$2(LenomedFragment lenomedFragment, View view) {
        lenomedFragment.startActivity(new Intent(lenomedFragment.getContext(), (Class<?>) LenomedUserOptionsActivity.class));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$3(LenomedFragment lenomedFragment, View view) {
        lenomedFragment.getAapsLogger().debug(LTag.PUMP, "Clicked connect to pump");
        lenomedFragment.getLenomedPump().setLastConnection(0L);
        lenomedFragment.getCommandQueue().readStatus(lenomedFragment.getRh().gs(app.aaps.core.ui.R.string.clicked_connect_to_pump), null);
    }

    @Override // androidx.fragment.app.Fragment
    public synchronized void onResume() {
        super.onResume();
        Runnable runnable = this.refreshLoop;
        if (runnable != null) {
            this.handler.postDelayed(runnable, T.INSTANCE.mins(1L).msecs());
        }
        CompositeDisposable compositeDisposable = this.disposable;
        Observable observableObserveOn = getRxBus().toObservable(EventInitializationChanged.class).observeOn(getAapsSchedulers().getMain());
        Consumer consumer = new Consumer() { // from class: app.aaps.pump.lenomed.LenomedFragment.onResume.2
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(EventInitializationChanged it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedFragment.this.updateGUI();
            }
        };
        final FabricPrivacy fabricPrivacy = getFabricPrivacy();
        Disposable disposableSubscribe = observableObserveOn.subscribe(consumer, new Consumer() { // from class: app.aaps.pump.lenomed.LenomedFragment.onResume.3
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(Throwable p0) {
                Intrinsics.checkNotNullParameter(p0, "p0");
                fabricPrivacy.logException(p0);
            }
        });
        Intrinsics.checkNotNullExpressionValue(disposableSubscribe, "subscribe(...)");
        DisposableKt.plusAssign(compositeDisposable, disposableSubscribe);
        CompositeDisposable compositeDisposable2 = this.disposable;
        Observable observableObserveOn2 = getRxBus().toObservable(EventEmbectaNewStatus.class).observeOn(getAapsSchedulers().getMain());
        Consumer consumer2 = new Consumer() { // from class: app.aaps.pump.lenomed.LenomedFragment.onResume.4
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(EventEmbectaNewStatus it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedFragment.this.updateGUI();
            }
        };
        final FabricPrivacy fabricPrivacy2 = getFabricPrivacy();
        Disposable disposableSubscribe2 = observableObserveOn2.subscribe(consumer2, new Consumer() { // from class: app.aaps.pump.lenomed.LenomedFragment.onResume.5
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(Throwable p0) {
                Intrinsics.checkNotNullParameter(p0, "p0");
                fabricPrivacy2.logException(p0);
            }
        });
        Intrinsics.checkNotNullExpressionValue(disposableSubscribe2, "subscribe(...)");
        DisposableKt.plusAssign(compositeDisposable2, disposableSubscribe2);
        CompositeDisposable compositeDisposable3 = this.disposable;
        Observable observableObserveOn3 = getRxBus().toObservable(EventExtendedBolusChange.class).observeOn(getAapsSchedulers().getMain());
        Consumer consumer3 = new Consumer() { // from class: app.aaps.pump.lenomed.LenomedFragment.onResume.6
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(EventExtendedBolusChange it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedFragment.this.updateGUI();
            }
        };
        final FabricPrivacy fabricPrivacy3 = getFabricPrivacy();
        Disposable disposableSubscribe3 = observableObserveOn3.subscribe(consumer3, new Consumer() { // from class: app.aaps.pump.lenomed.LenomedFragment.onResume.7
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(Throwable p0) {
                Intrinsics.checkNotNullParameter(p0, "p0");
                fabricPrivacy3.logException(p0);
            }
        });
        Intrinsics.checkNotNullExpressionValue(disposableSubscribe3, "subscribe(...)");
        DisposableKt.plusAssign(compositeDisposable3, disposableSubscribe3);
        CompositeDisposable compositeDisposable4 = this.disposable;
        Observable observableObserveOn4 = getRxBus().toObservable(EventTempBasalChange.class).observeOn(getAapsSchedulers().getMain());
        Consumer consumer4 = new Consumer() { // from class: app.aaps.pump.lenomed.LenomedFragment.onResume.8
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(EventTempBasalChange it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedFragment.this.updateGUI();
            }
        };
        final FabricPrivacy fabricPrivacy4 = getFabricPrivacy();
        Disposable disposableSubscribe4 = observableObserveOn4.subscribe(consumer4, new Consumer() { // from class: app.aaps.pump.lenomed.LenomedFragment.onResume.9
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(Throwable p0) {
                Intrinsics.checkNotNullParameter(p0, "p0");
                fabricPrivacy4.logException(p0);
            }
        });
        Intrinsics.checkNotNullExpressionValue(disposableSubscribe4, "subscribe(...)");
        DisposableKt.plusAssign(compositeDisposable4, disposableSubscribe4);
        CompositeDisposable compositeDisposable5 = this.disposable;
        Observable observableObserveOn5 = getRxBus().toObservable(EventQueueChanged.class).observeOn(getAapsSchedulers().getMain());
        Consumer consumer5 = new Consumer() { // from class: app.aaps.pump.lenomed.LenomedFragment.onResume.10
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(EventQueueChanged it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedFragment.this.updateGUI();
            }
        };
        final FabricPrivacy fabricPrivacy5 = getFabricPrivacy();
        Disposable disposableSubscribe5 = observableObserveOn5.subscribe(consumer5, new Consumer() { // from class: app.aaps.pump.lenomed.LenomedFragment.onResume.11
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(Throwable p0) {
                Intrinsics.checkNotNullParameter(p0, "p0");
                fabricPrivacy5.logException(p0);
            }
        });
        Intrinsics.checkNotNullExpressionValue(disposableSubscribe5, "subscribe(...)");
        DisposableKt.plusAssign(compositeDisposable5, disposableSubscribe5);
        CompositeDisposable compositeDisposable6 = this.disposable;
        Observable observableObserveOn6 = getRxBus().toObservable(EventPumpStatusChanged.class).observeOn(getAapsSchedulers().getMain());
        Consumer consumer6 = new Consumer() { // from class: app.aaps.pump.lenomed.LenomedFragment.onResume.12

            /* JADX INFO: renamed from: app.aaps.pump.lenomed.LenomedFragment$onResume$12$WhenMappings */
            /* JADX INFO: compiled from: LenomedFragment.kt */
            @Metadata(k = 3, mv = {2, 2, 0}, xi = 48)
            public static final /* synthetic */ class WhenMappings {
                public static final /* synthetic */ int[] $EnumSwitchMapping$0;

                static {
                    int[] iArr = new int[EventPumpStatusChanged.Status.values().length];
                    try {
                        iArr[EventPumpStatusChanged.Status.CONNECTING.ordinal()] = 1;
                    } catch (NoSuchFieldError unused) {
                    }
                    try {
                        iArr[EventPumpStatusChanged.Status.CONNECTED.ordinal()] = 2;
                    } catch (NoSuchFieldError unused2) {
                    }
                    try {
                        iArr[EventPumpStatusChanged.Status.DISCONNECTED.ordinal()] = 3;
                    } catch (NoSuchFieldError unused3) {
                    }
                    $EnumSwitchMapping$0 = iArr;
                }
            }

            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(EventPumpStatusChanged it) {
                Intrinsics.checkNotNullParameter(it, "it");
                int i = WhenMappings.$EnumSwitchMapping$0[it.getStatus().ordinal()];
                if (i == 1) {
                    LenomedFragment.this.getBinding().btconnection.setText("{fa-bluetooth-b spin} " + it.getSecondsElapsed() + "s");
                } else if (i == 2) {
                    LenomedFragment.this.getBinding().btconnection.setText("{fa-bluetooth}");
                } else if (i == 3) {
                    LenomedFragment.this.getBinding().btconnection.setText("{fa-bluetooth-b}");
                }
                Context contextRequireContext = LenomedFragment.this.requireContext();
                Intrinsics.checkNotNullExpressionValue(contextRequireContext, "requireContext(...)");
                if (!Intrinsics.areEqual(it.getStatus(contextRequireContext), "")) {
                    TextView textView = LenomedFragment.this.getBinding().embectaPumpstatus;
                    Context contextRequireContext2 = LenomedFragment.this.requireContext();
                    Intrinsics.checkNotNullExpressionValue(contextRequireContext2, "requireContext(...)");
                    textView.setText(it.getStatus(contextRequireContext2));
                    LenomedFragment.this.getBinding().embectaPumpstatuslayout.setVisibility(0);
                    return;
                }
                LenomedFragment.this.getBinding().embectaPumpstatuslayout.setVisibility(8);
            }
        };
        final FabricPrivacy fabricPrivacy6 = getFabricPrivacy();
        Disposable disposableSubscribe6 = observableObserveOn6.subscribe(consumer6, new Consumer() { // from class: app.aaps.pump.lenomed.LenomedFragment.onResume.13
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(Throwable p0) {
                Intrinsics.checkNotNullParameter(p0, "p0");
                fabricPrivacy6.logException(p0);
            }
        });
        Intrinsics.checkNotNullExpressionValue(disposableSubscribe6, "subscribe(...)");
        DisposableKt.plusAssign(compositeDisposable6, disposableSubscribe6);
        updateGUI();
    }

    @Override // androidx.fragment.app.Fragment
    public synchronized void onPause() {
        super.onPause();
        this.disposable.clear();
        Runnable runnable = this.refreshLoop;
        if (runnable != null) {
            this.handler.removeCallbacks(runnable);
        }
    }

    @Override // androidx.fragment.app.Fragment
    public synchronized void onDestroyView() {
        super.onDestroyView();
        this._binding = null;
    }

    public final synchronized void updateGUI() {
        if (this._binding == null) {
            return;
        }
        LenomedPump lenomedPump = getLenomedPump();
        Pump activePump = getActivePlugin().getActivePump();
        if (lenomedPump.getLastConnection() != 0) {
            int iCurrentTimeMillis = (int) (((System.currentTimeMillis() - lenomedPump.getLastConnection()) / 60.0d) / 1000.0d);
            getBinding().lastconnection.setText(getDateUtil().timeString(lenomedPump.getLastConnection()) + " (" + getRh().gs(app.aaps.core.interfaces.R.string.minago, Integer.valueOf(iCurrentTimeMillis)) + ")");
            getWarnColors().setColor(getBinding().lastconnection, iCurrentTimeMillis, 16.0d, 31.0d);
        }
        if (lenomedPump.getLastBolusTime() != 0) {
            if ((((System.currentTimeMillis() - lenomedPump.getLastBolusTime()) / 60.0d) / 60.0d) / 1000.0d < 6.0d) {
                getBinding().lastbolus.setText(getDateUtil().timeString(lenomedPump.getLastBolusTime()) + StringUtils.SPACE + getDateUtil().sinceString(lenomedPump.getLastBolusTime(), getRh()) + StringUtils.SPACE + getRh().gs(app.aaps.core.ui.R.string.format_insulin_units, Double.valueOf(lenomedPump.getLastBolusAmount())));
            } else {
                getBinding().dailBasal.setText("");
            }
        }
        getBinding().dailBasal.setText(getRh().gs(app.aaps.core.ui.R.string.formatsignedinsulinunits, Double.valueOf(lenomedPump.getTodayBaseAmount())));
        getBinding().dailBolus.setText(getRh().gs(app.aaps.core.ui.R.string.formatsignedinsulinunits, Double.valueOf(lenomedPump.getTodaySnackAmount())));
        double todayBaseAmount = lenomedPump.getTodayBaseAmount() + lenomedPump.getTodaySnackAmount() + lenomedPump.getTodayMealAmount();
        lenomedPump.getMaxBasal();
        lenomedPump.getMaxBolusePerDay();
        getBinding().dailyunits.setText(getRh().gs(app.aaps.core.ui.R.string.reservoir_1_value, Double.valueOf(todayBaseAmount)));
        getBinding().basabasalrate.setText(lenomedPump.getBaseInjAmount() + " / " + getRh().gs(app.aaps.core.ui.R.string.pump_base_basal_rate, Double.valueOf(activePump.getBaseBasalRate())));
        getBinding().tempbasal.setText(getLenomedPump().temporaryBasalToString());
        getBinding().reservoir.setText(getRh().gs(app.aaps.core.ui.R.string.reservoir_value, Double.valueOf(lenomedPump.getSystemRemainInsulin()), 300));
        getWarnColors().setColorInverse(getBinding().reservoir, lenomedPump.getSystemRemainInsulin(), 50, 20);
        getBinding().battery.setText("{fa-battery-" + (lenomedPump.getSystemRemainBattery() / 25) + "} (" + lenomedPump.getSystemRemainBattery() + " %)");
        getWarnColors().setColorInverse(getBinding().battery, lenomedPump.getSystemRemainBattery(), 51, 26);
        getBinding().firmware.setText(getRh().gs(R.string.lenomed_pump) + "\nVersion: " + lenomedPump.getMajorVersion() + "." + lenomedPump.getMinorVersion() + "." + lenomedPump.getPatchVersion() + "." + lenomedPump.getBuildVersion());
        getBinding().basalstep.setText(String.valueOf(lenomedPump.getBasalStep()));
        getBinding().bolusstep.setText(String.valueOf(lenomedPump.getBolusStep()));
        getBinding().serialNumber.setText(lenomedPump.getSerialNo());
        Spanned spannedSpannedStatus = getCommandQueue().spannedStatus();
        if (Intrinsics.areEqual(spannedSpannedStatus.toString(), "")) {
            getBinding().queue.setVisibility(8);
        } else {
            getBinding().queue.setVisibility(0);
            getBinding().queue.setText(spannedSpannedStatus);
        }
        getBinding().userOptions.setVisibility(0);
    }
}