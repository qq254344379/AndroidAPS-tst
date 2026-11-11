package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.plugin.ActivePlugin;
import app.aaps.pump.lenomed.LenomedPump;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: TempBasalReportPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class TempBasalReportPacket extends LenomedPacket {

    @Inject
    public ActivePlugin activePlugin;
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public TempBasalReportPacket() {
        super();
        setMsgType((byte) -54);
        getAapsLogger().debug(LTag.PUMPCOMM, "TempBasalReportPacket init ");
        this.friendlyName = "PUMP_TEMP_BASAL_REPORT";
    }

    public final ActivePlugin getActivePlugin() {
        ActivePlugin activePlugin = this.activePlugin;
        if (activePlugin != null) {
            return activePlugin;
        }
        Intrinsics.throwUninitializedPropertyAccessException("activePlugin");
        return null;
    }

    public final void setActivePlugin(ActivePlugin activePlugin) {
        Intrinsics.checkNotNullParameter(activePlugin, "<set-?>");
        this.activePlugin = activePlugin;
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
            getAapsLogger().debug(LTag.PUMPCOMM, "TempBasalReportPacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        getLenomedPump().setTbStatus(getByteToInt(LenomedPacket.INSTANCE.prefixDecode(data)));
        getAapsLogger().debug(LTag.PUMPCOMM, "tbStatus > " + getLenomedPump().getTbStatus());
        getAapsLogger().debug(LTag.PUMPCOMM, "tbTime> " + getLenomedPump().getTbTime());
        getAapsLogger().debug(LTag.PUMPCOMM, "tbInjectRateRatio > " + getLenomedPump().getTbInjectRateRatio());
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}