package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.pump.lenomed.LenomedSupportKt;
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
        getAapsLogger().debug(LTag.PUMPCOMM, "Raw bolus response: " + LenomedSupportKt.bytesToHexString(data, ", "));
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