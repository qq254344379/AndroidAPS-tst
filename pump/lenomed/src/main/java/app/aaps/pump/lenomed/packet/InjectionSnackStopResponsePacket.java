package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: InjectionSnackStopResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class InjectionSnackStopResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public InjectionSnackStopResponsePacket() {
        super();
        setMsgType((byte) -66);
        getAapsLogger().debug(LTag.PUMPCOMM, "InjectionSnackStopResponsePacket init");
        this.friendlyName = "PUMP_INJECTION_SNACK_STOP_RESPONSE";
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
            getAapsLogger().debug(LTag.PUMPCOMM, "InjectionSnackStopResponsePacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        int byteToInt = getByteToInt(byteBufferPrefixDecode);
        double byteToInt2 = ((double) getByteToInt(byteBufferPrefixDecode)) + (((double) getByteToInt(byteBufferPrefixDecode)) / 100.0d);
        getAapsLogger().debug(LTag.PUMPCOMM, "Type --> " + byteToInt);
        getAapsLogger().debug(LTag.PUMPBTCOMM, "Amount --> " + byteToInt2 + " U");
        getLenomedPump().setBolusStopped(true);
        getLenomedPump().setBolusDone(true);
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}