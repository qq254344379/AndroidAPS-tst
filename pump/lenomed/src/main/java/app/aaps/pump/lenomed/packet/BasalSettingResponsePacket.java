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

/* JADX INFO: compiled from: BasalSettingResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
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
        getAapsLogger().debug(LTag.PUMPCOMM, "Raw data: " + LenomedSupportKt.bytesToHexString(data, ", "));
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