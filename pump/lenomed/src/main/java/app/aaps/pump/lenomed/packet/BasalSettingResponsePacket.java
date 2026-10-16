package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.StringCompanionObject;

/* JADX INFO: compiled from: BasalSettingResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u0012\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u0010\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fH\u0016R\u001e\u0010\u0006\u001a\u00020\u00078\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u0014\u0010\u0010\u001a\u00020\u0011X\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013¨\u0006\u0014"}, d2 = {"Lapp/aaps/pump/lenomed/packet/BasalSettingResponsePacket;", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "injector", "Ldagger/android/HasAndroidInjector;", "<init>", "(Ldagger/android/HasAndroidInjector;)V", "lenomedPump", "Lapp/aaps/pump/lenomed/LenomedPump;", "getLenomedPump", "()Lapp/aaps/pump/lenomed/LenomedPump;", "setLenomedPump", "(Lapp/aaps/pump/lenomed/LenomedPump;)V", "handleMessage", "", "data", "", "friendlyName", "", "getFriendlyName", "()Ljava/lang/String;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class BasalSettingResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BasalSettingResponsePacket() {
        super();
        setMsgType((byte) -73);
        getAapsLogger().debug(LTag.PUMPCOMM, "BasalSettingResponsePacket init");
        this.friendlyName = "PUMP_BASAL_SETTING_RESPONSE";
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

    /* JADX INFO: Access modifiers changed from: private */
    public static final CharSequence handleMessage$lambda$0(byte b) {
        StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
        String str = String.format("%02X", Arrays.copyOf(new Object[]{Byte.valueOf(b)}, 1));
        Intrinsics.checkNotNullExpressionValue(str, "format(...)");
        return str;
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
        getAapsLogger().debug(LTag.PUMPCOMM, "Raw data: " + ArraysKt.joinToString$default(data, (CharSequence) null, (CharSequence) null, (CharSequence) null, 0, (CharSequence) null, new Function1() { // from class: app.aaps.pump.lenomed.packet.BasalSettingResponsePacket$$ExternalSyntheticLambda0
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return BasalSettingResponsePacket.handleMessage$lambda$0(((Byte) obj).byteValue());
            }
        }, 31, (Object) null));
        if (data.length < 12) {
            getAapsLogger().error(LTag.PUMPBTCOMM, "Response too short: " + data.length + " bytes");
            setFailed(true);
            return;
        }
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        try {
            int byteToInt = getByteToInt(byteBufferPrefixDecode);
            getLenomedPump().setInfusionSwitchStatus(byteToInt);
            getLenomedPump().setInfusionEnabled(byteToInt == 165);
            AAPSLogger aapsLogger = getAapsLogger();
            LTag lTag = LTag.PUMPCOMM;
            StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
            String str = String.format("%02X", Arrays.copyOf(new Object[]{Integer.valueOf(byteToInt)}, 1));
            Intrinsics.checkNotNullExpressionValue(str, "format(...)");
            aapsLogger.debug(lTag, "Infusion switch: 0x" + str);
            if (byteToInt == 165) {
                getAapsLogger().warn(LTag.PUMPBTCOMM, "Basal setting FAILED - Infusion switch is ON (A5)! Must be OFF (B5)");
                setFailed(true);
                return;
            }
            int byteToInt2 = getByteToInt(byteBufferPrefixDecode);
            boolean z = byteToInt2 == 17 || byteToInt2 == 33;
            AAPSLogger aapsLogger2 = getAapsLogger();
            LTag lTag2 = LTag.PUMPCOMM;
            StringCompanionObject stringCompanionObject2 = StringCompanionObject.INSTANCE;
            String str2 = String.format("%02X", Arrays.copyOf(new Object[]{Integer.valueOf(byteToInt2)}, 1));
            Intrinsics.checkNotNullExpressionValue(str2, "format(...)");
            String str3 = "Upper";
            aapsLogger2.debug(lTag2, "Mode flag: 0x" + str2 + " → " + (z ? "Upper" : "Lower") + " half");
            for (int i = 0; i < 12; i++) {
                getAapsLogger().debug(LTag.PUMPCOMM, (z ? "Upper" : "Lower") + " segment " + i + " → " + (((double) getByteToInt(byteBufferPrefixDecode)) / 10.0d) + " U/h");
            }
            AAPSLogger aapsLogger3 = getAapsLogger();
            LTag lTag3 = LTag.PUMPCOMM;
            if (!z) {
                str3 = "Lower";
            }
            aapsLogger3.debug(lTag3, "Basal setting SUCCESS (" + str3 + " half) - Switch is OFF");
            getLenomedPump().setOtpNumber(1);
            setFailed(false);
        } catch (Exception e) {
            getAapsLogger().error(LTag.PUMPCOMM, "Error parsing basal response: " + e.getMessage(), e);
            setFailed(true);
        }
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}