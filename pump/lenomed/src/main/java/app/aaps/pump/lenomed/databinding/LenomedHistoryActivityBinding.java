package app.aaps.pump.lenomed.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import app.aaps.pump.lenomed.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

/* JADX INFO: loaded from: classes4.dex */
public final class LenomedHistoryActivityBinding implements ViewBinding {
    public final RecyclerView recyclerview;
    public final MaterialButton reload;
    private final RelativeLayout rootView;
    public final TextView status;
    public final MaterialAutoCompleteTextView typeList;
    public final TextInputLayout typeListLayout;

    private LenomedHistoryActivityBinding(RelativeLayout rootView, RecyclerView recyclerview, MaterialButton reload, TextView status, MaterialAutoCompleteTextView typeList, TextInputLayout typeListLayout) {
        this.rootView = rootView;
        this.recyclerview = recyclerview;
        this.reload = reload;
        this.status = status;
        this.typeList = typeList;
        this.typeListLayout = typeListLayout;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static LenomedHistoryActivityBinding inflate(LayoutInflater inflater) {
        return inflate(inflater, null, false);
    }

    public static LenomedHistoryActivityBinding inflate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {
        View viewInflate = inflater.inflate(R.layout.lenomed_history_activity, parent, false);
        if (attachToParent) {
            parent.addView(viewInflate);
        }
        return bind(viewInflate);
    }

    public static LenomedHistoryActivityBinding bind(View rootView) {
        int i = R.id.recyclerview;
        RecyclerView recyclerView = (RecyclerView) ViewBindings.findChildViewById(rootView, i);
        if (recyclerView != null) {
            i = R.id.reload;
            MaterialButton materialButton = (MaterialButton) ViewBindings.findChildViewById(rootView, i);
            if (materialButton != null) {
                i = R.id.status;
                TextView textView = (TextView) ViewBindings.findChildViewById(rootView, i);
                if (textView != null) {
                    i = R.id.typeList;
                    MaterialAutoCompleteTextView materialAutoCompleteTextView = (MaterialAutoCompleteTextView) ViewBindings.findChildViewById(rootView, i);
                    if (materialAutoCompleteTextView != null) {
                        i = R.id.typeListLayout;
                        TextInputLayout textInputLayout = (TextInputLayout) ViewBindings.findChildViewById(rootView, i);
                        if (textInputLayout != null) {
                            return new LenomedHistoryActivityBinding((RelativeLayout) rootView, recyclerView, materialButton, textView, materialAutoCompleteTextView, textInputLayout);
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(rootView.getResources().getResourceName(i)));
    }
}