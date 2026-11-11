package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LogStatusInquireResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public class LogStatusInquireResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private int result;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public LogStatusInquireResponsePacket() {
        super();
        setMsgType((byte) -106);
        getAapsLogger().debug(LTag.PUMPCOMM, "LogStatusInquireResponsePacket init");
        this.friendlyName = "PUMP_LOG_STATUS_INQUIRE_RESPONSE";
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
            getAapsLogger().debug(LTag.PUMPCOMM, "LogStatusInquireResponsePacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        int byteToInt = getByteToInt(byteBufferPrefixDecode);
        this.result = byteToInt;
        if (!isSuccInquireResponseResult(byteToInt)) {
            setFailed(true);
            return;
        }
        getLenomedPump().setPumpLastLogNum(getShortToInt(byteBufferPrefixDecode));
        getLenomedPump().setPumpWrappingCount(getByteToInt(byteBufferPrefixDecode));
        getAapsLogger().debug(LTag.PUMPCOMM, "Result --> " + this.result);
        getAapsLogger().debug(LTag.PUMPCOMM, "pumpLastLogNum > " + getLenomedPump().getPumpLastLogNum());
        getAapsLogger().debug(LTag.PUMPCOMM, "pumpWrappingCount> " + getLenomedPump().getPumpWrappingCount());
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}