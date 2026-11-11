package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: ThresholdInquirePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class ThresholdInquirePacket extends LenomedPacket {
    private final String friendlyName;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public ThresholdInquirePacket() {
        super();
        setMsgType((byte) -79);
        getAapsLogger().debug(LTag.PUMPCOMM, "ThresholdInquirePacket init");
        this.friendlyName = "PUMP_THRESHOLD_INQUIRE_REQUEST";
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