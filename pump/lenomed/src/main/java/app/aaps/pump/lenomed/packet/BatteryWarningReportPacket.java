package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: BatteryWarningReportPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class BatteryWarningReportPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BatteryWarningReportPacket() {
        super();
        setMsgType((byte) -41);
        getAapsLogger().debug(LTag.PUMPCOMM, "BatteryWarningReportPacket init ");
        this.friendlyName = "PUMP_BATTERY_WARNING_REPORT";
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
            getAapsLogger().debug(LTag.PUMPCOMM, "BatteryWarningReportPacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        getLenomedPump().setBatteryWaningGrade(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setBatteryWaningProcess(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setBatteryWaningRemain(getByteToInt(byteBufferPrefixDecode));
        getAapsLogger().debug(LTag.PUMPCOMM, "batteryWaningGrade --> " + getLenomedPump().getBatteryWaningGrade() + " (1:info, 2: warning , 3: major , 4: critical)");
        getAapsLogger().debug(LTag.PUMPCOMM, "batteryWaningProcess --> " + getLenomedPump().getBatteryWaningProcess() + " (1:skip, 2: stop , 3: ignore ) ");
        getAapsLogger().debug(LTag.PUMPCOMM, "batteryWaningRemain --> " + getLenomedPump().getBatteryWaningRemain() + "   (0~100%) )");
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}