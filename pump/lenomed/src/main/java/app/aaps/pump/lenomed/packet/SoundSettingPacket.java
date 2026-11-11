package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: SoundSettingPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class SoundSettingPacket extends LenomedPacket {
    private final String friendlyName;
    private int intensity;

    @Inject
    public LenomedPump lenomedPump;
    private int type;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public SoundSettingPacket( int i, int i2) {
        super();
        this.type = i;
        this.intensity = i2;
        setMsgType((byte) 13);
        getAapsLogger().debug(LTag.PUMPCOMM, "SoundSettingPacket init");
        this.friendlyName = "PUMP_SOUND_SETTING";
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

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public byte[] encode(int msgSeq) {
        ByteBuffer byteBufferPrefixEncode = prefixEncode(getMsgType());
        byteBufferPrefixEncode.put((byte) this.type);
        byteBufferPrefixEncode.put((byte) this.intensity);
        return suffixEncode(byteBufferPrefixEncode);
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}