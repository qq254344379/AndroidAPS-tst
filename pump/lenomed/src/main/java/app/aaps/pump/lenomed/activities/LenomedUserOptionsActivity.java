package app.aaps.pump.lenomed.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.ActionBar;
import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.plugin.ActivePlugin;
import app.aaps.core.interfaces.queue.CommandQueue;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.sharedPreferences.SP;
import app.aaps.core.interfaces.ui.UiInteraction;
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy;
import app.aaps.core.ui.activities.TranslatedDaggerAppCompatActivity;
import app.aaps.pump.lenomed.compat.NumberPickerCompat;
import app.aaps.core.ui.toast.ToastUtils;
import app.aaps.pump.lenomed.LenomedPump;
import app.aaps.pump.lenomed.R;
import app.aaps.pump.lenomed.activities.LenomedUserOptionsActivity;
import app.aaps.pump.lenomed.databinding.EmbectaUserOptionsActivityBinding;
import java.text.DecimalFormat;
import javax.inject.Inject;
import kotlin.jvm.internal.Intrinsics;
import app.aaps.pump.lenomed.LenomedSupportKt;
import app.aaps.core.interfaces.notifications.AlarmSound;
import kotlin.Unit;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;

/* JADX INFO: compiled from: LenomedUserOptionsActivity.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class LenomedUserOptionsActivity extends TranslatedDaggerAppCompatActivity {

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

    @Inject
    public AAPSLogger aapsLogger;

    @Inject
    public ActivePlugin activePlugin;
    private EmbectaUserOptionsActivityBinding binding;

    @Inject
    public CommandQueue commandQueue;

    @Inject
    public Context context;

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
        Intrinsics.throwUninitializedPropertyAccessException("");
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
        releaseEventScope();
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
        embectaUserOptionsActivityBinding4.saveInfusion.setOnClickListener(v -> onSaveInfusionClick());
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding5 = this.binding;
        if (embectaUserOptionsActivityBinding5 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding5 = null;
        }
        embectaUserOptionsActivityBinding5.saveBluetooth.setOnClickListener(v -> onSaveBluetoothClick());
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding6 = this.binding;
        if (embectaUserOptionsActivityBinding6 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding6 = null;
        }
        embectaUserOptionsActivityBinding6.saveMaximumBolus.setOnClickListener(v -> onSaveMaximumBolusClick());
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding7 = this.binding;
        if (embectaUserOptionsActivityBinding7 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding7 = null;
        }
        embectaUserOptionsActivityBinding7.saveMaximumBasal.setOnClickListener(v -> onSaveMaximumBasalClick());
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding8 = this.binding;
        if (embectaUserOptionsActivityBinding8 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding8 = null;
        }
        embectaUserOptionsActivityBinding8.saveBolusDailyTotal.setOnClickListener(v -> onSaveBolusDailyTotalClick());
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding9 = this.binding;
        if (embectaUserOptionsActivityBinding9 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            embectaUserOptionsActivityBinding9 = null;
        }
        embectaUserOptionsActivityBinding9.saveBasalDailyTotal.setOnClickListener(v -> onSaveBasalDailyTotalClick());
        EmbectaUserOptionsActivityBinding embectaUserOptionsActivityBinding10 = this.binding;
        if (embectaUserOptionsActivityBinding10 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
        } else {
            embectaUserOptionsActivityBinding = embectaUserOptionsActivityBinding10;
        }
        embectaUserOptionsActivityBinding.saveFixedExhaust.setOnClickListener(v -> onSaveFixedExhaustClick());
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
        NumberPickerCompat numberPicker = embectaUserOptionsActivityBinding.maximumBolusRate;
        DecimalFormat decimalFormat = new DecimalFormat("0.0");
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
        NumberPickerCompat numberPicker2 = embectaUserOptionsActivityBinding4.maximumBasalRate;
        DecimalFormat decimalFormat2 = new DecimalFormat("0.0");
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
        NumberPickerCompat numberPicker3 = embectaUserOptionsActivityBinding6.embectaBolusDailyTotal;
        DecimalFormat decimalFormat3 = new DecimalFormat("0.0");
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
        NumberPickerCompat numberPicker4 = embectaUserOptionsActivityBinding8.embectaBasalDailyTotal;
        DecimalFormat decimalFormat4 = new DecimalFormat("0.0");
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
        NumberPickerCompat numberPicker5 = embectaUserOptionsActivityBinding10.embectaFixedExhaust;
        DecimalFormat decimalFormat5 = new DecimalFormat("0.0");
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
            value = embectaUserOptionsActivityBinding2.maximumBolusRate.getDecimalValue();
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
            value2 = embectaUserOptionsActivityBinding3.maximumBasalRate.getDecimalValue();
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
            value3 = embectaUserOptionsActivityBinding4.embectaBolusDailyTotal.getDecimalValue();
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
            value4 = embectaUserOptionsActivityBinding5.embectaBasalDailyTotal.getDecimalValue();
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
            value5 = embectaUserOptionsActivityBinding.embectaFixedExhaust.getDecimalValue();
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
        lenomedPump.setMaxBolus(embectaUserOptionsActivityBinding.maximumBolusRate.getDecimalValue());
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
        lenomedPump.setMaxBasal(embectaUserOptionsActivityBinding.maximumBasalRate.getDecimalValue());
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
        lenomedPump.setMaxBolusePerDay(embectaUserOptionsActivityBinding.embectaBolusDailyTotal.getDecimalValue());
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
        lenomedPump.setBasalDailyTotalThreshold(embectaUserOptionsActivityBinding.embectaBasalDailyTotal.getDecimalValue());
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
        lenomedPump.setFixedExhaustThreshold(embectaUserOptionsActivityBinding.embectaFixedExhaust.getDecimalValue());
        getSp().putDouble(R.string.key_lenomed_fixed_exhaust_threshold, getLenomedPump().getFixedExhaustThreshold());
        performSave();
    }

    private final void performSave() {
        if (getLenomedPump().getSetUserOptionType() == 11) {
            syncThresholdValues();
        }
        // CommandQueue.setUserOptions() is suspend on dev; the old Callback ran on the IO
        // dispatcher, which is where this helper invokes onResult.
        LenomedSupportKt.setUserOptionsAsync(getCommandQueue(), getEventScope(), result -> {
            if (result.getSuccess()) {
                runOnUiThread(() -> {
                    EmbectaUserOptionsActivityBinding b = this.binding;
                    if (b == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("binding");
                        b = null;
                    }
                    b.infusionSwitch.setChecked(getLenomedPump().getInfusionEnabled());
                    EmbectaUserOptionsActivityBinding b2 = this.binding;
                    if (b2 == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("binding");
                        b2 = null;
                    }
                    b2.bluetoothSwitch.setChecked(getLenomedPump().getBluetoothEnabled());
                    ToastUtils.INSTANCE.okToast(getContext(), getRh().gs(R.string.save_success), false);
                });
            } else {
                runOnUiThread(() -> getUiInteraction().runAlarm(result.getComment(), getRh().gs(R.string.pumperror), AlarmSound.BOLUS_ERROR));
            }
            getAapsLogger().debug(LTag.PUMP, "performSave: Result type=" + getLenomedPump().getSetUserOptionType() + ", success=" + result.getSuccess() + ", comment=" + result.getComment());
            return Unit.INSTANCE;
        });
    }

}
