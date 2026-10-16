package app.aaps.pump.lenomed.api;

import kotlin.Metadata;

/* JADX INFO: compiled from: LenomedApiResponse.kt */
/* JADX INFO: loaded from: classes4.dex */
public final /* data */ class Info {
    private final long pumplog_no;

    public static /* synthetic */ Info copy$default(Info info2, long j, int i, Object obj) {
        if ((i & 1) != 0) {
            j = info2.pumplog_no;
        }
        return info2.copy(j);
    }


    public final Info copy(long pumplog_no) {
        return new Info(pumplog_no);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return (other instanceof Info) && this.pumplog_no == ((Info) other).pumplog_no;
    }

    public int hashCode() {
        return Long.hashCode(this.pumplog_no);
    }

    public String toString() {
        return "Info(pumplog_no=" + this.pumplog_no + ")";
    }

    public Info(long j) {
        this.pumplog_no = j;
    }

    public final long getPumplog_no() {
        return this.pumplog_no;
    }
}