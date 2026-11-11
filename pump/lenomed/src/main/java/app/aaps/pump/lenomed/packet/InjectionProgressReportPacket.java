package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.rx.bus.RxBus;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: InjectionProgressReportPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class InjectionProgressReportPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    @Inject
    public ResourceHelper rh;

    @Inject
    public RxBus rxBus;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public InjectionProgressReportPacket() {
        super();
        setMsgType((byte) -22);
        getAapsLogger().debug(LTag.PUMPCOMM, "InjectionProgressReportPacket init ");
        this.friendlyName = "PUMP_INJECTION_PROGRESS_REPORT";
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
            getAapsLogger().debug(LTag.PUMPCOMM, "InjectionProgressReportPacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        double shortToInt = ((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d;
        double shortToInt2 = ((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d;
        int byteToInt = getByteToInt(byteBufferPrefixDecode);
        int byteToInt2 = getByteToInt(byteBufferPrefixDecode);
        getLenomedPump().setBolusingSetAmount(shortToInt);
        getLenomedPump().setBolusingInjAmount(shortToInt2);
        getLenomedPump().setBolusingSpeed(byteToInt);
        getLenomedPump().setBolusingInjProgress(byteToInt2);
        getAapsLogger().debug(LTag.PUMPCOMM, "bolusingSetAmount --> " + getLenomedPump().getBolusingSetAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "bolusingInjAmount --> " + getLenomedPump().getBolusingInjAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "bolusingSpeed --> " + getLenomedPump().getBolusingSpeed());
        getAapsLogger().debug(LTag.PUMPCOMM, "bolusingInjProgress --> " + getLenomedPump().getBolusingInjProgress());
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}