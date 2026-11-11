package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: RejectReportPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class RejectReportPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private Integer reason;
    private Integer reqMsgType;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public RejectReportPacket() {
        super();
        setMsgType((byte) -30);
        getAapsLogger().debug(LTag.PUMPCOMM, "RejectReportPacket init ");
        this.friendlyName = "PUMP_REJECT_REPORT";
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

    public final Integer getReqMsgType() {
        return this.reqMsgType;
    }

    public final void setReqMsgType(Integer num) {
        this.reqMsgType = num;
    }

    public final Integer getReason() {
        return this.reason;
    }

    public final void setReason(Integer num) {
        this.reason = num;
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
        if (LenomedPacket.INSTANCE.defect(data) != 0) {
            getAapsLogger().debug(LTag.PUMPCOMM, "RejectReportPacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        this.reqMsgType = Integer.valueOf(getByteToInt(byteBufferPrefixDecode));
        this.reason = Integer.valueOf(getByteToInt(byteBufferPrefixDecode));
        getAapsLogger().debug(LTag.PUMPCOMM, "reqMsgType --> " + this.reqMsgType);
        getAapsLogger().debug(LTag.PUMPCOMM, "Reject Reason --> " + this.reason + " (6:cancel, 10:timeout) ");
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}