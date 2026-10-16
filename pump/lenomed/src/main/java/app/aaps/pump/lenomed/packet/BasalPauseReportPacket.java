package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: BasalPauseReportPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class BasalPauseReportPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private Integer status;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BasalPauseReportPacket() {
        super();
        setMsgType((byte) -61);
        getAapsLogger().debug(LTag.PUMPCOMM, "BasalPauseReportPacket init ");
        this.friendlyName = "PUMP_BASAL_PAUSE_REPORT";
    }

    public final LenomedPump getLenomedPump() {
        LenomedPump lenomedPump = this.lenomedPump;
        if (lenomedPump != null) {
            return lenomedPump;
        }
        Intrinsics.throwUninitializedPropertyAccessException("lenomedPump");
        return null;
    }

    public final void setLenomedPump(LenomedPump lenomedPump) {
        Intrinsics.checkNotNullParameter(lenomedPump, "<set-?>");
        this.lenomedPump = lenomedPump;
    }

    public final Integer getStatus() {
        return this.status;
    }

    public final void setStatus(Integer num) {
        this.status = num;
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
        if (LenomedPacket.INSTANCE.defect(data) != 0) {
            getAapsLogger().debug(LTag.PUMPCOMM, "BasalPauseReportPacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        this.status = Integer.valueOf(getByteToInt(LenomedPacket.INSTANCE.prefixDecode(data)));
        getAapsLogger().debug(LTag.PUMPCOMM, "status --> " + this.status);
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}