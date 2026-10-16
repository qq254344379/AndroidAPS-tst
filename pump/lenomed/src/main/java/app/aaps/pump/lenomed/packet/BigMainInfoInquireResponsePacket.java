package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.sharedPreferences.SP;
import app.aaps.pump.lenomed.LenomedPump;
import app.aaps.pump.lenomed.R;
import app.aaps.pump.lenomed.pumplog.PumpLogUtil;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.joda.time.DateTime;

/* JADX INFO: compiled from: BigMainInfoInquireResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u0012\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u0010\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u001bH\u0016R\u001e\u0010\u0006\u001a\u00020\u00078\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u001e\u0010\f\u001a\u00020\r8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u000e\u0010\u000f\"\u0004\b\u0010\u0010\u0011R\u001e\u0010\u0012\u001a\u00020\u00138\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0014\u0010\u0015\"\u0004\b\u0016\u0010\u0017R\u0014\u0010\u001c\u001a\u00020\u001dX\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001f¨\u0006 "}, d2 = {"Lapp/aaps/pump/lenomed/packet/BigMainInfoInquireResponsePacket;", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "injector", "Ldagger/android/HasAndroidInjector;", "<init>", "(Ldagger/android/HasAndroidInjector;)V", "lenomedPump", "Lapp/aaps/pump/lenomed/LenomedPump;", "getLenomedPump", "()Lapp/aaps/pump/lenomed/LenomedPump;", "setLenomedPump", "(Lapp/aaps/pump/lenomed/LenomedPump;)V", "sp", "Lapp/aaps/core/interfaces/sharedPreferences/SP;", "getSp", "()Lapp/aaps/core/interfaces/sharedPreferences/SP;", "setSp", "(Lapp/aaps/core/interfaces/sharedPreferences/SP;)V", "rh", "Lapp/aaps/core/interfaces/resources/ResourceHelper;", "getRh", "()Lapp/aaps/core/interfaces/resources/ResourceHelper;", "setRh", "(Lapp/aaps/core/interfaces/resources/ResourceHelper;)V", "handleMessage", "", "data", "", "friendlyName", "", "getFriendlyName", "()Ljava/lang/String;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class BigMainInfoInquireResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    @Inject
    public ResourceHelper rh;

    @Inject
    public SP sp;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BigMainInfoInquireResponsePacket() {
        super();
        setMsgType((byte) -77);
        getAapsLogger().debug(LTag.PUMPCOMM, "BigMainInfoInquireResponsePacket init");
        this.friendlyName = "PUMP_BIG_MAIN_INFO_INQUIRE_RESPONSE";
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

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
        if (LenomedPacket.INSTANCE.defect(data) != 0) {
            getAapsLogger().debug(LTag.PUMPCOMM, "BigMainInfoInquireResponsePacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        if (!isSuccInquireResponseResult(getByteToInt(byteBufferPrefixDecode))) {
            setFailed(true);
            return;
        }
        getLenomedPump().setSystemRemainInsulin(Math.floor(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d));
        getLenomedPump().setSystemRemainBattery(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSystemBasePattern(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSystemTbStatus(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSystemInjectionMealStatus(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSystemInjectionSnackStatus(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSystemInjectionSquareStatue(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSystemInjectionDualStatus(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setBasePauseStatus(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setYear(getByteToInt(byteBufferPrefixDecode) + 2000);
        getLenomedPump().setMonth(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setDay(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setHour(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setMinute(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSecond(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setPumpLastLogNum(getShortToInt(byteBufferPrefixDecode));
        getLenomedPump().setPumpWrappingCount(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSpeed(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setTbStatus(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setTbTime(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setTbElapsedTime(getShortToInt(byteBufferPrefixDecode));
        getLenomedPump().setBaseStatus(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setBaseHour(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setBaseAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseInjAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setMealKind(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setMealStartTime(getIntToInt(byteBufferPrefixDecode));
        getLenomedPump().setMealStatus(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setMealAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setMealInjAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setMealSpeed(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSnackStatus(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSnackAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setSnackInjAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setSnackSpeed(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSquareStatus(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setSquareTime(getShortToInt(byteBufferPrefixDecode));
        getLenomedPump().setSquareInjTime(getShortToInt(byteBufferPrefixDecode));
        getLenomedPump().setSquareAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setSquareInjAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setDualStatus(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setDualAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setDualInjAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setDualSquareTime(getShortToInt(byteBufferPrefixDecode));
        getLenomedPump().setDualInjSquareTime(getShortToInt(byteBufferPrefixDecode));
        getLenomedPump().setDualSquareAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setDualInjSquareAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setRecentKind1(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setRecentTime1(getIntToInt(byteBufferPrefixDecode));
        getLenomedPump().setRecentAmount1(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setRecentKind2(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setRecentTime2(getIntToInt(byteBufferPrefixDecode));
        getLenomedPump().setRecentAmount2(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setTodayBaseAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setTodayMealAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setTodaySnackAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setMorningHour(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setMorningAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setLunchHour(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setLunchAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setDinnerHour(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setDinnerAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setCurrentBasePattern(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setCurrentBaseHour(getByteToInt(byteBufferPrefixDecode));
        getLenomedPump().setCurrentBaseTbBeforeAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setCurrentBaseTbAfterAmount(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount1(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount2(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount3(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount4(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount5(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount6(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount7(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount8(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount9(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount10(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount11(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount12(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount13(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount14(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount15(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount16(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount17(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount18(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount19(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount20(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount21(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount22(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount23(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        getLenomedPump().setBaseAmount24(((double) getShortToInt(byteBufferPrefixDecode)) / 100.0d);
        DateTime dateTime = new DateTime(getLenomedPump().getYear(), getLenomedPump().getMonth(), getLenomedPump().getDay(), getLenomedPump().getHour(), getLenomedPump().getMinute(), getLenomedPump().getSecond());
        getLenomedPump().setPumpTime(dateTime.getMillis());
        getAapsLogger().debug(LTag.PUMPCOMM, "Pump time " + getDateUtil().dateAndTimeAndSecondsString(dateTime.getMillis()));
        LenomedPump lenomedPump = getLenomedPump();
        Double[][] dArr = new Double[4][];
        for (int i = 0; i < 4; i++) {
            Double[] dArr2 = new Double[24];
            for (int i2 = 0; i2 < 24; i2++) {
                dArr2[i2] = Double.valueOf(0.0d);
            }
            dArr[i] = dArr2;
        }
        lenomedPump.setPumpProfiles(dArr);
        Double[][] pumpProfiles = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles);
        pumpProfiles[getLenomedPump().getActiveProfile()][0] = Double.valueOf(getLenomedPump().getBaseAmount1());
        Double[][] pumpProfiles2 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles2);
        pumpProfiles2[getLenomedPump().getActiveProfile()][1] = Double.valueOf(getLenomedPump().getBaseAmount2());
        Double[][] pumpProfiles3 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles3);
        pumpProfiles3[getLenomedPump().getActiveProfile()][2] = Double.valueOf(getLenomedPump().getBaseAmount3());
        Double[][] pumpProfiles4 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles4);
        pumpProfiles4[getLenomedPump().getActiveProfile()][3] = Double.valueOf(getLenomedPump().getBaseAmount4());
        Double[][] pumpProfiles5 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles5);
        pumpProfiles5[getLenomedPump().getActiveProfile()][4] = Double.valueOf(getLenomedPump().getBaseAmount5());
        Double[][] pumpProfiles6 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles6);
        pumpProfiles6[getLenomedPump().getActiveProfile()][5] = Double.valueOf(getLenomedPump().getBaseAmount6());
        Double[][] pumpProfiles7 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles7);
        pumpProfiles7[getLenomedPump().getActiveProfile()][6] = Double.valueOf(getLenomedPump().getBaseAmount7());
        Double[][] pumpProfiles8 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles8);
        pumpProfiles8[getLenomedPump().getActiveProfile()][7] = Double.valueOf(getLenomedPump().getBaseAmount8());
        Double[][] pumpProfiles9 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles9);
        pumpProfiles9[getLenomedPump().getActiveProfile()][8] = Double.valueOf(getLenomedPump().getBaseAmount9());
        Double[][] pumpProfiles10 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles10);
        pumpProfiles10[getLenomedPump().getActiveProfile()][9] = Double.valueOf(getLenomedPump().getBaseAmount10());
        Double[][] pumpProfiles11 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles11);
        pumpProfiles11[getLenomedPump().getActiveProfile()][10] = Double.valueOf(getLenomedPump().getBaseAmount11());
        Double[][] pumpProfiles12 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles12);
        pumpProfiles12[getLenomedPump().getActiveProfile()][11] = Double.valueOf(getLenomedPump().getBaseAmount12());
        Double[][] pumpProfiles13 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles13);
        pumpProfiles13[getLenomedPump().getActiveProfile()][12] = Double.valueOf(getLenomedPump().getBaseAmount13());
        Double[][] pumpProfiles14 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles14);
        pumpProfiles14[getLenomedPump().getActiveProfile()][13] = Double.valueOf(getLenomedPump().getBaseAmount14());
        Double[][] pumpProfiles15 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles15);
        pumpProfiles15[getLenomedPump().getActiveProfile()][14] = Double.valueOf(getLenomedPump().getBaseAmount15());
        Double[][] pumpProfiles16 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles16);
        pumpProfiles16[getLenomedPump().getActiveProfile()][15] = Double.valueOf(getLenomedPump().getBaseAmount16());
        Double[][] pumpProfiles17 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles17);
        pumpProfiles17[getLenomedPump().getActiveProfile()][16] = Double.valueOf(getLenomedPump().getBaseAmount17());
        Double[][] pumpProfiles18 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles18);
        pumpProfiles18[getLenomedPump().getActiveProfile()][17] = Double.valueOf(getLenomedPump().getBaseAmount18());
        Double[][] pumpProfiles19 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles19);
        pumpProfiles19[getLenomedPump().getActiveProfile()][18] = Double.valueOf(getLenomedPump().getBaseAmount19());
        Double[][] pumpProfiles20 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles20);
        pumpProfiles20[getLenomedPump().getActiveProfile()][19] = Double.valueOf(getLenomedPump().getBaseAmount20());
        Double[][] pumpProfiles21 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles21);
        pumpProfiles21[getLenomedPump().getActiveProfile()][20] = Double.valueOf(getLenomedPump().getBaseAmount21());
        Double[][] pumpProfiles22 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles22);
        pumpProfiles22[getLenomedPump().getActiveProfile()][21] = Double.valueOf(getLenomedPump().getBaseAmount22());
        Double[][] pumpProfiles23 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles23);
        pumpProfiles23[getLenomedPump().getActiveProfile()][22] = Double.valueOf(getLenomedPump().getBaseAmount23());
        Double[][] pumpProfiles24 = getLenomedPump().getPumpProfiles();
        Intrinsics.checkNotNull(pumpProfiles24);
        pumpProfiles24[getLenomedPump().getActiveProfile()][23] = Double.valueOf(getLenomedPump().getBaseAmount24());
        getLenomedPump().setPumpVersionGe2_63(PumpLogUtil.INSTANCE.isPumpVersionGe(getSp().getString(getRh().gs(R.string.pumpversion), ""), 2, 63));
        getLenomedPump().setPumpVersionGe3_53(PumpLogUtil.INSTANCE.isPumpVersionGe(getSp().getString(getRh().gs(R.string.pumpversion), ""), 3, 53));
        getAapsLogger().debug(LTag.PUMPCOMM, "result > " + getLenomedPump().getResult());
        getAapsLogger().debug(LTag.PUMPCOMM, "systemRemainInsulin > " + getLenomedPump().getSystemRemainInsulin());
        getAapsLogger().debug(LTag.PUMPCOMM, "systemRemainBattery > " + getLenomedPump().getSystemRemainBattery());
        getAapsLogger().debug(LTag.PUMPCOMM, "systemBasePattern > " + getLenomedPump().getSystemBasePattern());
        getAapsLogger().debug(LTag.PUMPCOMM, "systemTbStatus > " + getLenomedPump().getSystemTbStatus());
        getAapsLogger().debug(LTag.PUMPCOMM, "systemInjectionMealStatus > " + getLenomedPump().getSystemInjectionMealStatus());
        getAapsLogger().debug(LTag.PUMPCOMM, "systemInjectionSnackStatus > " + getLenomedPump().getSystemInjectionSnackStatus());
        getAapsLogger().debug(LTag.PUMPCOMM, "systemInjectionSquareStatue > " + getLenomedPump().getSystemInjectionSquareStatue());
        getAapsLogger().debug(LTag.PUMPCOMM, "systemInjectionDualStatus > " + getLenomedPump().getSystemInjectionDualStatus());
        getAapsLogger().debug(LTag.PUMPCOMM, "basePauseStatus > " + getLenomedPump().getBasePauseStatus());
        getAapsLogger().debug(LTag.PUMPCOMM, "year > " + getLenomedPump().getYear());
        getAapsLogger().debug(LTag.PUMPCOMM, "month > " + getLenomedPump().getMonth());
        getAapsLogger().debug(LTag.PUMPCOMM, "day > " + getLenomedPump().getDay());
        getAapsLogger().debug(LTag.PUMPCOMM, "hour > " + getLenomedPump().getHour());
        getAapsLogger().debug(LTag.PUMPCOMM, "minute > " + getLenomedPump().getMinute());
        getAapsLogger().debug(LTag.PUMPCOMM, "second > " + getLenomedPump().getSecond());
        getAapsLogger().debug(LTag.PUMPCOMM, "country > " + getLenomedPump().getCountry());
        getAapsLogger().debug(LTag.PUMPCOMM, "productType > " + getLenomedPump().getProductType());
        getAapsLogger().debug(LTag.PUMPCOMM, "makeYear > " + getLenomedPump().getMakeYear());
        getAapsLogger().debug(LTag.PUMPCOMM, "makeMonth > " + getLenomedPump().getMakeMonth());
        getAapsLogger().debug(LTag.PUMPCOMM, "makeDay > " + getLenomedPump().getMakeDay());
        getAapsLogger().debug(LTag.PUMPCOMM, "lotNo > " + getLenomedPump().getLotNo());
        getAapsLogger().debug(LTag.PUMPCOMM, "serialNo > " + getLenomedPump().getSerialNo());
        getAapsLogger().debug(LTag.PUMPCOMM, "majorVersion > " + getLenomedPump().getMajorVersion());
        getAapsLogger().debug(LTag.PUMPCOMM, "minorVersion > " + getLenomedPump().getMinorVersion());
        getAapsLogger().debug(LTag.PUMPCOMM, "lastNum  > " + getLenomedPump().getPumpLastLogNum());
        getAapsLogger().debug(LTag.PUMPCOMM, "wrappingCount > " + getLenomedPump().getPumpWrappingCount());
        getAapsLogger().debug(LTag.PUMPCOMM, "speed > " + getLenomedPump().getSpeed());
        getAapsLogger().debug(LTag.PUMPCOMM, "tbStatus > " + getLenomedPump().getTbStatus());
        getAapsLogger().debug(LTag.PUMPCOMM, "tbTime> " + getLenomedPump().getTbTime());
        getAapsLogger().debug(LTag.PUMPCOMM, "tbInjectRateRatio > " + getLenomedPump().getTbInjectRateRatio());
        getAapsLogger().debug(LTag.PUMPCOMM, "tbElapsedTime > " + getLenomedPump().getTbElapsedTime());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseStatus > " + getLenomedPump().getBaseStatus());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseHour > " + getLenomedPump().getBaseHour());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount > " + getLenomedPump().getBaseAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseInjAmount > " + getLenomedPump().getBaseInjAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "mealKind > " + getLenomedPump().getMealKind());
        getAapsLogger().debug(LTag.PUMPCOMM, "mealStartTime > " + getLenomedPump().getMealStartTime());
        getAapsLogger().debug(LTag.PUMPCOMM, "mealStatus > " + getLenomedPump().getMealStatus());
        getAapsLogger().debug(LTag.PUMPCOMM, "mealAmount > " + getLenomedPump().getMealAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "mealInjAmount > " + getLenomedPump().getMealInjAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "mealSpeed > " + getLenomedPump().getMealSpeed());
        getAapsLogger().debug(LTag.PUMPCOMM, "snackStatus > " + getLenomedPump().getSnackStatus());
        getAapsLogger().debug(LTag.PUMPCOMM, "snackAmount > " + getLenomedPump().getSnackAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "snackInjAmount > " + getLenomedPump().getSnackInjAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "snackSpeed > " + getLenomedPump().getSnackSpeed());
        getAapsLogger().debug(LTag.PUMPCOMM, "squareStatus > " + getLenomedPump().getSquareStatus());
        getAapsLogger().debug(LTag.PUMPCOMM, "squareTime > " + getLenomedPump().getSquareTime());
        getAapsLogger().debug(LTag.PUMPCOMM, "squareInjTime > " + getLenomedPump().getSquareInjTime());
        getAapsLogger().debug(LTag.PUMPCOMM, "squareAmount > " + getLenomedPump().getSquareAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "squareInjAmount > " + getLenomedPump().getSquareInjAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "dualStatus > " + getLenomedPump().getDualStatus());
        getAapsLogger().debug(LTag.PUMPCOMM, "dualAmount > " + getLenomedPump().getDualAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "dualInjAmount > " + getLenomedPump().getDualInjAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "dualSquareTime > " + getLenomedPump().getDualSquareTime());
        getAapsLogger().debug(LTag.PUMPCOMM, "dualInjSquareTime > " + getLenomedPump().getDualInjSquareTime());
        getAapsLogger().debug(LTag.PUMPCOMM, "dualSquareAmount > " + getLenomedPump().getDualSquareAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "dualInjSquareAmount> " + getLenomedPump().getDualInjSquareAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "recentKind1  > " + getLenomedPump().getRecentKind1());
        getAapsLogger().debug(LTag.PUMPCOMM, "recentTime1  > " + getLenomedPump().getRecentTime1());
        getAapsLogger().debug(LTag.PUMPCOMM, "recentAmount1 > " + getLenomedPump().getRecentAmount1());
        getAapsLogger().debug(LTag.PUMPCOMM, "recentKind2  \t> " + getLenomedPump().getRecentKind2());
        getAapsLogger().debug(LTag.PUMPCOMM, "recentTime2  \t> " + getLenomedPump().getRecentTime2());
        getAapsLogger().debug(LTag.PUMPCOMM, "recentAmount2 > " + getLenomedPump().getRecentAmount2());
        getAapsLogger().debug(LTag.PUMPCOMM, "todayBaseAmount > " + getLenomedPump().getTodayBaseAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "todayMealAmount > " + getLenomedPump().getTodayMealAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "todaySnackAmount > " + getLenomedPump().getTodaySnackAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "morningHour  > " + getLenomedPump().getMorningHour());
        getAapsLogger().debug(LTag.PUMPCOMM, "morningAmount > " + getLenomedPump().getMorningAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "lunchHour  > " + getLenomedPump().getLunchHour());
        getAapsLogger().debug(LTag.PUMPCOMM, "lunchAmount > " + getLenomedPump().getLunchAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "dinnerHour  > " + getLenomedPump().getDinnerHour());
        getAapsLogger().debug(LTag.PUMPCOMM, "dinnerAmount > " + getLenomedPump().getDinnerAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "currentBasePattern > " + getLenomedPump().getCurrentBasePattern());
        getAapsLogger().debug(LTag.PUMPCOMM, "currentBaseHour  > " + getLenomedPump().getCurrentBaseHour());
        getAapsLogger().debug(LTag.PUMPCOMM, "currentBaseTbBeforeAmount > " + getLenomedPump().getCurrentBaseTbBeforeAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "currentBaseTbAfterAmount > " + getLenomedPump().getCurrentBaseTbAfterAmount());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount1  > " + getLenomedPump().getBaseAmount1());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount2  > " + getLenomedPump().getBaseAmount2());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount3  > " + getLenomedPump().getBaseAmount3());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount4  > " + getLenomedPump().getBaseAmount4());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount5  > " + getLenomedPump().getBaseAmount5());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount6  > " + getLenomedPump().getBaseAmount6());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount7  > " + getLenomedPump().getBaseAmount7());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount8  > " + getLenomedPump().getBaseAmount8());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount9  > " + getLenomedPump().getBaseAmount9());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount10 > " + getLenomedPump().getBaseAmount10());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount11 > " + getLenomedPump().getBaseAmount11());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount12 > " + getLenomedPump().getBaseAmount12());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount13 > " + getLenomedPump().getBaseAmount13());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount14 > " + getLenomedPump().getBaseAmount14());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount15 > " + getLenomedPump().getBaseAmount15());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount16 > " + getLenomedPump().getBaseAmount16());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount17 > " + getLenomedPump().getBaseAmount17());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount18 > " + getLenomedPump().getBaseAmount18());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount19 > " + getLenomedPump().getBaseAmount19());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount20 > " + getLenomedPump().getBaseAmount20());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount21 > " + getLenomedPump().getBaseAmount21());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount22 > " + getLenomedPump().getBaseAmount22());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount23 > " + getLenomedPump().getBaseAmount23());
        getAapsLogger().debug(LTag.PUMPCOMM, "baseAmount24 > " + getLenomedPump().getBaseAmount24());
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}