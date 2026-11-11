package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: DailyTotalInquireResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class DailyTotalInquireResponsePacket extends LenomedPacket {
    private double basalTotal;
    private double bolusTotal;
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public DailyTotalInquireResponsePacket() {
        super();
        setMsgType((byte) -89);
        getAapsLogger().debug(LTag.PUMPCOMM, "DailyTotalInquireResponsePacket init");
        this.friendlyName = "DAILY_TOTAL_INQUIRE_RESPONSE";
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

    public final double getBasalTotal() {
        return this.basalTotal;
    }

    public final void setBasalTotal(double d) {
        this.basalTotal = d;
    }

    public final double getBolusTotal() {
        return this.bolusTotal;
    }

    public final void setBolusTotal(double d) {
        this.bolusTotal = d;
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
        if (LenomedPacket.INSTANCE.defect(data) != 0) {
            getAapsLogger().debug(LTag.PUMPCOMM, "DailyTotalInquireResponsePacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        int byteToInt = getByteToInt(byteBufferPrefixDecode);
        int byteToInt2 = getByteToInt(byteBufferPrefixDecode);
        this.basalTotal = ((double) byteToInt) + (((double) byteToInt2) / 100.0d);
        int byteToInt3 = getByteToInt(byteBufferPrefixDecode);
        int byteToInt4 = getByteToInt(byteBufferPrefixDecode);
        this.bolusTotal = ((double) byteToInt3) + (((double) byteToInt4) / 100.0d);
        getLenomedPump().setTodayBaseAmount(this.basalTotal);
        getLenomedPump().setTodaySnackAmount(this.bolusTotal);
        getAapsLogger().debug(LTag.PUMPCOMM, "DailyTotalInquireResponsePacket: todayBaseAmount=" + this.basalTotal + " U, todaySnackAmount=" + this.bolusTotal + " U (basal: " + byteToInt + "." + byteToInt2 + ", bolus: " + byteToInt3 + "." + byteToInt4 + ")");
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}