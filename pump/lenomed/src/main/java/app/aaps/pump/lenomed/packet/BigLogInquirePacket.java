package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.compat.BleEncryption;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: BigLogInquirePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class BigLogInquirePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private final int logIndex;
    private final byte logType;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BigLogInquirePacket( byte b, int i) {
        super();
        this.logType = b;
        this.logIndex = i;
        setMsgType(BleEncryption.DANAI_PACKET__ENCRYPTION_START);
        getAapsLogger().debug(LTag.PUMPCOMM, "BigLogInquirePacket init");
        this.friendlyName = "PUMP_BIG_LOG_INQUIRE";
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
        ByteBuffer byteBufferPrefixEncode$default = LenomedPacket.prefixEncode$default(this, getMsgType(), 2, (byte) 0, 4, null);
        byteBufferPrefixEncode$default.put(this.logType);
        byteBufferPrefixEncode$default.put((byte) this.logIndex);
        return suffixEncode(byteBufferPrefixEncode$default);
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}