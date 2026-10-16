package app.aaps.pump.lenomed.api;

import kotlin.Metadata;

/* JADX INFO: compiled from: LenomedApiResponse.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0002\b\t\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003HÆ\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003HÆ\u0001J\u0013\u0010\n\u001a\u00020\u00032\b\u0010\u000b\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\f\u001a\u00020\rHÖ\u0001J\t\u0010\u000e\u001a\u00020\u000fHÖ\u0001R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007¨\u0006\u0010"}, d2 = {"Lapp/aaps/pump/lenomed/api/ApiResponse;", "", "ok", "", "<init>", "(Z)V", "getOk", "()Z", "component1", "copy", "equals", "other", "hashCode", "", "toString", "", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final /* data */ class ApiResponse {
    private final boolean ok;

    public static /* synthetic */ ApiResponse copy$default(ApiResponse apiResponse, boolean z, int i, Object obj) {
        if ((i & 1) != 0) {
            z = apiResponse.ok;
        }
        return apiResponse.copy(z);
    }

    /* JADX INFO: renamed from: component1, reason: from getter */
    public final boolean getOk() {
        return this.ok;
    }

    public final ApiResponse copy(boolean ok) {
        return new ApiResponse(ok);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return (other instanceof ApiResponse) && this.ok == ((ApiResponse) other).ok;
    }

    public int hashCode() {
        return Boolean.hashCode(this.ok);
    }

    public String toString() {
        return "ApiResponse(ok=" + this.ok + ")";
    }

    public ApiResponse(boolean z) {
        this.ok = z;
    }

    public final boolean getOk() {
        return this.ok;
    }
}