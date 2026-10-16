package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: BluetoothSwitchResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class BluetoothSwitchResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BluetoothSwitchResponsePacket() {
        super();
        setMsgType((byte) -85);
        getAapsLogger().debug(LTag.PUMPCOMM, "BluetoothSwitchResponsePacket init");
        this.friendlyName = "PUMP_BLUETOOTH_SWITCH_RESPONSE";
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
            getAapsLogger().debug(LTag.PUMPCOMM, "BluetoothSwitchResponsePacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        int byteToInt = getByteToInt(byteBufferPrefixDecode);
        int byteToInt2 = getByteToInt(byteBufferPrefixDecode);
        if (byteToInt != 0) {
            setFailed(true);
            getAapsLogger().debug(LTag.PUMPCOMM, "BluetoothSwitch failed with code: " + byteToInt);
        } else {
            getLenomedPump().setOtpNumber(1);
            getAapsLogger().debug(LTag.PUMPCOMM, "BluetoothSwitch success --> " + byteToInt);
            getAapsLogger().debug(LTag.PUMPCOMM, "Default byte --> " + byteToInt2);
        }
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}