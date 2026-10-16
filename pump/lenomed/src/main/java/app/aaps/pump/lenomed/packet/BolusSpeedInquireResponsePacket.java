package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.sharedPreferences.SP;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: BolusSpeedInquireResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class BolusSpeedInquireResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    @Inject
    public SP sp;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BolusSpeedInquireResponsePacket() {
        super();
        setMsgType((byte) -123);
        getAapsLogger().debug(LTag.PUMPCOMM, "BolusSpeedInquireResponsePacket init");
        this.friendlyName = "PUMP_BOLUS_SPEED_INQUIRE_RESPONSE";
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

    public final SP getSp() {
        SP sp = this.sp;
        if (sp != null) {
            return sp;
        }
        Intrinsics.throwUninitializedPropertyAccessException("sp");
        return null;
    }

    public final void setSp(SP sp) {
        Intrinsics.checkNotNullParameter(sp, "<set-?>");
        this.sp = sp;
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
        if (LenomedPacket.INSTANCE.defect(data) != 0) {
            getAapsLogger().debug(LTag.PUMPCOMM, "BolusSpeedInquireResponsePacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        if (!isSuccInquireResponseResult(getByteToInt(byteBufferPrefixDecode))) {
            setFailed(true);
            return;
        }
        getLenomedPump().setSpeed(getByteToInt(byteBufferPrefixDecode));
        getSp().putString("g8_bolusspeed", String.valueOf(getLenomedPump().getSpeed()));
        getAapsLogger().debug(LTag.PUMPCOMM, "Result --> " + getLenomedPump().getResult());
        getAapsLogger().debug(LTag.PUMPCOMM, "bolusSpeed > " + getLenomedPump().getSpeed());
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}