package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: ConfirmReportPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class ConfirmReportPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private Integer reqMsgType;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public ConfirmReportPacket() {
        super();
        setMsgType((byte) -24);
        getAapsLogger().debug(LTag.PUMPCOMM, "ConfirmReportPacket init ");
        this.friendlyName = "PUMP_CONFIRM_REPORT";
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

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
        if (LenomedPacket.INSTANCE.defect(data) != 0) {
            getAapsLogger().debug(LTag.PUMPCOMM, "ConfirmReportPacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        this.reqMsgType = Integer.valueOf(getByteToInt(LenomedPacket.INSTANCE.prefixDecode(data)));
        getAapsLogger().debug(LTag.PUMPCOMM, "Pump Report Confirm reqMsgType --> " + this.reqMsgType);
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}