package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: SnackLimitInquireResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class SnackLimitInquireResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public SnackLimitInquireResponsePacket() {
        super();
        setMsgType((byte) -112);
        getAapsLogger().debug(LTag.PUMPCOMM, "SnackLimitInquireResponsePacket init");
        this.friendlyName = "PUMP_SNACK_LIMIT_INQUIRE_RESPONSE";
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
            getAapsLogger().debug(LTag.PUMPCOMM, "SnackLimitInquireResponsePacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        if (!isSuccInquireResponseResult(getByteToInt(byteBufferPrefixDecode))) {
            setFailed(true);
            return;
        }
        double d = 100;
        getLenomedPump().setMaxBolus(((double) getShortToInt(byteBufferPrefixDecode)) / d);
        getLenomedPump().setMaxBolusePerDay(((double) getShortToInt(byteBufferPrefixDecode)) / d);
        getAapsLogger().debug(LTag.PUMPCOMM, "Result --> " + getLenomedPump().getResult());
        getAapsLogger().debug(LTag.PUMPCOMM, "maxBolusesPerDay --> " + getLenomedPump().getMaxBolusePerDay());
        getAapsLogger().debug(LTag.PUMPCOMM, "maxBolus --> " + getLenomedPump().getMaxBolus());
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}