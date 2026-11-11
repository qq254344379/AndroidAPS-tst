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
import app.aaps.core.interfaces.queue.CommandQueue;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.rx.AapsSchedulers;
import app.aaps.core.interfaces.rx.bus.RxBus;
import app.aaps.core.interfaces.rx.events.EventInitializationChanged;
import app.aaps.core.interfaces.rx.events.EventPumpStatusChanged;
import app.aaps.core.interfaces.rx.events.EventQueueChanged;
import app.aaps.core.interfaces.sharedPreferences.SP;
import app.aaps.core.interfaces.ui.UiInteraction;
import app.aaps.core.interfaces.utils.DateUtil;
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy;
import app.aaps.pump.lenomed.activities.LenomedHistoryActivity;
import app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity;
import app.aaps.pump.lenomed.databinding.LenomedFragmentBinding;
import app.aaps.pump.lenomed.events.EventEmbectaNewStatus;
import dagger.android.support.DaggerFragment;
import javax.inject.Inject;
import kotlin.Unit;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Reflection;
import org.apache.commons.lang3.StringUtils;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlin.jvm.functions.Function1;

/* JADX INFO: compiled from: LenomedFragment.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class LenomedFragment extends DaggerFragment {

    private CoroutineScope eventScope;

    public final CoroutineScope getEventScope() {
        if (this.eventScope == null) {
            this.eventScope = LenomedSupportKt.lenomedIoScope();
        }
        return this.eventScope;
    }

    public final void releaseEventScope() {
        CoroutineScope scope = this.eventScope;
        if (scope != null) {
            CoroutineScopeKt.cancel(scope, null);
            this.eventScope = null;
        }
    }

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

    public LenomedFragment() {
        HandlerThread handlerThread = new HandlerThread(Reflection.getOrCreateKotlinClass(getClass()).getSimpleName() + "Handler");
        handlerThread.start();
        this.handler = new Handler(handlerThread.getLooper());
        this.refreshLoop = () -> LenomedFragment._init_$lambda$0(LenomedFragment.this);
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
            activity.runOnUiThread(() -> lenomedFragment.updateGUI());
        }
        Runnable runnable = lenomedFragment.refreshLoop;
        if (runnable != null) {
            lenomedFragment.handler.postDelayed(runnable, T.Companion.mins(1L).msecs());
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
        getBinding().history.setOnClickListener(v -> LenomedFragment.onViewCreated$lambda$0(LenomedFragment.this, v));
        // The TDD stats activity was removed from the dev branch (UiInteraction.getTddStatsActivity()
        // no longer exists); hide the button instead of leaving a dead entry point.
        getBinding().stats.setVisibility(View.GONE);
        getBinding().userOptions.setOnClickListener(v -> LenomedFragment.onViewCreated$lambda$2(LenomedFragment.this, v));
        getBinding().btconnection.setOnClickListener(v -> LenomedFragment.onViewCreated$lambda$3(LenomedFragment.this, v));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$0(LenomedFragment lenomedFragment, View view) {
        lenomedFragment.startActivity(new Intent(lenomedFragment.getContext(), (Class<?>) LenomedHistoryActivity.class));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$1(LenomedFragment lenomedFragment, View view) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$2(LenomedFragment lenomedFragment, View view) {
        lenomedFragment.startActivity(new Intent(lenomedFragment.getContext(), (Class<?>) LenomedUserOptionsActivity.class));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$3(LenomedFragment lenomedFragment, View view) {
        lenomedFragment.getAapsLogger().debug(LTag.PUMP, "Clicked connect to pump");
        lenomedFragment.getLenomedPump().setLastConnection(0L);
        LenomedSupportKt.readStatusAsync(lenomedFragment.getCommandQueue(), lenomedFragment.getRh().gs(app.aaps.core.ui.R.string.clicked_connect_to_pump));
    }

    @Override // androidx.fragment.app.Fragment
    public synchronized void onResume() {
        super.onResume();
        Runnable runnable = this.refreshLoop;
        if (runnable != null) {
            this.handler.postDelayed(runnable, T.Companion.mins(1L).msecs());
        }
        // RxBus is Flow-based on dev. The removed EventTempBasalChange / EventExtendedBolusChange
        // events are covered by EventPumpStatusChanged below, which fires on every status refresh.
        final LenomedFragment self = this;
        CoroutineScope scope = getEventScope();
        LenomedSupportKt.subscribeEvent(scope, getRxBus(), getAapsLogger(),
            Reflection.getOrCreateKotlinClass(EventInitializationChanged.class),
            (Function1<EventInitializationChanged, Unit>) it -> {
                Intrinsics.checkNotNullParameter(it, "it");
                self.updateGUI();
                return Unit.INSTANCE;
            });
        LenomedSupportKt.subscribeEvent(scope, getRxBus(), getAapsLogger(),
            Reflection.getOrCreateKotlinClass(EventEmbectaNewStatus.class),
            (Function1<EventEmbectaNewStatus, Unit>) it -> {
                Intrinsics.checkNotNullParameter(it, "it");
                self.updateGUI();
                return Unit.INSTANCE;
            });
        LenomedSupportKt.subscribeEvent(scope, getRxBus(), getAapsLogger(),
            Reflection.getOrCreateKotlinClass(EventQueueChanged.class),
            (Function1<EventQueueChanged, Unit>) it -> {
                Intrinsics.checkNotNullParameter(it, "it");
                self.updateGUI();
                return Unit.INSTANCE;
            });
        LenomedSupportKt.subscribeEvent(scope, getRxBus(), getAapsLogger(),
            Reflection.getOrCreateKotlinClass(EventPumpStatusChanged.class),
            (Function1<EventPumpStatusChanged, Unit>) it -> {
                Intrinsics.checkNotNullParameter(it, "it");
                EventPumpStatusChanged.Status status = LenomedSupportKt.pumpConnectionStatus(it);
                if (status == EventPumpStatusChanged.Status.CONNECTING) {
                    self.getBinding().btconnection.setText("{fa-bluetooth-b spin} " + it.getSecondsElapsed() + "s");
                } else if (status == EventPumpStatusChanged.Status.CONNECTED) {
                    self.getBinding().btconnection.setText("{fa-bluetooth}");
                } else if (status == EventPumpStatusChanged.Status.DISCONNECTED) {
                    self.getBinding().btconnection.setText("{fa-bluetooth-b}");
                }
                Context ctx = self.requireContext();
                Intrinsics.checkNotNullExpressionValue(ctx, "requireContext(...)");
                String text = getRh().gs(LenomedSupportKt.pumpStatusText(it));
                if (!Intrinsics.areEqual(text, "")) {
                    TextView tv = self.getBinding().embectaPumpstatus;
                    tv.setText(text);
                    self.getBinding().embectaPumpstatuslayout.setVisibility(0);
                } else {
                    self.getBinding().embectaPumpstatuslayout.setVisibility(8);
                }
                return Unit.INSTANCE;
            });
        updateGUI();
    }

    @Override // androidx.fragment.app.Fragment
    public synchronized void onPause() {
        super.onPause();
        releaseEventScope();
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
        }
        if (lenomedPump.getLastBolusTime() != 0) {
            if ((((System.currentTimeMillis() - lenomedPump.getLastBolusTime()) / 60.0d) / 60.0d) / 1000.0d < 6.0d) {
                getBinding().lastbolus.setText(getDateUtil().timeString(lenomedPump.getLastBolusTime()) + StringUtils.SPACE + getDateUtil().sinceString(lenomedPump.getLastBolusTime(), getRh()) + StringUtils.SPACE + getRh().gs(app.aaps.core.interfaces.R.string.format_insulin_units, Double.valueOf(lenomedPump.getLastBolusAmount())));
            } else {
                getBinding().dailBasal.setText("");
            }
        }
        getBinding().dailBasal.setText(getRh().gs(app.aaps.core.interfaces.R.string.formatsignedinsulinunits, Double.valueOf(lenomedPump.getTodayBaseAmount())));
        getBinding().dailBolus.setText(getRh().gs(app.aaps.core.interfaces.R.string.formatsignedinsulinunits, Double.valueOf(lenomedPump.getTodaySnackAmount())));
        double todayBaseAmount = lenomedPump.getTodayBaseAmount() + lenomedPump.getTodaySnackAmount() + lenomedPump.getTodayMealAmount();
        lenomedPump.getMaxBasal();
        lenomedPump.getMaxBolusePerDay();
        getBinding().dailyunits.setText(getRh().gs(R.string.reservoir_1_value, Double.valueOf(todayBaseAmount)));
        getBinding().basabasalrate.setText(lenomedPump.getBaseInjAmount() + " / " + getRh().gs(R.string.pump_base_basal_rate, Double.valueOf(activePump.getBaseBasalRate().getCU())));
        getBinding().tempbasal.setText(getLenomedPump().temporaryBasalToString());
        getBinding().reservoir.setText(getRh().gs(R.string.reservoir_value, Double.valueOf(lenomedPump.getSystemRemainInsulin()), 300));
        getBinding().battery.setText("{fa-battery-" + (lenomedPump.getSystemRemainBattery() / 25) + "} (" + lenomedPump.getSystemRemainBattery() + " %)");
        getBinding().firmware.setText(getRh().gs(R.string.lenomed_pump) + "\nVersion: " + lenomedPump.getMajorVersion() + "." + lenomedPump.getMinorVersion() + "." + lenomedPump.getPatchVersion() + "." + lenomedPump.getBuildVersion());
        getBinding().basalstep.setText(String.valueOf(lenomedPump.getBasalStep()));
        getBinding().bolusstep.setText(String.valueOf(lenomedPump.getBolusStep()));
        getBinding().serialNumber.setText(lenomedPump.getSerialNo());
        // CommandQueue.spannedStatus() was replaced by statusAsAnnotated() (Compose) on dev.
        // Render a plain-text summary of the queue instead: what is running + how many are waiting.
        app.aaps.core.interfaces.queue.Command performing = getCommandQueue().performing();
        StringBuilder queueStatus = new StringBuilder();
        if (performing != null) {
            queueStatus.append(performing.getCommandType().name());
        }
        int queueSize = getCommandQueue().size();
        if (queueSize > 0) {
            if (queueStatus.length() > 0) {
                queueStatus.append(" ");
            }
            queueStatus.append("(").append(queueSize).append(")");
        }
        if (queueStatus.length() == 0) {
            getBinding().queue.setVisibility(8);
        } else {
            getBinding().queue.setVisibility(0);
            getBinding().queue.setText(queueStatus.toString());
        }
        getBinding().userOptions.setVisibility(0);
    }
}