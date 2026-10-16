package app.aaps.pump.lenomed.api;

import kotlin.Metadata;

/* JADX INFO: compiled from: LenomedApiResponse.kt */
/* JADX INFO: loaded from: classes4.dex */
public final /* data */ class ApiResponse {
    private final boolean ok;

    public static /* synthetic */ ApiResponse copy$default(ApiResponse apiResponse, boolean z, int i, Object obj) {
        if ((i & 1) != 0) {
            z = apiResponse.ok;
        }
        return apiResponse.copy(z);
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