package app.aaps.pump.lenomed.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LenomedApiResponse.kt */
/* JADX INFO: loaded from: classes4.dex */
public final /* data */ class PumpLogDto {

    @SerializedName("app_uid")
    private final String app_uid;

    @SerializedName("app_version")
    private final String app_version;

    @SerializedName("incarnation_num")
    private final int incarnation_num;

    @SerializedName("pump_uid")
    private final String pump_uid;

    @SerializedName("pump_version")
    private final String pump_version;

    @SerializedName("pumplog_info")
    private final List<PumpLog> pumplog_info;

    /* JADX WARN: Multi-variable type inference failed */
    public static /* synthetic */ PumpLogDto copy$default(PumpLogDto pumpLogDto, String str, String str2, String str3, String str4, int i, List list, int i2, Object obj) {
        if ((i2 & 1) != 0) {
            str = pumpLogDto.app_uid;
        }
        if ((i2 & 2) != 0) {
            str2 = pumpLogDto.app_version;
        }
        if ((i2 & 4) != 0) {
            str3 = pumpLogDto.pump_uid;
        }
        if ((i2 & 8) != 0) {
            str4 = pumpLogDto.pump_version;
        }
        if ((i2 & 16) != 0) {
            i = pumpLogDto.incarnation_num;
        }
        if ((i2 & 32) != 0) {
            list = pumpLogDto.pumplog_info;
        }
        int i3 = i;
        List list2 = list;
        return pumpLogDto.copy(str, str2, str3, str4, i3, list2);
    }






    public final List<PumpLog> component6() {
        return this.pumplog_info;
    }

    public final PumpLogDto copy(String app_uid, String app_version, String pump_uid, String pump_version, int incarnation_num, List<PumpLog> pumplog_info) {
        Intrinsics.checkNotNullParameter(app_uid, "app_uid");
        Intrinsics.checkNotNullParameter(app_version, "app_version");
        Intrinsics.checkNotNullParameter(pump_uid, "pump_uid");
        Intrinsics.checkNotNullParameter(pump_version, "pump_version");
        Intrinsics.checkNotNullParameter(pumplog_info, "pumplog_info");
        return new PumpLogDto(app_uid, app_version, pump_uid, pump_version, incarnation_num, pumplog_info);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PumpLogDto)) {
            return false;
        }
        PumpLogDto pumpLogDto = (PumpLogDto) other;
        return Intrinsics.areEqual(this.app_uid, pumpLogDto.app_uid) && Intrinsics.areEqual(this.app_version, pumpLogDto.app_version) && Intrinsics.areEqual(this.pump_uid, pumpLogDto.pump_uid) && Intrinsics.areEqual(this.pump_version, pumpLogDto.pump_version) && this.incarnation_num == pumpLogDto.incarnation_num && Intrinsics.areEqual(this.pumplog_info, pumpLogDto.pumplog_info);
    }

    public int hashCode() {
        return (((((((((this.app_uid.hashCode() * 31) + this.app_version.hashCode()) * 31) + this.pump_uid.hashCode()) * 31) + this.pump_version.hashCode()) * 31) + Integer.hashCode(this.incarnation_num)) * 31) + this.pumplog_info.hashCode();
    }

    public String toString() {
        return "PumpLogDto(app_uid=" + this.app_uid + ", app_version=" + this.app_version + ", pump_uid=" + this.pump_uid + ", pump_version=" + this.pump_version + ", incarnation_num=" + this.incarnation_num + ", pumplog_info=" + this.pumplog_info + ")";
    }

    public PumpLogDto(String app_uid, String app_version, String pump_uid, String pump_version, int i, List<PumpLog> pumplog_info) {
        Intrinsics.checkNotNullParameter(app_uid, "app_uid");
        Intrinsics.checkNotNullParameter(app_version, "app_version");
        Intrinsics.checkNotNullParameter(pump_uid, "pump_uid");
        Intrinsics.checkNotNullParameter(pump_version, "pump_version");
        Intrinsics.checkNotNullParameter(pumplog_info, "pumplog_info");
        this.app_uid = app_uid;
        this.app_version = app_version;
        this.pump_uid = pump_uid;
        this.pump_version = pump_version;
        this.incarnation_num = i;
        this.pumplog_info = pumplog_info;
    }

    public final String getApp_uid() {
        return this.app_uid;
    }

    public final String getApp_version() {
        return this.app_version;
    }

    public final String getPump_uid() {
        return this.pump_uid;
    }

    public final String getPump_version() {
        return this.pump_version;
    }

    public final int getIncarnation_num() {
        return this.incarnation_num;
    }

    public final List<PumpLog> getPumplog_info() {
        return this.pumplog_info;
    }
}