package app.aaps.pump.lenomed.api;

import com.google.gson.annotations.SerializedName;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LenomedApiResponse.kt */
/* JADX INFO: loaded from: classes4.dex */
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