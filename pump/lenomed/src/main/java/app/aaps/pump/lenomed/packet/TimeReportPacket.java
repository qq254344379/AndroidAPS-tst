package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: TimeReportPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class TimeReportPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private int result;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public TimeReportPacket() {
        super();
        setMsgType((byte) -49);
        getAapsLogger().debug(LTag.PUMPCOMM, "TimeReportPacket init ");
        this.friendlyName = "PUMP_TIME_REPORT";
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

    public final int getResult() {
        return this.result;
    }

    public final void setResult(int i) {
        this.result = i;
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
        if (LenomedPacket.INSTANCE.defect(data) != 0) {
            getAapsLogger().debug(LTag.PUMPCOMM, "TimeReportPacket Report Packet Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        getLenomedPump().setYear(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setMonth(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setDay(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setHour(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setMinute(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSecond(getByteToInt(byteBufferPrefixDecode));
        getAapsLogger().debug(LTag.PUMPCOMM, "year   --> " + getLenomedPump().getYear());
        getAapsLogger().debug(LTag.PUMPCOMM, "month  --> " + getLenomedPump().getMonth());
        getAapsLogger().debug(LTag.PUMPCOMM, "day    --> " + getLenomedPump().getDay());
        getAapsLogger().debug(LTag.PUMPCOMM, "hour   --> " + getLenomedPump().getHour());
        getAapsLogger().debug(LTag.PUMPCOMM, "minute --> " + getLenomedPump().getMinute());
        getAapsLogger().debug(LTag.PUMPCOMM, "second --> " + getLenomedPump().getSecond());
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}