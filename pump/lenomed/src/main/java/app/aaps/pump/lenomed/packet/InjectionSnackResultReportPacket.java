package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.rx.bus.RxBus;
import app.aaps.core.interfaces.rx.events.EventOverviewBolusProgress;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: InjectionSnackResultReportPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class InjectionSnackResultReportPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    @Inject
    public ResourceHelper rh;

    @Inject
    public RxBus rxBus;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public InjectionSnackResultReportPacket() {
        super();
        setMsgType((byte) -28);
        getAapsLogger().debug(LTag.PUMPCOMM, "InjectionSnackResultReportPacket init ");
        this.friendlyName = "PUMP_INJECTION_SNACK_REPORT";
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

    public final RxBus getRxBus() {
        RxBus rxBus = this.rxBus;
        if (rxBus != null) {
            return rxBus;
        }
        Intrinsics.throwUninitializedPropertyAccessException("rxBus");
        return null;
    }

    public final void setRxBus(RxBus rxBus) {
        Intrinsics.checkNotNullParameter(rxBus, "<set-?>");
        this.rxBus = rxBus;
    }

    public final ResourceHelper getRh() {
        ResourceHelper resourceHelper = this.rh;
        if (resourceHelper != null) {
            return resourceHelper;
        }
        Intrinsics.throwUninitializedPropertyAccessException("rh");
        return null;
    }

    public final void setRh(ResourceHelper resourceHelper) {
        Intrinsics.checkNotNullParameter(resourceHelper, "<set-?>");
        this.rh = resourceHelper;
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
        if (LenomedPacket.INSTANCE.defect(data) != 0) {
            getAapsLogger().debug(LTag.PUMPCOMM, "InjectionSnackResultReportPacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        int byteToInt = getByteToInt(byteBufferPrefixDecode);
        double shortToInt = ((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d;
        double shortToInt2 = ((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d;
        getLenomedPump().setBolusAmountToBeDelivered(shortToInt);
        getLenomedPump().setLastBolusAmount(shortToInt2);
        getLenomedPump().setLastBolusTime(getDateUtil().now());
        EventOverviewBolusProgress.Treatment bolusingTreatment = getLenomedPump().getBolusingTreatment();
        if (bolusingTreatment != null) {
            bolusingTreatment.setInsulin(shortToInt2);
        }
        if (byteToInt == 1) {
            getLenomedPump().setBolusStopped(true);
        }
        getLenomedPump().setBolusDone(true);
        getAapsLogger().debug(LTag.PUMPCOMM, "Result --> " + byteToInt);
        getAapsLogger().debug(LTag.PUMPCOMM, "bolusAmountToBeDelivered --> " + getLenomedPump().getBolusAmountToBeDelivered());
        getAapsLogger().debug(LTag.PUMPCOMM, "lastBolusAmount --> " + getLenomedPump().getLastBolusAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "lastBolusTime --> " + getLenomedPump().getLastBolusTime());
        AAPSLogger aapsLogger = getAapsLogger();
        LTag lTag = LTag.PUMPCOMM;
        EventOverviewBolusProgress.Treatment bolusingTreatment2 = getLenomedPump().getBolusingTreatment();
        aapsLogger.debug(lTag, "embectaPump.bolusingTreatment?.insulin --> " + (bolusingTreatment2 != null ? Double.valueOf(bolusingTreatment2.getInsulin()) : null));
        getAapsLogger().debug(LTag.PUMPCOMM, "bolusDone --> " + getLenomedPump().getBolusDone());
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}