package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: SoundInquireResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class SoundInquireResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public SoundInquireResponsePacket() {
        super();
        setMsgType((byte) -115);
        getAapsLogger().debug(LTag.PUMPCOMM, "SoundInquireResponsePacket init");
        this.friendlyName = "PUMP_SOUND_INQUIRE_RESPONSE";
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
            getAapsLogger().debug(LTag.PUMPCOMM, "SoundInquireResponsePacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        if (!isSuccInquireResponseResult(getByteToInt(byteBufferPrefixDecode))) {
            setFailed(true);
            return;
        }
        getLenomedPump().setBeepAndAlarm(getByteToInt(byteBufferPrefixDecode) - 1);
        getLenomedPump().setAlarmIntensity(getByteToInt(byteBufferPrefixDecode) - 1);
        getAapsLogger().debug(LTag.PUMPCOMM, "Result --> " + getLenomedPump().getResult());
        getAapsLogger().debug(LTag.PUMPCOMM, "beepAndAlarm --> " + getLenomedPump().getBeepAndAlarm());
        getAapsLogger().debug(LTag.PUMPCOMM, "alarmIntensity --> " + getLenomedPump().getAlarmIntensity());
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}