package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: InjectionBasalInquireResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class InjectionBasalInquireResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public InjectionBasalInquireResponsePacket() {
        super();
        setMsgType((byte) -89);
        getAapsLogger().debug(LTag.PUMPCOMM, "InjectionBasalInquireResponsePacket init");
        this.friendlyName = "PUMP_INJECTION_BASAL_INQUIRE_RESPONSE";
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
            getAapsLogger().debug(LTag.PUMPCOMM, "InjectionBasalInquireResponsePacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        try {
            getLenomedPump().setTodayBaseAmount(((double) getByteToInt(byteBufferPrefixDecode)) + (((double) getByteToInt(byteBufferPrefixDecode)) / 100.0d));
            getLenomedPump().setTodayMealAmount(((double) getByteToInt(byteBufferPrefixDecode)) + (((double) getByteToInt(byteBufferPrefixDecode)) / 100.0d));
            getAapsLogger().debug(LTag.PUMPCOMM, "Result --> todayBaseAmount: " + getLenomedPump().getTodayBaseAmount() + ", todayMealAmount: " + getLenomedPump().getTodayMealAmount());
            setFailed(false);
        } catch (Exception e) {
            getAapsLogger().error(LTag.PUMPCOMM, "Error parsing payload: " + e.getMessage());
            setFailed(true);
        }
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}