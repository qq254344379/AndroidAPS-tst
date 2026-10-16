package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.rx.bus.RxBus;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: InjectionExtendedBolusResultReportPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class InjectionExtendedBolusResultReportPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    @Inject
    public ResourceHelper rh;

    @Inject
    public RxBus rxBus;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public InjectionExtendedBolusResultReportPacket() {
        super();
        setMsgType((byte) -27);
        getAapsLogger().debug(LTag.PUMPCOMM, "InjectionExtendedBolusResultReportPacket init ");
        this.friendlyName = "PUMP_INJECTION_EXTENDED_BOLUS_RESULT_REPORT";
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
            getAapsLogger().debug(LTag.PUMPCOMM, "InjectionExtendedBolusResultReportPacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        int byteToInt = getByteToInt(byteBufferPrefixDecode);
        int shortToInt = getShortToInt(byteBufferPrefixDecode);
        int shortToInt2 = getShortToInt(byteBufferPrefixDecode);
        double shortToInt3 = ((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d;
        double shortToInt4 = ((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d;
        getLenomedPump().setExtendedInProgress(byteToInt == 0);
        getLenomedPump().setSquareTime(shortToInt);
        getLenomedPump().setSquareInjTime(shortToInt2);
        getLenomedPump().setSquareAmount(shortToInt3);
        getLenomedPump().setSquareInjAmount(shortToInt4);
        getAapsLogger().debug(LTag.PUMPCOMM, "Result: " + byteToInt);
        getAapsLogger().debug(LTag.PUMPCOMM, "Extended bolus running: " + getLenomedPump().getSquareAmount() + " U/h");
        getAapsLogger().debug(LTag.PUMPCOMM, "Extended bolus duration: " + getLenomedPump().getSquareTime() + " min");
        getAapsLogger().debug(LTag.PUMPCOMM, "Extended bolus so far: " + getLenomedPump().getSquareInjTime() + " min");
        getAapsLogger().debug(LTag.PUMPCOMM, "Extended bolus delivered so far: " + getLenomedPump().getSquareInjAmount() + " U");
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}