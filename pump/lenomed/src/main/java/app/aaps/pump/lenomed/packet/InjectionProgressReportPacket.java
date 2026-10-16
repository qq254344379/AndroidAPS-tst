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
@Metadata(d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u0012\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u0010\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u001bH\u0016R\u001e\u0010\u0006\u001a\u00020\u00078\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u001e\u0010\f\u001a\u00020\r8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u000e\u0010\u000f\"\u0004\b\u0010\u0010\u0011R\u001e\u0010\u0012\u001a\u00020\u00138\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0014\u0010\u0015\"\u0004\b\u0016\u0010\u0017R\u0014\u0010\u001c\u001a\u00020\u001dX\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001f¨\u0006 "}, d2 = {"Lapp/aaps/pump/lenomed/packet/InjectionProgressReportPacket;", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "injector", "Ldagger/android/HasAndroidInjector;", "<init>", "(Ldagger/android/HasAndroidInjector;)V", "lenomedPump", "Lapp/aaps/pump/lenomed/LenomedPump;", "getLenomedPump", "()Lapp/aaps/pump/lenomed/LenomedPump;", "setLenomedPump", "(Lapp/aaps/pump/lenomed/LenomedPump;)V", "rxBus", "Lapp/aaps/core/interfaces/rx/bus/RxBus;", "getRxBus", "()Lapp/aaps/core/interfaces/rx/bus/RxBus;", "setRxBus", "(Lapp/aaps/core/interfaces/rx/bus/RxBus;)V", "rh", "Lapp/aaps/core/interfaces/resources/ResourceHelper;", "getRh", "()Lapp/aaps/core/interfaces/resources/ResourceHelper;", "setRh", "(Lapp/aaps/core/interfaces/resources/ResourceHelper;)V", "handleMessage", "", "data", "", "friendlyName", "", "getFriendlyName", "()Ljava/lang/String;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
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