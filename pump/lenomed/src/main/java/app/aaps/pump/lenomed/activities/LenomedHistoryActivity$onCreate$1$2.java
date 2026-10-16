package app.aaps.pump.lenomed.activities;

import app.aaps.core.interfaces.queue.Callback;
import app.aaps.pump.lenomed.activities.LenomedHistoryActivity;
import app.aaps.pump.lenomed.databinding.LenomedHistoryActivityBinding;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LenomedHistoryActivity.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000\u0011\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000*\u0001\u0000\b\n\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H\u0016¨\u0006\u0004"}, d2 = {"app/aaps/pump/lenomed/activities/LenomedHistoryActivity$onCreate$1$2", "Lapp/aaps/core/interfaces/queue/Callback;", "run", "", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LenomedHistoryActivity$onCreate$1$2 extends Callback {
    final /* synthetic */ LenomedHistoryActivity.TypeList $selected;
    final /* synthetic */ LenomedHistoryActivity this$0;

    LenomedHistoryActivity$onCreate$1$2(LenomedHistoryActivity lenomedHistoryActivity, LenomedHistoryActivity.TypeList typeList) {
        this.this$0 = lenomedHistoryActivity;
        this.$selected = typeList;
    }

    @Override // java.lang.Runnable
    public void run() {
        this.this$0.swapAdapter(this.$selected.getType());
        final LenomedHistoryActivity lenomedHistoryActivity = this.this$0;
        lenomedHistoryActivity.runOnUiThread(new Runnable() { // from class: app.aaps.pump.lenomed.activities.LenomedHistoryActivity$onCreate$1$2$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                LenomedHistoryActivity$onCreate$1$2.run$lambda$0(lenomedHistoryActivity);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void run$lambda$0(LenomedHistoryActivity lenomedHistoryActivity) {
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding = lenomedHistoryActivity.binding;
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding2 = null;
        if (lenomedHistoryActivityBinding == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedHistoryActivityBinding = null;
        }
        lenomedHistoryActivityBinding.reload.setVisibility(0);
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding3 = lenomedHistoryActivity.binding;
        if (lenomedHistoryActivityBinding3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
        } else {
            lenomedHistoryActivityBinding2 = lenomedHistoryActivityBinding3;
        }
        lenomedHistoryActivityBinding2.status.setVisibility(8);
    }
}