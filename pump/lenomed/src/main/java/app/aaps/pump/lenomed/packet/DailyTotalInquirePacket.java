package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import java.nio.ByteBuffer;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: DailyTotalInquirePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class DailyTotalInquirePacket extends LenomedPacket {
    private final String friendlyName;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public DailyTotalInquirePacket() {
        super();
        setMsgType((byte) -93);
        getAapsLogger().debug(LTag.PUMPCOMM, "DailyTotalInquirePacket init");
        this.friendlyName = "DAILY_TOTAL_INQUIRE";
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public byte[] encode(int msgSeq) {
        ByteBuffer byteBufferPrefixEncode$default = LenomedPacket.prefixEncode$default(this, getMsgType(), 1, (byte) 0, 4, null);
        byteBufferPrefixEncode$default.put((byte) 84);
        byteBufferPrefixEncode$default.put((byte) 66);
        return suffixEncode(byteBufferPrefixEncode$default);
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}