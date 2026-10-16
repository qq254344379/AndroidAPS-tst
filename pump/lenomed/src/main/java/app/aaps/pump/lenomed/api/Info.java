package app.aaps.pump.lenomed.api;

import kotlin.Metadata;

/* JADX INFO: compiled from: LenomedApiResponse.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003HÆ\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003HÆ\u0001J\u0013\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\r\u001a\u00020\u000eHÖ\u0001J\t\u0010\u000f\u001a\u00020\u0010HÖ\u0001R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007¨\u0006\u0011"}, d2 = {"Lapp/aaps/pump/lenomed/api/Info;", "", "pumplog_no", "", "<init>", "(J)V", "getPumplog_no", "()J", "component1", "copy", "equals", "", "other", "hashCode", "", "toString", "", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final /* data */ class Info {
    private final long pumplog_no;

    public static /* synthetic */ Info copy$default(Info info2, long j, int i, Object obj) {
        if ((i & 1) != 0) {
            j = info2.pumplog_no;
        }
        return info2.copy(j);
    }

    /* JADX INFO: renamed from: component1, reason: from getter */
    public final long getPumplog_no() {
        return this.pumplog_no;
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