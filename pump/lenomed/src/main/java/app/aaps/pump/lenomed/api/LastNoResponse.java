package app.aaps.pump.lenomed.api;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LenomedApiResponse.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005¢\u0006\u0004\b\u0006\u0010\u0007J\t\u0010\f\u001a\u00020\u0003HÆ\u0003J\t\u0010\r\u001a\u00020\u0005HÆ\u0003J\u001d\u0010\u000e\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005HÆ\u0001J\u0013\u0010\u000f\u001a\u00020\u00032\b\u0010\u0010\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u0011\u001a\u00020\u0012HÖ\u0001J\t\u0010\u0013\u001a\u00020\u0014HÖ\u0001R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0004\u001a\u00020\u0005¢\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b¨\u0006\u0015"}, d2 = {"Lapp/aaps/pump/lenomed/api/LastNoResponse;", "", "ok", "", "info", "Lapp/aaps/pump/lenomed/api/Info;", "<init>", "(ZLapp/aaps/pump/lenomed/api/Info;)V", "getOk", "()Z", "getInfo", "()Lapp/aaps/pump/lenomed/api/Info;", "component1", "component2", "copy", "equals", "other", "hashCode", "", "toString", "", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final /* data */ class LastNoResponse {
    private final Info info;
    private final boolean ok;

    public static /* synthetic */ LastNoResponse copy$default(LastNoResponse lastNoResponse, boolean z, Info info2, int i, Object obj) {
        if ((i & 1) != 0) {
            z = lastNoResponse.ok;
        }
        if ((i & 2) != 0) {
            info2 = lastNoResponse.info;
        }
        return lastNoResponse.copy(z, info2);
    }

    /* JADX INFO: renamed from: component1, reason: from getter */
    public final boolean getOk() {
        return this.ok;
    }

    /* JADX INFO: renamed from: component2, reason: from getter */
    public final Info getInfo() {
        return this.info;
    }

    public final LastNoResponse copy(boolean ok, Info info2) {
        Intrinsics.checkNotNullParameter(info2, "info");
        return new LastNoResponse(ok, info2);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LastNoResponse)) {
            return false;
        }
        LastNoResponse lastNoResponse = (LastNoResponse) other;
        return this.ok == lastNoResponse.ok && Intrinsics.areEqual(this.info, lastNoResponse.info);
    }

    public int hashCode() {
        return (Boolean.hashCode(this.ok) * 31) + this.info.hashCode();
    }

    public String toString() {
        return "LastNoResponse(ok=" + this.ok + ", info=" + this.info + ")";
    }

    public LastNoResponse(boolean z, Info info2) {
        Intrinsics.checkNotNullParameter(info2, "info");
        this.ok = z;
        this.info = info2;
    }

    public final Info getInfo() {
        return this.info;
    }

    public final boolean getOk() {
        return this.ok;
    }
}