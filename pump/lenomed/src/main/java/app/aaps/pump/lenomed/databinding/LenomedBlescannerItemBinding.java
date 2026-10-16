package app.aaps.pump.lenomed.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import app.aaps.pump.lenomed.R;

/* JADX INFO: loaded from: classes4.dex */
public final class LenomedBlescannerItemBinding implements ViewBinding {
    public final TextView bleAddress;
    public final TextView bleName;
    private final RelativeLayout rootView;

    private LenomedBlescannerItemBinding(RelativeLayout rootView, TextView bleAddress, TextView bleName) {
        this.rootView = rootView;
        this.bleAddress = bleAddress;
        this.bleName = bleName;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static LenomedBlescannerItemBinding inflate(LayoutInflater inflater) {
        return inflate(inflater, null, false);
    }

    public static LenomedBlescannerItemBinding inflate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {
        View viewInflate = inflater.inflate(R.layout.lenomed_blescanner_item, parent, false);
        if (attachToParent) {
            parent.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static LenomedBlescannerItemBinding bind(View rootView) {
        int i = R.id.ble_address;
        TextView textView = (TextView) ViewBindings.findChildViewById(rootView, i);
        if (textView != null) {
            i = R.id.ble_name;
            TextView textView2 = (TextView) ViewBindings.findChildViewById(rootView, i);
            if (textView2 != null) {
                return new LenomedBlescannerItemBinding((RelativeLayout) rootView, textView, textView2);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(rootView.getResources().getResourceName(i)));
    }
}