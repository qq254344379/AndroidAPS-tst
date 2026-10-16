package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: InsulinLackReportPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class InsulinLackReportPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public InsulinLackReportPacket() {
        super();
        setMsgType((byte) -40);
        getAapsLogger().debug(LTag.PUMPCOMM, "InsulinLackReportPacket init ");
        this.friendlyName = "PUMP_INJECTION_LACK_REPORT";
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
            getAapsLogger().debug(LTag.PUMPCOMM, "InsulinLackReportPacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        getLenomedPump().setInsulinWarningGrade(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setInsulinWarningProcess(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setInsulinWarningRemain(getByteToInt(byteBufferPrefixDecode));
        getAapsLogger().debug(LTag.PUMPCOMM, "insulinWarningGrade --> " + getLenomedPump().getInsulinWarningGrade() + " (1:info, 2: warning , 3: major , 4: critical)");
        getAapsLogger().debug(LTag.PUMPCOMM, "insulinWarningProcess --> " + getLenomedPump().getInsulinWarningProcess() + " (1:skip, 2: stop , 3: ignore ) ");
        getAapsLogger().debug(LTag.PUMPCOMM, "insulinWarningRemain --> " + getLenomedPump().getInsulinWarningRemain() + " (0~100%) ");
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}