package app.aaps.pump.lenomed.packet;

import java.nio.ByteBuffer;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: ThresholdSettingPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0007\n\u0002\u0010\u0012\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B7\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u0012\u0006\u0010\u0007\u001a\u00020\u0005\u0012\u0006\u0010\b\u001a\u00020\u0005\u0012\u0006\u0010\t\u001a\u00020\u0005¢\u0006\u0004\b\n\u0010\u000bJ\u0010\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u0005H\u0016R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u0014\u0010\u000f\u001a\u00020\u0010X\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012¨\u0006\u0013"}, d2 = {"Lapp/aaps/pump/lenomed/packet/ThresholdSettingPacket;", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "injector", "Ldagger/android/HasAndroidInjector;", "bolusSingle", "", "bolusDaily", "basalSingle", "basalDaily", "fixedExhaust", "<init>", "(Ldagger/android/HasAndroidInjector;IIIII)V", "encode", "", "msgSeq", "friendlyName", "", "getFriendlyName", "()Ljava/lang/String;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
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