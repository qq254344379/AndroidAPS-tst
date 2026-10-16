package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.StringCompanionObject;

/* JADX INFO: compiled from: TempBasalSettingPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0005\n\u0000\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0012\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B/\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0007\u0012\u0006\u0010\t\u001a\u00020\u0007¢\u0006\u0004\b\n\u0010\u000bJ\u0010\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u0007H\u0017R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0007X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0007X\u0082\u0004¢\u0006\u0002\n\u0000R\u001e\u0010\f\u001a\u00020\r8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u000e\u0010\u000f\"\u0004\b\u0010\u0010\u0011R\u0014\u0010\u0015\u001a\u00020\u0016X\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018¨\u0006\u0019"}, d2 = {"Lapp/aaps/pump/lenomed/packet/TempBasalSettingPacket;", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "injector", "Ldagger/android/HasAndroidInjector;", "mode", "", "startTimeMin", "", "endTimeMin", "value", "<init>", "(Ldagger/android/HasAndroidInjector;BIII)V", "lenomedPump", "Lapp/aaps/pump/lenomed/LenomedPump;", "getLenomedPump", "()Lapp/aaps/pump/lenomed/LenomedPump;", "setLenomedPump", "(Lapp/aaps/pump/lenomed/LenomedPump;)V", "encode", "", "msgSeq", "friendlyName", "", "getFriendlyName", "()Ljava/lang/String;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class TempBasalSettingPacket extends LenomedPacket {
    private final int endTimeMin;
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private final byte mode;
    private final int startTimeMin;
    private final int value;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public TempBasalSettingPacket( byte b, int i, int i2, int i3) {
        super();
        this.mode = b;
        this.startTimeMin = i;
        this.endTimeMin = i2;
        this.value = i3;
        setMsgType((byte) -88);
        getAapsLogger().debug(LTag.PUMPCOMM, "TempBasalSettingPacket Init");
        this.friendlyName = "PUMP_TEMP_BASAL_SETTING";
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
    public byte[] encode(int msgSeq) {
        ByteBuffer byteBufferPrefixEncode$default = LenomedPacket.prefixEncode$default(this, getMsgType(), 7, (byte) 0, 4, null);
        byteBufferPrefixEncode$default.put(this.mode);
        byteBufferPrefixEncode$default.put((byte) (this.startTimeMin >> 8));
        byteBufferPrefixEncode$default.put((byte) this.startTimeMin);
        byteBufferPrefixEncode$default.put((byte) (this.endTimeMin >> 8));
        byteBufferPrefixEncode$default.put((byte) this.endTimeMin);
        int i = this.value / 100;
        int i2 = i / 100;
        int i3 = i % 100;
        byteBufferPrefixEncode$default.put((byte) i2);
        byteBufferPrefixEncode$default.put((byte) i3);
        getAapsLogger().debug(LTag.PUMPCOMM, "Mode --> " + ((int) this.mode) + " (0: cancel, 1: percent)");
        AAPSLogger aapsLogger = getAapsLogger();
        LTag lTag = LTag.PUMPCOMM;
        StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
        String str = String.format("0x%04X", Arrays.copyOf(new Object[]{Integer.valueOf(this.startTimeMin)}, 1));
        Intrinsics.checkNotNullExpressionValue(str, "format(...)");
        StringCompanionObject stringCompanionObject2 = StringCompanionObject.INSTANCE;
        String str2 = String.format("%02d:%02d", Arrays.copyOf(new Object[]{Integer.valueOf(this.startTimeMin / 60), Integer.valueOf(this.startTimeMin % 60)}, 2));
        Intrinsics.checkNotNullExpressionValue(str2, "format(...)");
        aapsLogger.debug(lTag, "StartTimeMin --> " + str + " (" + str2 + ")");
        AAPSLogger aapsLogger2 = getAapsLogger();
        LTag lTag2 = LTag.PUMPCOMM;
        StringCompanionObject stringCompanionObject3 = StringCompanionObject.INSTANCE;
        String str3 = String.format("0x%04X", Arrays.copyOf(new Object[]{Integer.valueOf(this.endTimeMin)}, 1));
        Intrinsics.checkNotNullExpressionValue(str3, "format(...)");
        StringCompanionObject stringCompanionObject4 = StringCompanionObject.INSTANCE;
        String str4 = String.format("%02d:%02d", Arrays.copyOf(new Object[]{Integer.valueOf(this.endTimeMin / 60), Integer.valueOf(this.endTimeMin % 60)}, 2));
        Intrinsics.checkNotNullExpressionValue(str4, "format(...)");
        aapsLogger2.debug(lTag2, "EndTimeMin --> " + str3 + " (" + str4 + ")");
        AAPSLogger aapsLogger3 = getAapsLogger();
        LTag lTag3 = LTag.PUMPCOMM;
        int i4 = this.value / 100;
        StringCompanionObject stringCompanionObject5 = StringCompanionObject.INSTANCE;
        String str5 = String.format("%02X", Arrays.copyOf(new Object[]{Integer.valueOf(i2)}, 1));
        Intrinsics.checkNotNullExpressionValue(str5, "format(...)");
        StringCompanionObject stringCompanionObject6 = StringCompanionObject.INSTANCE;
        String str6 = String.format("%02X", Arrays.copyOf(new Object[]{Integer.valueOf(i3)}, 1));
        Intrinsics.checkNotNullExpressionValue(str6, "format(...)");
        aapsLogger3.debug(lTag3, "Value --> " + i4 + "% (valueIntHigh=0x" + str5 + ", valueIntLow=0x" + str6 + ")");
        return suffixEncode(byteBufferPrefixEncode$default);
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}