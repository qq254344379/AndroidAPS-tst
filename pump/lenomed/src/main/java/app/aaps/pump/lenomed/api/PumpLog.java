package app.aaps.pump.lenomed.api;

import com.google.gson.annotations.SerializedName;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LenomedApiResponse.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0010\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001B'\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0007¢\u0006\u0004\b\t\u0010\nJ\t\u0010\u0012\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0013\u001a\u00020\u0005HÆ\u0003J\t\u0010\u0014\u001a\u00020\u0007HÆ\u0003J\t\u0010\u0015\u001a\u00020\u0007HÆ\u0003J1\u0010\u0016\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u0007HÆ\u0001J\u0013\u0010\u0017\u001a\u00020\u00182\b\u0010\u0019\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u001a\u001a\u00020\u0005HÖ\u0001J\t\u0010\u001b\u001a\u00020\u0007HÖ\u0001R\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004¢\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0016\u0010\u0004\u001a\u00020\u00058\u0006X\u0087\u0004¢\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0016\u0010\u0006\u001a\u00020\u00078\u0006X\u0087\u0004¢\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0016\u0010\b\u001a\u00020\u00078\u0006X\u0087\u0004¢\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0010¨\u0006\u001c"}, d2 = {"Lapp/aaps/pump/lenomed/api/PumpLog;", "", "pumplog_no", "", "pumplog_wrapping_count", "", "pumplog_data", "", "act_type", "<init>", "(JILjava/lang/String;Ljava/lang/String;)V", "getPumplog_no", "()J", "getPumplog_wrapping_count", "()I", "getPumplog_data", "()Ljava/lang/String;", "getAct_type", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "toString", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final /* data */ class PumpLog {

    @SerializedName("act_type")
    private final String act_type;

    @SerializedName("pumplog_data")
    private final String pumplog_data;

    @SerializedName("pumplog_no")
    private final long pumplog_no;

    @SerializedName("pumplog_wrapping_count")
    private final int pumplog_wrapping_count;

    public static /* synthetic */ PumpLog copy$default(PumpLog pumpLog, long j, int i, String str, String str2, int i2, Object obj) {
        if ((i2 & 1) != 0) {
            j = pumpLog.pumplog_no;
        }
        long j2 = j;
        if ((i2 & 2) != 0) {
            i = pumpLog.pumplog_wrapping_count;
        }
        int i3 = i;
        if ((i2 & 4) != 0) {
            str = pumpLog.pumplog_data;
        }
        String str3 = str;
        if ((i2 & 8) != 0) {
            str2 = pumpLog.act_type;
        }
        return pumpLog.copy(j2, i3, str3, str2);
    }

    /* JADX INFO: renamed from: component1, reason: from getter */
    public final long getPumplog_no() {
        return this.pumplog_no;
    }

    /* JADX INFO: renamed from: component2, reason: from getter */
    public final int getPumplog_wrapping_count() {
        return this.pumplog_wrapping_count;
    }

    /* JADX INFO: renamed from: component3, reason: from getter */
    public final String getPumplog_data() {
        return this.pumplog_data;
    }

    /* JADX INFO: renamed from: component4, reason: from getter */
    public final String getAct_type() {
        return this.act_type;
    }

    public final PumpLog copy(long pumplog_no, int pumplog_wrapping_count, String pumplog_data, String act_type) {
        Intrinsics.checkNotNullParameter(pumplog_data, "pumplog_data");
        Intrinsics.checkNotNullParameter(act_type, "act_type");
        return new PumpLog(pumplog_no, pumplog_wrapping_count, pumplog_data, act_type);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PumpLog)) {
            return false;
        }
        PumpLog pumpLog = (PumpLog) other;
        return this.pumplog_no == pumpLog.pumplog_no && this.pumplog_wrapping_count == pumpLog.pumplog_wrapping_count && Intrinsics.areEqual(this.pumplog_data, pumpLog.pumplog_data) && Intrinsics.areEqual(this.act_type, pumpLog.act_type);
    }

    public int hashCode() {
        return (((((Long.hashCode(this.pumplog_no) * 31) + Integer.hashCode(this.pumplog_wrapping_count)) * 31) + this.pumplog_data.hashCode()) * 31) + this.act_type.hashCode();
    }

    public String toString() {
        return "PumpLog(pumplog_no=" + this.pumplog_no + ", pumplog_wrapping_count=" + this.pumplog_wrapping_count + ", pumplog_data=" + this.pumplog_data + ", act_type=" + this.act_type + ")";
    }

    public PumpLog(long j, int i, String pumplog_data, String act_type) {
        Intrinsics.checkNotNullParameter(pumplog_data, "pumplog_data");
        Intrinsics.checkNotNullParameter(act_type, "act_type");
        this.pumplog_no = j;
        this.pumplog_wrapping_count = i;
        this.pumplog_data = pumplog_data;
        this.act_type = act_type;
    }

    public final long getPumplog_no() {
        return this.pumplog_no;
    }

    public final int getPumplog_wrapping_count() {
        return this.pumplog_wrapping_count;
    }

    public final String getPumplog_data() {
        return this.pumplog_data;
    }

    public final String getAct_type() {
        return this.act_type;
    }
}