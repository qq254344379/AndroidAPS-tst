package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: InjectionBlockReportPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class InjectionBlockReportPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public InjectionBlockReportPacket() {
        super();
        setMsgType((byte) -40);
        getAapsLogger().debug(LTag.PUMPCOMM, "InjectionBlockReportPacket init ");
        this.friendlyName = "PUMP_INJECTION_BLOCK_REPORT";
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
            getAapsLogger().debug(LTag.PUMPCOMM, "InjectionBasalReportPacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        getLenomedPump().setInjectionBlockGrade(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setInjectionBlockProcess(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setInjectionBlockRemainAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setInjectionBlockType(getByteToInt(byteBufferPrefixDecode));
        getAapsLogger().debug(LTag.PUMPCOMM, "injectionBlockGrade --> " + getLenomedPump().getInjectionBlockGrade() + " (1:info, 2: warning , 3: major , 4: critical)");
        getAapsLogger().debug(LTag.PUMPCOMM, "injectionBlockProcess --> " + getLenomedPump().getInjectionBlockProcess() + " (1:skip, 2: stop , 3: ignore ) ");
        getAapsLogger().debug(LTag.PUMPCOMM, "injectionBlockRemainAmount --> " + getLenomedPump().getInjectionBlockRemainAmount() + "  ");
        getAapsLogger().debug(LTag.PUMPCOMM, "injectionBlockType --> " + getLenomedPump().getInjectionBlockType() + " (1:basal, 2: meal , 3: normal , 4: square , 5:dual, 6:tube, 7:needle) ");
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}