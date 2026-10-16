package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: AppCancelSettingResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class AppCancelSettingResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private int result;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public AppCancelSettingResponsePacket() {
        super();
        setMsgType((byte) -87);
        getAapsLogger().debug(LTag.PUMPCOMM, "AppCancelSettingResPacket init ");
        this.friendlyName = "PUMP_APP_CANCEL_SETTING_RESPONSE";
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
            getAapsLogger().debug(LTag.PUMPCOMM, "AppCancelSettingResponsePacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        int byteToInt = getByteToInt(LenomedPacket.INSTANCE.prefixDecode(data));
        this.result = byteToInt;
        if (!isSuccSettingResponseResult(byteToInt)) {
            getLenomedPump().setResultErrorCode(this.result);
            setFailed(true);
        } else {
            getAapsLogger().debug(LTag.PUMPCOMM, "Result --> " + this.result);
        }
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}