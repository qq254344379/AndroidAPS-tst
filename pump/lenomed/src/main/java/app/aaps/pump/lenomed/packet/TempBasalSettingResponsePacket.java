package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: TempBasalSettingResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class TempBasalSettingResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private int result;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public TempBasalSettingResponsePacket() {
        super();
        setMsgType((byte) -88);
        getAapsLogger().debug(LTag.PUMPCOMM, "TempBasalSettingResponsePacket init");
        this.friendlyName = "PUMP_TEMP_BASAL_SETTING_RESPONSE";
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
            getAapsLogger().debug(LTag.PUMPCOMM, "TempBasalSettingResponsePacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        this.result = getByteToInt(byteBufferPrefixDecode);
        int byteToInt = getByteToInt(byteBufferPrefixDecode);
        if (!isSuccSettingResponseResult(this.result)) {
            getLenomedPump().setResultErrorCode(this.result);
            setFailed(true);
            getLenomedPump().setOtpNumber(1);
            getAapsLogger().error(LTag.PUMPCOMM, "Temp basal setting failed, result: " + this.result);
            return;
        }
        getLenomedPump().setOtpNumber(1);
        getAapsLogger().debug(LTag.PUMPCOMM, "Result --> " + this.result);
        getAapsLogger().debug(LTag.PUMPCOMM, "Default --> " + byteToInt);
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}