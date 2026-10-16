package app.aaps.pump.lenomed.api;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LenomedApiResponse.kt */
/* JADX INFO: loaded from: classes4.dex */
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