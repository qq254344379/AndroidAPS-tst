package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.pump.lenomed.LenomedSupportKt;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.sharedPreferences.SP;
import app.aaps.pump.lenomed.compat.BleEncryption;
import app.aaps.pump.lenomed.LenomedPump;
import app.aaps.pump.lenomed.R;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.StringCompanionObject;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

/* JADX INFO: compiled from: BigAPSMainInfoInquireResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class BigAPSMainInfoInquireResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    @Inject
    public ResourceHelper rh;

    @Inject
    public SP sp;

    private final int parseBCD(int i) {
        return (((i >> 4) & 15) * 10) + (i & 15);
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BigAPSMainInfoInquireResponsePacket() {
        super();
        setMsgType(BleEncryption.DANAR_PACKET__TYPE_COMMAND);
        getAapsLogger().debug(LTag.PUMPCOMM, "BigAPSMainInfoInquireResponsePacket init");
        this.friendlyName = "PUMP_BIG_APS_MAIN_INFO_INQUIRE_RESPONSE";
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

    public final SP getSp() {
        SP sp = this.sp;
        if (sp != null) {
            return sp;
        }
        Intrinsics.throwUninitializedPropertyAccessException("sp");
        return null;
    }

    public final void setSp(SP sp) {
        Intrinsics.checkNotNullParameter(sp, "<set-?>");
        this.sp = sp;
    }

    public final ResourceHelper getRh() {
        ResourceHelper resourceHelper = this.rh;
        if (resourceHelper != null) {
            return resourceHelper;
        }
        Intrinsics.throwUninitializedPropertyAccessException("rh");
        return null;
    }

    public final void setRh(ResourceHelper resourceHelper) {
        Intrinsics.checkNotNullParameter(resourceHelper, "<set-?>");
        this.rh = resourceHelper;
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
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        try {
            getLenomedPump().setSystemRemainBattery(getByteToInt(byteBufferPrefixDecode));
            getAapsLogger().debug(LTag.PUMPCOMM, "systemRemainBattery: " + getLenomedPump().getSystemRemainBattery() + "%");
            getLenomedPump().setSystemRemainInsulin(((double) ((getByteToInt(byteBufferPrefixDecode) * 256) + getByteToInt(byteBufferPrefixDecode))) / 100.0d);
            getAapsLogger().debug(LTag.PUMPCOMM, "systemRemainInsulin: " + getLenomedPump().getSystemRemainInsulin() + " U");
            getLenomedPump().setInfusionSwitchStatus(getByteToInt(byteBufferPrefixDecode));
            getLenomedPump().setInfusionEnabled(getLenomedPump().getInfusionSwitchStatus() == 165);
            AAPSLogger aapsLogger = getAapsLogger();
            LTag lTag = LTag.PUMPCOMM;
            StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
            String str = String.format("%02X", Arrays.copyOf(new Object[]{Integer.valueOf(getLenomedPump().getInfusionSwitchStatus())}, 1));
            Intrinsics.checkNotNullExpressionValue(str, "format(...)");
            aapsLogger.debug(lTag, "infusionSwitchStatus: 0x" + str + " → " + (getLenomedPump().getInfusionEnabled() ? "开" : "关"));
            getLenomedPump().setCurrentBasePattern(getByteToInt(byteBufferPrefixDecode));
            getAapsLogger().debug(LTag.PUMPCOMM, "currentBasePattern: " + getLenomedPump().getCurrentBasePattern() + " (1=基础率1, 2=基础率2, 3=临时基础率)");
            getLenomedPump().setBaseAmount(((double) getByteToInt(byteBufferPrefixDecode)) / 10.0d);
            getAapsLogger().debug(LTag.PUMPCOMM, "current segment basal rate: " + getLenomedPump().getBaseAmount() + " U/h");
            getLenomedPump().setBaseInjAmount(((double) ((getByteToInt(byteBufferPrefixDecode) * 256) + getByteToInt(byteBufferPrefixDecode))) / 100.0d);
            getAapsLogger().debug(LTag.PUMPCOMM, "current segment injected basal: " + getLenomedPump().getBaseInjAmount() + " U");
            getLenomedPump().setTodayBaseAmount(((double) ((getByteToInt(byteBufferPrefixDecode) * 256) + getByteToInt(byteBufferPrefixDecode))) / 100.0d);
            getAapsLogger().debug(LTag.PUMPCOMM, "todayBaseAmount: " + getLenomedPump().getTodayBaseAmount() + " U");
            getLenomedPump().setTodayMealAmount(((double) ((getByteToInt(byteBufferPrefixDecode) * 256) + getByteToInt(byteBufferPrefixDecode))) / 100.0d);
            getAapsLogger().debug(LTag.PUMPCOMM, "todayMealAmount (daily bolus): " + getLenomedPump().getTodayMealAmount() + " U");
            getLenomedPump().setDailyTotalUnits(((double) ((getByteToInt(byteBufferPrefixDecode) * 256) + getByteToInt(byteBufferPrefixDecode))) / 100.0d);
            getAapsLogger().debug(LTag.PUMPCOMM, "dailyTotalUnits: " + getLenomedPump().getDailyTotalUnits() + " U");
            getLenomedPump().setYear(parseBCD(getByteToInt(byteBufferPrefixDecode)));
            getLenomedPump().setMonth(parseBCD(getByteToInt(byteBufferPrefixDecode)));
            getLenomedPump().setDay(parseBCD(getByteToInt(byteBufferPrefixDecode)));
            getLenomedPump().setHour(parseBCD(getByteToInt(byteBufferPrefixDecode)));
            getLenomedPump().setMinute(parseBCD(getByteToInt(byteBufferPrefixDecode)));
            getLenomedPump().setSecond(0);
            getLenomedPump().setPumpTime(new DateTime(getLenomedPump().getYear() + 2000, getLenomedPump().getMonth(), getLenomedPump().getDay(), getLenomedPump().getHour(), getLenomedPump().getMinute(), 0).getMillis());
            AAPSLogger aapsLogger2 = getAapsLogger();
            LTag lTag2 = LTag.PUMPCOMM;
            int year = getLenomedPump().getYear() + 2000;
            StringCompanionObject stringCompanionObject2 = StringCompanionObject.INSTANCE;
            String str2 = String.format("%02d", Arrays.copyOf(new Object[]{Integer.valueOf(getLenomedPump().getMonth())}, 1));
            Intrinsics.checkNotNullExpressionValue(str2, "format(...)");
            StringCompanionObject stringCompanionObject3 = StringCompanionObject.INSTANCE;
            String str3 = String.format("%02d", Arrays.copyOf(new Object[]{Integer.valueOf(getLenomedPump().getDay())}, 1));
            Intrinsics.checkNotNullExpressionValue(str3, "format(...)");
            StringCompanionObject stringCompanionObject4 = StringCompanionObject.INSTANCE;
            String str4 = String.format("%02d", Arrays.copyOf(new Object[]{Integer.valueOf(getLenomedPump().getHour())}, 1));
            Intrinsics.checkNotNullExpressionValue(str4, "format(...)");
            StringCompanionObject stringCompanionObject5 = StringCompanionObject.INSTANCE;
            String str5 = String.format("%02d", Arrays.copyOf(new Object[]{Integer.valueOf(getLenomedPump().getMinute())}, 1));
            Intrinsics.checkNotNullExpressionValue(str5, "format(...)");
            aapsLogger2.debug(lTag2, "pump time: " + year + "-" + str2 + "-" + str3 + StringUtils.SPACE + str4 + ":" + str5);
            getAapsLogger().debug(LTag.PUMPCOMM, "daily basal limit: " + (((double) ((getByteToInt(byteBufferPrefixDecode) * 256) + getByteToInt(byteBufferPrefixDecode))) / 10.0d) + " U");
            getAapsLogger().debug(LTag.PUMPCOMM, "bolus protection limit: " + (((double) getByteToInt(byteBufferPrefixDecode)) / 10.0d) + " U");
            getAapsLogger().debug(LTag.PUMPCOMM, "basal rate protection limit: " + (((double) getByteToInt(byteBufferPrefixDecode)) / 10.0d) + " U/h");
            getAapsLogger().debug(LTag.PUMPCOMM, "internal software version: " + getByteToInt(byteBufferPrefixDecode));
            getLenomedPump().setBlockStatus(getByteToInt(byteBufferPrefixDecode));
            getAapsLogger().debug(LTag.PUMPCOMM, "blockStatus: " + getLenomedPump().getBlockStatus() + " (0=正常, 1=阻塞)");
            getSp().putString(getRh().gs(R.string.pumpversion), getLenomedPump().getMajorVersion() + "." + getLenomedPump().getMinorVersion() + "." + getLenomedPump().getPatchVersion() + "." + getLenomedPump().getBuildVersion());
            setFailed(false);
        } catch (Exception e) {
            getAapsLogger().error(LTag.PUMPCOMM, "Error parsing 0xA1 packet: " + e.getMessage(), e);
            setFailed(true);
        }
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}