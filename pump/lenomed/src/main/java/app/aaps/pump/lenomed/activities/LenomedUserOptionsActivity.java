package app.aaps.pump.lenomed.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.ActionBar;
import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.plugin.ActivePlugin;
import app.aaps.core.interfaces.queue.Callback;
import app.aaps.core.interfaces.queue.CommandQueue;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.sharedPreferences.SP;
import app.aaps.core.interfaces.ui.UiInteraction;
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy;
import app.aaps.core.ui.activities.TranslatedDaggerAppCompatActivity;
import app.aaps.core.ui.elements.NumberPicker;
import app.aaps.core.ui.toast.ToastUtils;
import app.aaps.pump.lenomed.LenomedPump;
import app.aaps.pump.lenomed.R;
import app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity;
import app.aaps.pump.lenomed.databinding.EmbectaUserOptionsActivityBinding;
import ch.qos.logback.core.CoreConstants;
import coil.disk.DiskLruCache;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import java.text.DecimalFormat;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LenomedUserOptionsActivity.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000p\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u000b\u0018\u00002\u00020\u0001B\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\b\u0010>\u001a\u00020?H\u0014J\b\u0010@\u001a\u00020?H\u0014J\u0012\u0010A\u001a\u00020?2\b\u0010B\u001a\u0004\u0018\u00010CH\u0014J\b\u0010D\u001a\u00020?H\u0002J\b\u0010E\u001a\u00020?H\u0002J\b\u0010F\u001a\u00020?H\u0002J\b\u0010G\u001a\u00020?H\u0002J\b\u0010H\u001a\u00020?H\u0002J\b\u0010I\u001a\u00020?H\u0002J\b\u0010J\u001a\u00020?H\u0002J\b\u0010K\u001a\u00020?H\u0002J\b\u0010L\u001a\u00020?H\u0002J\b\u0010M\u001a\u00020?H\u0002R\u001e\u0010\u0004\u001a\u00020\u00058\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\tR\u001e\u0010\n\u001a\u00020\u000b8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u001e\u0010\u0010\u001a\u00020\u00118\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u001e\u0010\u0016\u001a\u00020\u00178\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0018\u0010\u0019\"\u0004\b\u001a\u0010\u001bR\u001e\u0010\u001c\u001a\u00020\u001d8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u001e\u0010\u001f\"\u0004\b \u0010!R\u001e\u0010\"\u001a\u00020#8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b$\u0010%\"\u0004\b&\u0010'R\u001e\u0010(\u001a\u00020)8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b*\u0010+\"\u0004\b,\u0010-R\u001e\u0010.\u001a\u00020/8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b0\u00101\"\u0004\b2\u00103R\u001e\u00104\u001a\u0002058\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b6\u00107\"\u0004\b8\u00109R\u000e\u0010:\u001a\u00020;X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010<\u001a\u00020=X\u0082.¢\u0006\u0002\n\u0000¨\u0006N"}, d2 = {"Lapp/aaps/pump/lenomed/activities/LenomedUserOptionsActivity;", "Lapp/aaps/core/ui/activities/TranslatedDaggerAppCompatActivity;", "<init>", "()V", "fabricPrivacy", "Lapp/aaps/core/interfaces/utils/fabric/FabricPrivacy;", "getFabricPrivacy", "()Lapp/aaps/core/interfaces/utils/fabric/FabricPrivacy;", "setFabricPrivacy", "(Lapp/aaps/core/interfaces/utils/fabric/FabricPrivacy;)V", CoreConstants.CONTEXT_SCOPE_VALUE, "Landroid/content/Context;", "getContext", "()Landroid/content/Context;", "setContext", "(Landroid/content/Context;)V", "lenomedPump", "Lapp/aaps/pump/lenomed/LenomedPump;", "getLenomedPump", "()Lapp/aaps/pump/lenomed/LenomedPump;", "setLenomedPump", "(Lapp/aaps/pump/lenomed/LenomedPump;)V", "activePlugin", "Lapp/aaps/core/interfaces/plugin/ActivePlugin;", "getActivePlugin", "()Lapp/aaps/core/interfaces/plugin/ActivePlugin;", "setActivePlugin", "(Lapp/aaps/core/interfaces/plugin/ActivePlugin;)V", "commandQueue", "Lapp/aaps/core/interfaces/queue/CommandQueue;", "getCommandQueue", "()Lapp/aaps/core/interfaces/queue/CommandQueue;", "setCommandQueue", "(Lapp/aaps/core/interfaces/queue/CommandQueue;)V", "sp", "Lapp/aaps/core/interfaces/sharedPreferences/SP;", "getSp", "()Lapp/aaps/core/interfaces/sharedPreferences/SP;", "setSp", "(Lapp/aaps/core/interfaces/sharedPreferences/SP;)V", "uiInteraction", "Lapp/aaps/core/interfaces/ui/UiInteraction;", "getUiInteraction", "()Lapp/aaps/core/interfaces/ui/UiInteraction;", "setUiInteraction", "(Lapp/aaps/core/interfaces/ui/UiInteraction;)V", "aapsLogger", "Lapp/aaps/core/interfaces/logging/AAPSLogger;", "getAapsLogger", "()Lapp/aaps/core/interfaces/logging/AAPSLogger;", "setAapsLogger", "(Lapp/aaps/core/interfaces/logging/AAPSLogger;)V", "rh", "Lapp/aaps/core/interfaces/resources/ResourceHelper;", "getRh", "()Lapp/aaps/core/interfaces/resources/ResourceHelper;", "setRh", "(Lapp/aaps/core/interfaces/resources/ResourceHelper;)V", "disposable", "Lio/reactivex/rxjava3/disposables/CompositeDisposable;", "binding", "Lapp/aaps/pump/lenomed/databinding/EmbectaUserOptionsActivityBinding;", "onResume", "", "onPause", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "initNumberPickers", "syncThresholdValues", "onSaveInfusionClick", "onSaveBluetoothClick", "onSaveMaximumBolusClick", "onSaveMaximumBasalClick", "onSaveBolusDailyTotalClick", "onSaveBasalDailyTotalClick", "onSaveFixedExhaustClick", "performSave", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LenomedUserOptionsActivity extends TranslatedDaggerAppCompatActivity {

    @Inject
    public AAPSLogger aapsLogger;

    @Inject
    public ActivePlugin activePlugin;
    private EmbectaUserOptionsActivityBinding binding;

    @Inject
    public CommandQueue commandQueue;

    @Inject
    public Context context;
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Inject
    public FabricPrivacy fabricPrivacy;

    @Inject
    public LenomedPump lenomedPump;

    @Inject
    public ResourceHelper rh;

    @Inject
    public SP sp;

    @Inject
    public UiInteraction uiInteraction;

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

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onPause() {
        this.disposable.clear();
        super.onPause();
    }

    @Override // app.aaps.core.ui.activities.TranslatedDaggerAppCompatActivity, dagger.android.support.DaggerAppCompatActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBindingInflate = EmbectaUserOptionsActivityBinding.inflate(getLayoutInflater());
        Intrinsics.checkNotNullExpressionValue(embectaUserOptionsActivityBindingInflate, "inflate(...)");
        this.binding = embectaUserOptionsActivityBindingInflate;
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding = null;
        if (embectaUserOptionsActivityBindingInflate == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBindingInflate = null;
        }
        setContentView(embectaUserOptionsActivityBindingInflate.getRoot());
        setTitle(getRh().gs(R.string.lenomed_pump_settings));
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
        ActionBar supportActionBar2 = getSupportActionBar();
        if (supportActionBar2 != null) {
            supportActionBar2.setDisplayShowHomeEnabled(true);
        }
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding2 = this.binding;
        if (embectaUserOptionsActivityBinding2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding2 = null;
        }
        embectaUserOptionsActivityBinding2.infusionSwitch.setChecked(getLenomedPump().getInfusionEnabled());
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding3 = this.binding;
        if (embectaUserOptionsActivityBinding3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding3 = null;
        }
        embectaUserOptionsActivityBinding3.bluetoothSwitch.setChecked(getLenomedPump().getBluetoothEnabled());
        initNumberPickers();
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding4 = this.binding;
        if (embectaUserOptionsActivityBinding4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding4 = null;
        }
        embectaUserOptionsActivityBinding4.saveInfusion.setOnClickListener(new View.OnClickListener() { // from class: app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.onSaveInfusionClick();
            }
        });
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding5 = this.binding;
        if (embectaUserOptionsActivityBinding5 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding5 = null;
        }
        embectaUserOptionsActivityBinding5.saveBluetooth.setOnClickListener(new View.OnClickListener() { // from class: app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.onSaveBluetoothClick();
            }
        });
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding6 = this.binding;
        if (embectaUserOptionsActivityBinding6 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding6 = null;
        }
        embectaUserOptionsActivityBinding6.saveMaximumBolus.setOnClickListener(new View.OnClickListener() { // from class: app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.onSaveMaximumBolusClick();
            }
        });
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding7 = this.binding;
        if (embectaUserOptionsActivityBinding7 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding7 = null;
        }
        embectaUserOptionsActivityBinding7.saveMaximumBasal.setOnClickListener(new View.OnClickListener() { // from class: app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.onSaveMaximumBasalClick();
            }
        });
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding8 = this.binding;
        if (embectaUserOptionsActivityBinding8 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding8 = null;
        }
        embectaUserOptionsActivityBinding8.saveBolusDailyTotal.setOnClickListener(new View.OnClickListener() { // from class: app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.onSaveBolusDailyTotalClick();
            }
        });
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding9 = this.binding;
        if (embectaUserOptionsActivityBinding9 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding9 = null;
        }
        embectaUserOptionsActivityBinding9.saveBasalDailyTotal.setOnClickListener(new View.OnClickListener() { // from class: app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.onSaveBasalDailyTotalClick();
            }
        });
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding10 = this.binding;
        if (embectaUserOptionsActivityBinding10 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
        } else {
            embectaUserOptionsActivityBinding = embectaUserOptionsActivityBinding10;
        }
        embectaUserOptionsActivityBinding.saveFixedExhaust.setOnClickListener(new View.OnClickListener() { // from class: app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.onSaveFixedExhaustClick();
            }
        });
        getAapsLogger().debug(LTag.PUMP, "UserOptionsLoaded: " + ((System.currentTimeMillis() - getLenomedPump().getLastConnection()) / ((long) 1000)) + " s ago\ninfusionEnabled: " + getLenomedPump().getInfusionEnabled() + "\nbluetoothEnabled: " + getLenomedPump().getBluetoothEnabled() + "\nmaxBolus: " + getLenomedPump().getMaxBolus() + "\nmaxBasal: " + getLenomedPump().getMaxBasal() + "\nmaxBolusePerDay: " + getLenomedPump().getMaxBolusePerDay() + "\nbasalDailyTotalThreshold: " + getLenomedPump().getBasalDailyTotalThreshold() + "\nfixedExhaustThreshold: " + getLenomedPump().getFixedExhaustThreshold());
    }

    private final void initNumberPickers() {
        double maxBolus = getLenomedPump().getMaxBolus() > 0.0d ? getLenomedPump().getMaxBolus() : getSp().getDouble(R.string.key_lenomed_bolus_single, 10.0d);
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding = this.binding;
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding2 = null;
        if (embectaUserOptionsActivityBinding == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding = null;
        }
        NumberPicker numberPicker = embectaUserOptionsActivityBinding.maximumBolusRate;
        DecimalFormat decimalFormat = new DecimalFormat(DiskLruCache.VERSION);
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding3 = this.binding;
        if (embectaUserOptionsActivityBinding3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding3 = null;
        }
        numberPicker.setParams(maxBolus, 0.0d, 25.0d, 1.0d, decimalFormat, true, embectaUserOptionsActivityBinding3.saveMaximumBolus);
        double maxBasal = getLenomedPump().getMaxBasal() > 0.0d ? getLenomedPump().getMaxBasal() : getSp().getDouble(R.string.key_lenomed_basal_single, 2.0d);
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding4 = this.binding;
        if (embectaUserOptionsActivityBinding4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding4 = null;
        }
        NumberPicker numberPicker2 = embectaUserOptionsActivityBinding4.maximumBasalRate;
        DecimalFormat decimalFormat2 = new DecimalFormat(DiskLruCache.VERSION);
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding5 = this.binding;
        if (embectaUserOptionsActivityBinding5 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding5 = null;
        }
        numberPicker2.setParams(maxBasal, 0.0d, 25.0d, 1.0d, decimalFormat2, true, embectaUserOptionsActivityBinding5.saveMaximumBasal);
        double maxBolusePerDay = getLenomedPump().getMaxBolusePerDay() > 0.0d ? getLenomedPump().getMaxBolusePerDay() : getSp().getDouble(R.string.key_lenomed_bolus_daily_total_threshold, 75.0d);
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding6 = this.binding;
        if (embectaUserOptionsActivityBinding6 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding6 = null;
        }
        NumberPicker numberPicker3 = embectaUserOptionsActivityBinding6.embectaBolusDailyTotal;
        DecimalFormat decimalFormat3 = new DecimalFormat(DiskLruCache.VERSION);
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding7 = this.binding;
        if (embectaUserOptionsActivityBinding7 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding7 = null;
        }
        numberPicker3.setParams(maxBolusePerDay, 0.0d, 75.0d, 1.0d, decimalFormat3, true, embectaUserOptionsActivityBinding7.saveBolusDailyTotal);
        double basalDailyTotalThreshold = getLenomedPump().getBasalDailyTotalThreshold() > 0.0d ? getLenomedPump().getBasalDailyTotalThreshold() : getSp().getDouble(R.string.key_lenomed_basal_daily_total_threshold, 50.0d);
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding8 = this.binding;
        if (embectaUserOptionsActivityBinding8 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding8 = null;
        }
        NumberPicker numberPicker4 = embectaUserOptionsActivityBinding8.embectaBasalDailyTotal;
        DecimalFormat decimalFormat4 = new DecimalFormat(DiskLruCache.VERSION);
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding9 = this.binding;
        if (embectaUserOptionsActivityBinding9 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding9 = null;
        }
        numberPicker4.setParams(basalDailyTotalThreshold, 0.0d, 50.0d, 1.0d, decimalFormat4, true, embectaUserOptionsActivityBinding9.saveBasalDailyTotal);
        double fixedExhaustThreshold = getLenomedPump().getFixedExhaustThreshold() > 0.0d ? getLenomedPump().getFixedExhaustThreshold() : getSp().getDouble(R.string.key_lenomed_fixed_exhaust_threshold, 5.0d);
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding10 = this.binding;
        if (embectaUserOptionsActivityBinding10 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding10 = null;
        }
        NumberPicker numberPicker5 = embectaUserOptionsActivityBinding10.embectaFixedExhaust;
        DecimalFormat decimalFormat5 = new DecimalFormat(DiskLruCache.VERSION);
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding11 = this.binding;
        if (embectaUserOptionsActivityBinding11 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
        } else {
            embectaUserOptionsActivityBinding2 = embectaUserOptionsActivityBinding11;
        }
        numberPicker5.setParams(fixedExhaustThreshold, 0.0d, 10.0d, 1.0d, decimalFormat5, true, embectaUserOptionsActivityBinding2.saveFixedExhaust);
    }

    private final void syncThresholdValues() {
        double value;
        double value2;
        double value3;
        double value4;
        double value5;
        LenomedPump lenomedPump = getLenomedPump();
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding = null;
        if (getLenomedPump().getMaxBolus() > 0.0d) {
            value = getLenomedPump().getMaxBolus();
        } else {
            EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding2 = this.binding;
            if (embectaUserOptionsActivityBinding2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("binding");
                embectaUserOptionsActivityBinding2 = null;
            }
            value = embectaUserOptionsActivityBinding2.maximumBolusRate.getValue();
        }
        lenomedPump.setMaxBolus(value);
        LenomedPump lenomedPump2 = getLenomedPump();
        if (getLenomedPump().getMaxBasal() > 0.0d) {
            value2 = getLenomedPump().getMaxBasal();
        } else {
            EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding3 = this.binding;
            if (embectaUserOptionsActivityBinding3 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("binding");
                embectaUserOptionsActivityBinding3 = null;
            }
            value2 = embectaUserOptionsActivityBinding3.maximumBasalRate.getValue();
        }
        lenomedPump2.setMaxBasal(value2);
        LenomedPump lenomedPump3 = getLenomedPump();
        if (getLenomedPump().getMaxBolusePerDay() > 0.0d) {
            value3 = getLenomedPump().getMaxBolusePerDay();
        } else {
            EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding4 = this.binding;
            if (embectaUserOptionsActivityBinding4 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("binding");
                embectaUserOptionsActivityBinding4 = null;
            }
            value3 = embectaUserOptionsActivityBinding4.embectaBolusDailyTotal.getValue();
        }
        lenomedPump3.setMaxBolusePerDay(value3);
        LenomedPump lenomedPump4 = getLenomedPump();
        if (getLenomedPump().getBasalDailyTotalThreshold() > 0.0d) {
            value4 = getLenomedPump().getBasalDailyTotalThreshold();
        } else {
            EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding5 = this.binding;
            if (embectaUserOptionsActivityBinding5 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("binding");
                embectaUserOptionsActivityBinding5 = null;
            }
            value4 = embectaUserOptionsActivityBinding5.embectaBasalDailyTotal.getValue();
        }
        lenomedPump4.setBasalDailyTotalThreshold(value4);
        LenomedPump lenomedPump5 = getLenomedPump();
        if (getLenomedPump().getFixedExhaustThreshold() > 0.0d) {
            value5 = getLenomedPump().getFixedExhaustThreshold();
        } else {
            EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding6 = this.binding;
            if (embectaUserOptionsActivityBinding6 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("binding");
            } else {
                embectaUserOptionsActivityBinding = embectaUserOptionsActivityBinding6;
            }
            value5 = embectaUserOptionsActivityBinding.embectaFixedExhaust.getValue();
        }
        lenomedPump5.setFixedExhaustThreshold(value5);
        getAapsLogger().debug(LTag.PUMP, "syncThresholdValues: Updated thresholds: maxBolus=" + getLenomedPump().getMaxBolus() + ", maxBasal=" + getLenomedPump().getMaxBasal() + ", maxBolusePerDay=" + getLenomedPump().getMaxBolusePerDay() + ", basalDailyTotalThreshold=" + getLenomedPump().getBasalDailyTotalThreshold() + ", fixedExhaustThreshold=" + getLenomedPump().getFixedExhaustThreshold());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void onSaveInfusionClick() {
        getLenomedPump().setSetUserOptionType(9);
        LenomedPump lenomedPump = getLenomedPump();
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding = this.binding;
        if (embectaUserOptionsActivityBinding == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding = null;
        }
        lenomedPump.setInfusionEnabled(embectaUserOptionsActivityBinding.infusionSwitch.isChecked());
        performSave();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void onSaveBluetoothClick() {
        getLenomedPump().setSetUserOptionType(10);
        LenomedPump lenomedPump = getLenomedPump();
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding = this.binding;
        if (embectaUserOptionsActivityBinding == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding = null;
        }
        lenomedPump.setBluetoothEnabled(embectaUserOptionsActivityBinding.bluetoothSwitch.isChecked());
        performSave();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void onSaveMaximumBolusClick() {
        getLenomedPump().setSetUserOptionType(11);
        LenomedPump lenomedPump = getLenomedPump();
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding = this.binding;
        if (embectaUserOptionsActivityBinding == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding = null;
        }
        lenomedPump.setMaxBolus(embectaUserOptionsActivityBinding.maximumBolusRate.getValue());
        getSp().putDouble(R.string.key_lenomed_bolus_single, getLenomedPump().getMaxBolus());
        performSave();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void onSaveMaximumBasalClick() {
        getLenomedPump().setSetUserOptionType(11);
        LenomedPump lenomedPump = getLenomedPump();
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding = this.binding;
        if (embectaUserOptionsActivityBinding == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding = null;
        }
        lenomedPump.setMaxBasal(embectaUserOptionsActivityBinding.maximumBasalRate.getValue());
        getSp().putDouble(R.string.key_lenomed_basal_single, getLenomedPump().getMaxBasal());
        performSave();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void onSaveBolusDailyTotalClick() {
        getLenomedPump().setSetUserOptionType(11);
        LenomedPump lenomedPump = getLenomedPump();
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding = this.binding;
        if (embectaUserOptionsActivityBinding == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding = null;
        }
        lenomedPump.setMaxBolusePerDay(embectaUserOptionsActivityBinding.embectaBolusDailyTotal.getValue());
        getSp().putDouble(R.string.key_lenomed_bolus_daily_total_threshold, getLenomedPump().getMaxBolusePerDay());
        performSave();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void onSaveBasalDailyTotalClick() {
        getLenomedPump().setSetUserOptionType(11);
        LenomedPump lenomedPump = getLenomedPump();
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding = this.binding;
        if (embectaUserOptionsActivityBinding == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding = null;
        }
        lenomedPump.setBasalDailyTotalThreshold(embectaUserOptionsActivityBinding.embectaBasalDailyTotal.getValue());
        getSp().putDouble(R.string.key_lenomed_basal_daily_total_threshold, getLenomedPump().getBasalDailyTotalThreshold());
        performSave();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void onSaveFixedExhaustClick() {
        getLenomedPump().setSetUserOptionType(11);
        LenomedPump lenomedPump = getLenomedPump();
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding = this.binding;
        if (embectaUserOptionsActivityBinding == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding = null;
        }
        lenomedPump.setFixedExhaustThreshold(embectaUserOptionsActivityBinding.embectaFixedExhaust.getValue());
        getSp().putDouble(R.string.key_lenomed_fixed_exhaust_threshold, getLenomedPump().getFixedExhaustThreshold());
        performSave();
    }

    private final void performSave() {
        if (getLenomedPump().getSetUserOptionType() == 11) {
            syncThresholdValues();
        }
        getCommandQueue().setUserOptions(new AnonymousClass1());
    }

    /* JADX INFO: renamed from: app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity$performSave$1, reason: invalid class name */
    /* JADX INFO: compiled from: LenomedUserOptionsActivity.kt */
    @Metadata(d1 = {"\u0000\u0011\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000*\u0001\u0000\b\n\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H\u0016¨\u0006\u0004"}, d2 = {"app/aaps/pump/lenomed/activities/LenomedUserOptionsActivity$performSave$1", "Lapp/aaps/core/interfaces/queue/Callback;", "run", "", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
    public static final class AnonymousClass1 extends Callback {
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() {
            if (getResult().getSuccess()) {
                final LenomedUserOptionsActivity lenomedUserOptionsActivity = LenomedUserOptionsActivity.this;
                lenomedUserOptionsActivity.runOnUiThread(new Runnable() { // from class: app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity$performSave$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        LenomedUserOptionsActivity.AnonymousClass1.run$lambda$0(lenomedUserOptionsActivity);
                    }
                });
            } else {
                final LenomedUserOptionsActivity lenomedUserOptionsActivity2 = LenomedUserOptionsActivity.this;
                lenomedUserOptionsActivity2.runOnUiThread(new Runnable() { // from class: app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity$performSave$1$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        LenomedUserOptionsActivity.AnonymousClass1.run$lambda$1(lenomedUserOptionsActivity2, this);
                    }
                });
            }
            LenomedUserOptionsActivity.this.getAapsLogger().debug(LTag.PUMP, "performSave: Result type=" + LenomedUserOptionsActivity.this.getLenomedPump().getSetUserOptionType() + ", success=" + getResult().getSuccess() + ", comment=" + getResult().getComment());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void run$lambda$0(LenomedUserOptionsActivity lenomedUserOptionsActivity) {
            EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding = lenomedUserOptionsActivity.binding;
            EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding2 = null;
            if (embectaUserOptionsActivityBinding == null) {
                Intrinsics.throwUninitializedPropertyAccessException("binding");
                embectaUserOptionsActivityBinding = null;
            }
            embectaUserOptionsActivityBinding.infusionSwitch.setChecked(lenomedUserOptionsActivity.getLenomedPump().getInfusionEnabled());
            EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding3 = lenomedUserOptionsActivity.binding;
            if (embectaUserOptionsActivityBinding3 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("binding");
            } else {
                embectaUserOptionsActivityBinding2 = embectaUserOptionsActivityBinding3;
            }
            embectaUserOptionsActivityBinding2.bluetoothSwitch.setChecked(lenomedUserOptionsActivity.getLenomedPump().getBluetoothEnabled());
            ToastUtils.okToast$default(ToastUtils.INSTANCE, lenomedUserOptionsActivity.getContext(), lenomedUserOptionsActivity.getRh().gs(R.string.save_success), false, 4, null);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void run$lambda$1(LenomedUserOptionsActivity lenomedUserOptionsActivity, AnonymousClass1 anonymousClass1) {
            lenomedUserOptionsActivity.getUiInteraction().runAlarm(anonymousClass1.getResult().getComment(), lenomedUserOptionsActivity.getRh().gs(R.string.pumperror), app.aaps.core.ui.R.raw.boluserror);
        }
    }
}