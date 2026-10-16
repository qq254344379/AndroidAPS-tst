package app.aaps.pump.lenomed.packet;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: BolusStatusInquirePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class BolusStatusInquirePacket extends LenomedPacket {
    private final String friendlyName;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BolusStatusInquirePacket() {
        super();
        setMsgType((byte) -81);
        this.friendlyName = "PUMP_BOLUS_STATUS_INQUIRE";
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public byte[] encode(int msgSeq) {
        return suffixEncode(prefixEncode(getMsgType()));
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}