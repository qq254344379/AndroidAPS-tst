package app.aaps.pump.lenomed.packet;

import app.aaps.pump.danars.encryption.BleEncryption;
import java.nio.ByteBuffer;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LenomedInfusionSwitchPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u0012\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005¢\u0006\u0004\b\u0006\u0010\u0007J\u0010\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u0005H\u0016R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u0014\u0010\u000b\u001a\u00020\fX\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e¨\u0006\u000f"}, d2 = {"Lapp/aaps/pump/lenomed/packet/LenomedInfusionSwitchPacket;", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "injector", "Ldagger/android/HasAndroidInjector;", "enable", "", "<init>", "(Ldagger/android/HasAndroidInjector;I)V", "encode", "", "msgSeq", "friendlyName", "", "getFriendlyName", "()Ljava/lang/String;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LenomedInfusionSwitchPacket extends LenomedPacket {
    private final int enable;
    private final String friendlyName;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public LenomedInfusionSwitchPacket( int i) {
        super();
        this.enable = i;
        setMsgType(BleEncryption.DANAR_PACKET__TYPE_COMMAND);
        this.friendlyName = "LENOMED_INFUSION_SWITCH";
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