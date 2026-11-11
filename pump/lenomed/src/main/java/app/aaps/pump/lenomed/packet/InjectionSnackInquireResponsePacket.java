package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: InjectionSnackInquireResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class InjectionSnackInquireResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public InjectionSnackInquireResponsePacket() {
        super();
        setMsgType((byte) -121);
        getAapsLogger().debug(LTag.PUMPCOMM, "InjectionSnackInquireResponsePacket init");
        this.friendlyName = "PUMP_INJECTION_SNACK_INQUIRE_RESPONSE";
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
    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
        if (LenomedPacket.INSTANCE.defect(data) != 0) {
            getAapsLogger().debug(LTag.PUMPCOMM, "InjectionSnackInquireResponsePacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        int byteToInt = getByteToInt(byteBufferPrefixDecode);
        if (!isSuccInquireResponseResult(byteToInt)) {
            setFailed(true);
            return;
        }
        getLenomedPump().setSnackStatus(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSnackAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setSnackInjAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setSnackSpeed(getByteToInt(byteBufferPrefixDecode));
        getAapsLogger().debug(LTag.PUMPCOMM, "Result --> " + byteToInt);
        getAapsLogger().debug(LTag.PUMPCOMM, "snackStatus > " + getLenomedPump().getSnackStatus());
        getAapsLogger().debug(LTag.PUMPCOMM, "snackAmount > " + getLenomedPump().getSnackAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "snackInjAmount > " + getLenomedPump().getSnackInjAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "snackSpeed > " + getLenomedPump().getSnackSpeed());
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}