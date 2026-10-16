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

/* JADX INFO: compiled from: InjectionSnackSettingResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u0012\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u0010\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fH\u0016R\u001e\u0010\u0006\u001a\u00020\u00078\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u0014\u0010\u0010\u001a\u00020\u0011X\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013¨\u0006\u0014"}, d2 = {"Lapp/aaps/pump/lenomed/packet/InjectionSnackSettingResponsePacket;", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "injector", "Ldagger/android/HasAndroidInjector;", "<init>", "(Ldagger/android/HasAndroidInjector;)V", "lenomedPump", "Lapp/aaps/pump/lenomed/LenomedPump;", "getLenomedPump", "()Lapp/aaps/pump/lenomed/LenomedPump;", "setLenomedPump", "(Lapp/aaps/pump/lenomed/LenomedPump;)V", "handleMessage", "", "data", "", "friendlyName", "", "getFriendlyName", "()Ljava/lang/String;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class InjectionSnackSettingResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public InjectionSnackSettingResponsePacket() {
        super();
        setMsgType((byte) -78);
        getAapsLogger().debug(LTag.PUMPCOMM, "InjectionSnackSettingResponsePacket init");
        this.friendlyName = "PUMP_INJECTION_SNACK_SETTING_RESPONSE";
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
        getAapsLogger().debug(LTag.PUMPCOMM, "Raw bolus response: " + ArraysKt.joinToString$default(data, (CharSequence) null, (CharSequence) null, (CharSequence) null, 0, (CharSequence) null, new Function1() { // from class: app.aaps.pump.lenomed.packet.InjectionSnackSettingResponsePacket$$ExternalSyntheticLambda0
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return InjectionSnackSettingResponsePacket.handleMessage$lambda$0(((Byte) obj).byteValue());
            }
        }, 31, (Object) null));
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        try {
            int byteToInt = getByteToInt(byteBufferPrefixDecode);
            switch (byteToInt) {
                case 10:
                    int byteToInt2 = getByteToInt(byteBufferPrefixDecode);
                    getByteToInt(byteBufferPrefixDecode);
                    int byteToInt3 = getByteToInt(byteBufferPrefixDecode);
                    AAPSLogger aapsLogger = getAapsLogger();
                    LTag lTag = LTag.PUMPCOMM;
                    StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
                    String str = String.format("%02X", Arrays.copyOf(new Object[]{Integer.valueOf(byteToInt2)}, 1));
                    Intrinsics.checkNotNullExpressionValue(str, "format(...)");
                    aapsLogger.debug(lTag, "Bolus START (0x0A): switch=0x" + str + ", dose=" + byteToInt3 + "/10 U");
                    getLenomedPump().setBolusDone(false);
                    setFailed(false);
                    break;
                case 11:
                    getByteToInt(byteBufferPrefixDecode);
                    int byteToInt4 = getByteToInt(byteBufferPrefixDecode);
                    getAapsLogger().warn(LTag.PUMPCOMM, "Bolus FAILED (0x0B): error=" + byteToInt4 + ", limit=" + getByteToInt(byteBufferPrefixDecode));
                    getLenomedPump().setResultErrorCode(byteToInt4);
                    setFailed(true);
                    break;
                case 12:
                    getByteToInt(byteBufferPrefixDecode);
                    getByteToInt(byteBufferPrefixDecode);
                    int byteToInt5 = getByteToInt(byteBufferPrefixDecode);
                    int byteToInt6 = getByteToInt(byteBufferPrefixDecode);
                    int byteToInt7 = getByteToInt(byteBufferPrefixDecode);
                    double byteToInt8 = ((double) ((getByteToInt(byteBufferPrefixDecode) * 256) + getByteToInt(byteBufferPrefixDecode))) / 100.0d;
                    getAapsLogger().debug(LTag.PUMPCOMM, "Bolus COMPLETE (0x0C): actual=" + byteToInt8 + " U, time=" + byteToInt5 + ":" + byteToInt6 + ", block=" + byteToInt7);
                    getLenomedPump().setBolusingInjAmount(byteToInt8);
                    getLenomedPump().setBolusDone(true);
                    setFailed(false);
                    break;
                default:
                    AAPSLogger aapsLogger2 = getAapsLogger();
                    LTag lTag2 = LTag.PUMPCOMM;
                    StringCompanionObject stringCompanionObject2 = StringCompanionObject.INSTANCE;
                    String str2 = String.format("%02X", Arrays.copyOf(new Object[]{Integer.valueOf(byteToInt)}, 1));
                    Intrinsics.checkNotNullExpressionValue(str2, "format(...)");
                    aapsLogger2.warn(lTag2, "Unknown bolus state: 0x" + str2);
                    setFailed(true);
                    break;
            }
        } catch (Exception e) {
            getAapsLogger().error(LTag.PUMPCOMM, "Error parsing bolus response: " + e.getMessage(), e);
            setFailed(true);
        }
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}