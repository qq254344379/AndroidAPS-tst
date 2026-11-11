package app.aaps.pump.lenomed.packet;

import java.nio.ByteBuffer;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: ThresholdSettingPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class ThresholdSettingPacket extends LenomedPacket {
    private final int basalDaily;
    private final int basalSingle;
    private final int bolusDaily;
    private final int bolusSingle;
    private final int fixedExhaust;
    private final String friendlyName;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public ThresholdSettingPacket( int i, int i2, int i3, int i4, int i5) {
        super();
        this.bolusSingle = i;
        this.bolusDaily = i2;
        this.basalSingle = i3;
        this.basalDaily = i4;
        this.fixedExhaust = i5;
        setMsgType((byte) -80);
        this.friendlyName = "THRESHOLD_SETTING";
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public byte[] encode(int msgSeq) {
        ByteBuffer byteBufferPrefixEncode$default = LenomedPacket.prefixEncode$default(this, getMsgType(), 5, (byte) 0, 4, null);
        byteBufferPrefixEncode$default.put((byte) this.bolusSingle);
        byteBufferPrefixEncode$default.put((byte) this.bolusDaily);
        byteBufferPrefixEncode$default.put((byte) this.basalSingle);
        byteBufferPrefixEncode$default.put((byte) this.basalDaily);
        byteBufferPrefixEncode$default.put((byte) this.fixedExhaust);
        return suffixEncode(byteBufferPrefixEncode$default);
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}