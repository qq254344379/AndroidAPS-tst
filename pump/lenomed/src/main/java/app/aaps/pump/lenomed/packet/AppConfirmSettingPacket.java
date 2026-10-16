package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: AppConfirmSettingPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class AppConfirmSettingPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private int otp;
    private byte reqMsgType;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public AppConfirmSettingPacket( byte b, int i) {
        super();
        this.reqMsgType = b;
        this.otp = i;
        setMsgType((byte) 55);
        getAapsLogger().debug(LTag.PUMPCOMM, "AppConfirmSettingPacket init");
        this.friendlyName = "PUMP_APP_CONFRIM_SETTING";
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
        byteBufferPrefixEncode.put(this.reqMsgType);
        byteBufferPrefixEncode.putInt(this.otp);
        getAapsLogger().debug(LTag.PUMPCOMM, "reqMsgType -> " + ((int) this.reqMsgType));
        getAapsLogger().debug(LTag.PUMPCOMM, "otp -> " + this.otp);
        return suffixEncode(byteBufferPrefixEncode);
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}