package app.aaps.pump.lenomed.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import app.aaps.pump.lenomed.R;

/* JADX INFO: loaded from: classes4.dex */
public final class LenomedBlescannerActivityBinding implements ViewBinding {
    public final ListView bleScannerListview;
    public final TextView bleScannerNoDevice;
    private final LinearLayout rootView;

    private LenomedBlescannerActivityBinding(LinearLayout rootView, ListView bleScannerListview, TextView bleScannerNoDevice) {
        this.rootView = rootView;
        this.bleScannerListview = bleScannerListview;
        this.bleScannerNoDevice = bleScannerNoDevice;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static LenomedBlescannerActivityBinding inflate(LayoutInflater inflater) {
        return inflate(inflater, null, false);
    }

    public static LenomedBlescannerActivityBinding inflate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {
        View viewInflate = inflater.inflate(R.layout.lenomed_blescanner_activity, parent, false);
        if (attachToParent) {
            parent.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static LenomedBlescannerActivityBinding bind(View rootView) {
        int i = R.id.ble_scanner_listview;
        ListView listView = (ListView) ViewBindings.findChildViewById(rootView, i);
        if (listView != null) {
            i = R.id.ble_scanner_no_device;
            TextView textView = (TextView) ViewBindings.findChildViewById(rootView, i);
            if (textView != null) {
                return new LenomedBlescannerActivityBinding((LinearLayout) rootView, listView, textView);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(rootView.getResources().getResourceName(i)));
    }
}