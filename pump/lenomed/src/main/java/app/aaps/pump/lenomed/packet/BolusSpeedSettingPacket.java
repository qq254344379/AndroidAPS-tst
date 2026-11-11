package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: BolusSpeedSettingPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class BolusSpeedSettingPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private int type;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BolusSpeedSettingPacket( int i) {
        super();
        this.type = i;
        setMsgType((byte) 5);
        getAapsLogger().debug(LTag.PUMPCOMM, "BolusSpeedSettingPacket init");
        this.friendlyName = "PUMP_BOLUS_SPEED_SETTING_PACKET";
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
        return suffixEncode(byteBufferPrefixEncode);
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}