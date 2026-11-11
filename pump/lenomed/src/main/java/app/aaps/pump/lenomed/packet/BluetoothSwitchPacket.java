package app.aaps.pump.lenomed.packet;

import java.nio.ByteBuffer;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: BluetoothSwitchPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class BluetoothSwitchPacket extends LenomedPacket {
    private final int enable;
    private final String friendlyName;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BluetoothSwitchPacket( int i) {
        super();
        this.enable = i;
        setMsgType((byte) -85);
        this.friendlyName = "BLUETOOTH_SWITCH";
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public byte[] encode(int msgSeq) {
        ByteBuffer byteBufferPrefixEncode$default = LenomedPacket.prefixEncode$default(this, getMsgType(), 1, (byte) 0, 4, null);
        byteBufferPrefixEncode$default.put((byte) this.enable);
        return suffixEncode(byteBufferPrefixEncode$default);
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}