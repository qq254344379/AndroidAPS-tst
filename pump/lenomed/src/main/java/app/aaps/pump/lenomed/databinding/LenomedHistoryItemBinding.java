package app.aaps.pump.lenomed.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import app.aaps.pump.lenomed.R;
import com.google.android.material.card.MaterialCardView;

/* JADX INFO: loaded from: classes4.dex */
public final class LenomedHistoryItemBinding implements ViewBinding {
    public final MaterialCardView danarHistoryCard;
    public final TextView embectaHistoryAlarm;
    public final TextView embectaHistoryBolustype;
    public final TextView embectaHistoryDailybasal;
    public final TextView embectaHistoryDailybolus;
    public final TextView embectaHistoryDailytotal;
    public final TextView embectaHistoryDuration;
    public final TextView embectaHistoryStringvalue;
    public final TextView embectaHistoryTime;
    public final TextView embectaHistoryValue;
    private final MaterialCardView rootView;

    private LenomedHistoryItemBinding(MaterialCardView rootView, MaterialCardView danarHistoryCard, TextView embectaHistoryAlarm, TextView embectaHistoryBolustype, TextView embectaHistoryDailybasal, TextView embectaHistoryDailybolus, TextView embectaHistoryDailytotal, TextView embectaHistoryDuration, TextView embectaHistoryStringvalue, TextView embectaHistoryTime, TextView embectaHistoryValue) {
        this.rootView = rootView;
        this.danarHistoryCard = danarHistoryCard;
        this.embectaHistoryAlarm = embectaHistoryAlarm;
        this.embectaHistoryBolustype = embectaHistoryBolustype;
        this.embectaHistoryDailybasal = embectaHistoryDailybasal;
        this.embectaHistoryDailybolus = embectaHistoryDailybolus;
        this.embectaHistoryDailytotal = embectaHistoryDailytotal;
        this.embectaHistoryDuration = embectaHistoryDuration;
        this.embectaHistoryStringvalue = embectaHistoryStringvalue;
        this.embectaHistoryTime = embectaHistoryTime;
        this.embectaHistoryValue = embectaHistoryValue;
    }

    @Override // androidx.viewbinding.ViewBinding
    public MaterialCardView getRoot() {
        return this.rootView;
    }

    public static LenomedHistoryItemBinding inflate(LayoutInflater inflater) {
        return inflate(inflater, null, false);
    }

    public static LenomedHistoryItemBinding inflate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {
        View viewInflate = inflater.inflate(R.layout.lenomed_history_item, parent, false);
        if (attachToParent) {
            parent.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static LenomedHistoryItemBinding bind(View rootView) {
        MaterialCardView materialCardView = (MaterialCardView) rootView;
        int i = R.id.embecta_history_alarm;
        TextView textView = (TextView) ViewBindings.findChildViewById(rootView, i);
        if (textView != null) {
            i = R.id.embecta_history_bolustype;
            TextView textView2 = (TextView) ViewBindings.findChildViewById(rootView, i);
            if (textView2 != null) {
                i = R.id.embecta_history_dailybasal;
                TextView textView3 = (TextView) ViewBindings.findChildViewById(rootView, i);
                if (textView3 != null) {
                    i = R.id.embecta_history_dailybolus;
                    TextView textView4 = (TextView) ViewBindings.findChildViewById(rootView, i);
                    if (textView4 != null) {
                        i = R.id.embecta_history_dailytotal;
                        TextView textView5 = (TextView) ViewBindings.findChildViewById(rootView, i);
                        if (textView5 != null) {
                            i = R.id.embecta_history_duration;
                            TextView textView6 = (TextView) ViewBindings.findChildViewById(rootView, i);
                            if (textView6 != null) {
                                i = R.id.embecta_history_stringvalue;
                                TextView textView7 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                if (textView7 != null) {
                                    i = R.id.embecta_history_time;
                                    TextView textView8 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                    if (textView8 != null) {
                                        i = R.id.embecta_history_value;
                                        TextView textView9 = (TextView) ViewBindings.findChildViewById(rootView, i);
                                        if (textView9 != null) {
                                            return new LenomedHistoryItemBinding(materialCardView, materialCardView, textView, textView2, textView3, textView4, textView5, textView6, textView7, textView8, textView9);
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