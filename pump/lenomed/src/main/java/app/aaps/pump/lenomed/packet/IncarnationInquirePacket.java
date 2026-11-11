package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.compat.BleEncryption;
import app.aaps.pump.lenomed.LenomedPump;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: IncarnationInquirePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class IncarnationInquirePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public IncarnationInquirePacket() {
        super();
        setMsgType(BleEncryption.DANAR_PACKET__ENCRYPTION_START);
        getAapsLogger().debug(LTag.PUMPCOMM, "IncarnationInquirePacket init");
        this.friendlyName = "PUMP_INCARNATION_INQUIRE";
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
        return suffixEncode(prefixEncode(getMsgType()));
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}