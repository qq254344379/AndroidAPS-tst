package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: InjectionExtendedBolusSettingPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class InjectionExtendedBolusSettingPacket extends LenomedPacket {
    private final int amount;
    private final long bcDttm;
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private final int minutes;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public InjectionExtendedBolusSettingPacket( int i, int i2, long j) {
        super();
        this.amount = i;
        this.minutes = i2;
        this.bcDttm = j;
        setMsgType((byte) 8);
        getAapsLogger().debug(LTag.PUMPCOMM, "InjectionExtendedBolusSettingPacket init ");
        this.friendlyName = "PUMP_INJECTION_EXTENDED_BOLUS_SETTING";
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
        byteBufferPrefixEncode.putShort((short) this.minutes);
        byteBufferPrefixEncode.putShort((short) this.amount);
        byteBufferPrefixEncode.putInt((int) this.bcDttm);
        return suffixEncode(byteBufferPrefixEncode);
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}