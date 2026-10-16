package app.aaps.pump.lenomed.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import app.aaps.pump.lenomed.R;
import com.google.android.material.button.MaterialButton;
import com.joanzapata.iconify.widget.IconTextView;

/* JADX INFO: loaded from: classes4.dex */
public final class LenomedFragmentBinding implements ViewBinding {
    public final TextView basabasalrate;
    public final TextView basalstep;
    public final IconTextView battery;
    public final TextView bolusstep;
    public final IconTextView btconnection;
    public final LinearLayout buttons;
    public final TextView dailBasal;
    public final TextView dailBolus;
    public final TextView dailyunits;
    public final TextView embectaPumpstatus;
    public final LinearLayout embectaPumpstatuslayout;
    public final TextView firmware;
    public final MaterialButton history;
    public final TextView lastbolus;
    public final TextView lastconnection;
    public final TextView queue;
    public final TextView reservoir;
    private final RelativeLayout rootView;
    public final TextView serialNumber;
    public final MaterialButton stats;
    public final TextView tempbasal;
    public final MaterialButton userOptions;

    private LenomedFragmentBinding(RelativeLayout rootView, TextView basabasalrate, TextView basalstep, IconTextView battery, TextView bolusstep, IconTextView btconnection, LinearLayout buttons, TextView dailBasal, TextView dailBolus, TextView dailyunits, TextView embectaPumpstatus, LinearLayout embectaPumpstatuslayout, TextView firmware, MaterialButton history, TextView lastbolus, TextView lastconnection, TextView queue, TextView reservoir, TextView serialNumber, MaterialButton stats, TextView tempbasal, MaterialButton userOptions) {
        this.rootView = rootView;
        this.basabasalrate = basabasalrate;
        this.basalstep = basalstep;
        this.battery = battery;
        this.bolusstep = bolusstep;
        this.btconnection = btconnection;
        this.buttons = buttons;
        this.dailBasal = dailBasal;
        this.dailBolus = dailBolus;
        this.dailyunits = dailyunits;
        this.embectaPumpstatus = embectaPumpstatus;
        this.embectaPumpstatuslayout = embectaPumpstatuslayout;
        this.firmware = firmware;
        this.history = history;
        this.lastbolus = lastbolus;
        this.lastconnection = lastconnection;
        this.queue = queue;
        this.reservoir = reservoir;
        this.serialNumber = serialNumber;
        this.stats = stats;
        this.tempbasal = tempbasal;
        this.userOptions = userOptions;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static LenomedFragmentBinding inflate(LayoutInflater inflater) {
        return inflate(inflater, null, false);
    }

    public static LenomedFragmentBinding inflate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {
        View viewInflate = inflater.inflate(R.layout.lenomed_fragment, parent, false);
        if (attachToParent) {
            parent.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static LenomedFragmentBinding bind(View rootView) {
        int i = R.id.basabasalrate;
        TextView textView = (TextView) ViewBindings.findChildViewById(rootView, i);
        if (textView != null) {
            i = R.id.basalstep;
            TextView textView2 = (TextView) ViewBindings.findChildViewById(rootView, i);
            if (textView2 != null) {
                i = R.id.battery;
                IconTextView iconTextView = (IconTextView) ViewBindings.findChildViewById(rootView, i);
                if (iconTextView != null) {
                    i = R.id.bolusstep;
                    TextView textView3 = (TextView) ViewBindings.findChildViewById(rootView, i);
                    if (textView3 != null) {
                        i = R.id.btconnection;
                        IconTextView iconTextView2 = (IconTextView) ViewBindings.findChildViewById(rootView, i);
                        if (iconTextView2 != null) {
                            i = R.id.buttons;
                            LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(rootView, i);
                            if (linearLayout != null) {
                                i = R.id.dail_basal;
                                TextView textView4 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                if (textView4 != null) {
                                    i = R.id.dail_bolus;
                                    TextView textView5 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                    if (textView5 != null) {
                                        i = R.id.dailyunits;
                                        TextView textView6 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                        if (textView6 != null) {
                                            i = R.id.embecta_pumpstatus;
                                            TextView textView7 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                            if (textView7 != null) {
                                                i = R.id.embecta_pumpstatuslayout;
                                                LinearLayout linearLayout2 = (LinearLayout) ViewBindings.findChildViewById(rootView, i);
                                                if (linearLayout2 != null) {
                                                    i = R.id.firmware;
                                                    TextView textView8 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                                    if (textView8 != null) {
                                                        i = R.id.history;
                                                        MaterialButton materialButton = (MaterialButton) ViewBindings.findChildViewById(rootView, i);
                                                        if (materialButton != null) {
                                                            i = R.id.lastbolus;
                                                            TextView textView9 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                                            if (textView9 != null) {
                                                                i = R.id.lastconnection;
                                                                TextView textView10 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                                                if (textView10 != null) {
                                                                    i = R.id.queue;
                                                                    TextView textView11 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                                                    if (textView11 != null) {
                                                                        i = R.id.reservoir;
                                                                        TextView textView12 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                                                        if (textView12 != null) {
                                                                            i = R.id.serial_number;
                                                                            TextView textView13 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                                                            if (textView13 != null) {
                                                                                i = R.id.stats;
                                                                                MaterialButton materialButton2 = (MaterialButton) ViewBindings.findChildViewById(rootView, i);
                                                                                if (materialButton2 != null) {
                                                                                    i = R.id.tempbasal;
                                                                                    TextView textView14 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                                                                    if (textView14 != null) {
                                                                                        i = R.id.user_options;
                                                                                        MaterialButton materialButton3 = (MaterialButton) ViewBindings.findChildViewById(rootView, i);
                                                                                        if (materialButton3 != null) {
                                                                                            return new LenomedFragmentBinding((RelativeLayout) rootView, textView, textView2, iconTextView, textView3, iconTextView2, linearLayout, textView4, textView5, textView6, textView7, linearLayout2, textView8, materialButton, textView9, textView10, textView11, textView12, textView13, materialButton2, textView14, materialButton3);
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