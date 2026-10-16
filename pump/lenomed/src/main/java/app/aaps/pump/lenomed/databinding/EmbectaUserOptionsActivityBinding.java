package app.aaps.pump.lenomed.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import app.aaps.core.ui.elements.NumberPicker;
import app.aaps.pump.lenomed.R;
import com.google.android.material.button.MaterialButton;

/* JADX INFO: loaded from: classes4.dex */
public final class EmbectaUserOptionsActivityBinding implements ViewBinding {
    public final SwitchCompat bluetoothSwitch;
    public final NumberPicker embectaBasalDailyTotal;
    public final NumberPicker embectaBolusDailyTotal;
    public final NumberPicker embectaFixedExhaust;
    public final SwitchCompat infusionSwitch;
    public final NumberPicker maximumBasalRate;
    public final NumberPicker maximumBolusRate;
    private final ScrollView rootView;
    public final MaterialButton saveBasalDailyTotal;
    public final MaterialButton saveBluetooth;
    public final MaterialButton saveBolusDailyTotal;
    public final MaterialButton saveFixedExhaust;
    public final MaterialButton saveInfusion;
    public final MaterialButton saveMaximumBasal;
    public final MaterialButton saveMaximumBolus;

    private EmbectaUserOptionsActivityBinding(ScrollView rootView, SwitchCompat bluetoothSwitch, NumberPicker embectaBasalDailyTotal, NumberPicker embectaBolusDailyTotal, NumberPicker embectaFixedExhaust, SwitchCompat infusionSwitch, NumberPicker maximumBasalRate, NumberPicker maximumBolusRate, MaterialButton saveBasalDailyTotal, MaterialButton saveBluetooth, MaterialButton saveBolusDailyTotal, MaterialButton saveFixedExhaust, MaterialButton saveInfusion, MaterialButton saveMaximumBasal, MaterialButton saveMaximumBolus) {
        this.rootView = rootView;
        this.bluetoothSwitch = bluetoothSwitch;
        this.embectaBasalDailyTotal = embectaBasalDailyTotal;
        this.embectaBolusDailyTotal = embectaBolusDailyTotal;
        this.embectaFixedExhaust = embectaFixedExhaust;
        this.infusionSwitch = infusionSwitch;
        this.maximumBasalRate = maximumBasalRate;
        this.maximumBolusRate = maximumBolusRate;
        this.saveBasalDailyTotal = saveBasalDailyTotal;
        this.saveBluetooth = saveBluetooth;
        this.saveBolusDailyTotal = saveBolusDailyTotal;
        this.saveFixedExhaust = saveFixedExhaust;
        this.saveInfusion = saveInfusion;
        this.saveMaximumBasal = saveMaximumBasal;
        this.saveMaximumBolus = saveMaximumBolus;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ScrollView getRoot() {
        return this.rootView;
    }

    public static EmbectaUserOptionsActivityBinding inflate(LayoutInflater inflater) {
        return inflate(inflater, null, false);
    }

    public static EmbectaUserOptionsActivityBinding inflate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {
        View viewInflate = inflater.inflate(R.layout.embecta_user_options_activity, parent, false);
        if (attachToParent) {
            parent.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static EmbectaUserOptionsActivityBinding bind(View rootView) {
        int i = R.id.bluetooth_switch;
        SwitchCompat switchCompat = (SwitchCompat) ViewBindings.findChildViewById(rootView, i);
        if (switchCompat != null) {
            i = R.id.embecta_basal_daily_total;
            NumberPicker numberPicker = (NumberPicker) ViewBindings.findChildViewById(rootView, i);
            if (numberPicker != null) {
                i = R.id.embecta_bolus_daily_total;
                NumberPicker numberPicker2 = (NumberPicker) ViewBindings.findChildViewById(rootView, i);
                if (numberPicker2 != null) {
                    i = R.id.embecta_fixed_exhaust;
                    NumberPicker numberPicker3 = (NumberPicker) ViewBindings.findChildViewById(rootView, i);
                    if (numberPicker3 != null) {
                        i = R.id.infusion_switch;
                        SwitchCompat switchCompat2 = (SwitchCompat) ViewBindings.findChildViewById(rootView, i);
                        if (switchCompat2 != null) {
                            i = R.id.maximum_basal_rate;
                            NumberPicker numberPicker4 = (NumberPicker) ViewBindings.findChildViewById(rootView, i);
                            if (numberPicker4 != null) {
                                i = R.id.maximum_bolus_rate;
                                NumberPicker numberPicker5 = (NumberPicker) ViewBindings.findChildViewById(rootView, i);
                                if (numberPicker5 != null) {
                                    i = R.id.save_basal_daily_total;
                                    MaterialButton materialButton = (MaterialButton) ViewBindings.findChildViewById(rootView, i);
                                    if (materialButton != null) {
                                        i = R.id.save_bluetooth;
                                        MaterialButton materialButton2 = (MaterialButton) ViewBindings.findChildViewById(rootView, i);
                                        if (materialButton2 != null) {
                                            i = R.id.save_bolus_daily_total;
                                            MaterialButton materialButton3 = (MaterialButton) ViewBindings.findChildViewById(rootView, i);
                                            if (materialButton3 != null) {
                                                i = R.id.save_fixed_exhaust;
                                                MaterialButton materialButton4 = (MaterialButton) ViewBindings.findChildViewById(rootView, i);
                                                if (materialButton4 != null) {
                                                    i = R.id.save_infusion;
                                                    MaterialButton materialButton5 = (MaterialButton) ViewBindings.findChildViewById(rootView, i);
                                                    if (materialButton5 != null) {
                                                        i = R.id.save_maximum_basal;
                                                        MaterialButton materialButton6 = (MaterialButton) ViewBindings.findChildViewById(rootView, i);
                                                        if (materialButton6 != null) {
                                                            i = R.id.save_maximum_bolus;
                                                            MaterialButton materialButton7 = (MaterialButton) ViewBindings.findChildViewById(rootView, i);
                                                            if (materialButton7 != null) {
                                                                return new EmbectaUserOptionsActivityBinding((ScrollView) rootView, switchCompat, numberPicker, numberPicker2, numberPicker3, switchCompat2, numberPicker4, numberPicker5, materialButton, materialButton2, materialButton3, materialButton4, materialButton5, materialButton6, materialButton7);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(rootView.getResources().getResourceName(i)));
    }
}