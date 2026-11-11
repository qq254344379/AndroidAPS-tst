package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedSupportKt;
import app.aaps.pump.lenomed.compat.BleEncryption;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.StringCompanionObject;
import kotlin.ranges.RangesKt;

/* JADX INFO: compiled from: TempBasalInquireResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class TempBasalInquireResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public TempBasalInquireResponsePacket() {
        super();
        setMsgType(BleEncryption.DANAR_PACKET__START_PACKET);
        getAapsLogger().debug(LTag.PUMPCOMM, "TempBasalInquireResponsePacket initialized");
        this.friendlyName = "TEMP_BASAL_INQUIRE_RESPONSE";
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
        double baseAmount;
        long j;
        int i;
        int i2;
        Intrinsics.checkNotNullParameter(data, "data");
        getAapsLogger().debug(LTag.PUMPCOMM, "Raw data: " + LenomedSupportKt.bytesToHexString(data, ", "));
        int iDefect = LenomedPacket.INSTANCE.defect(data);
        if (iDefect != 0) {
            getAapsLogger().error(LTag.PUMPCOMM, "Packet defect detected: " + iDefect);
            setFailed(true);
            return;
        }
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        try {
            if (byteBufferPrefixDecode.remaining() < 7) {
                getAapsLogger().error(LTag.PUMPCOMM, "Insufficient data length: " + byteBufferPrefixDecode.remaining() + ", expected at least 7 bytes");
                setFailed(true);
                return;
            }
            int byteToInt = getByteToInt(byteBufferPrefixDecode);
            if (byteToInt == 0 || byteToInt == 255) {
                getLenomedPump().setTbStatus(0);
                getLenomedPump().setTbTime(0);
                getLenomedPump().setTempBasalStart(0L);
                getLenomedPump().setTempBasalAbsoluteRate(0.0d);
                getLenomedPump().setTempBasalDuration(0L);
                getLenomedPump().setTempBasalPercent(0);
                getLenomedPump().setOtpNumber(1);
                getLenomedPump().setTempBasalInProgress(false);
                for (int i3 = 0; i3 < 6; i3++) {
                    getByteToInt(byteBufferPrefixDecode);
                }
                getAapsLogger().debug(LTag.PUMPCOMM, "No temporary basal rate set");
                setFailed(false);
                return;
            }
            if (byteToInt != 1 && byteToInt != 2) {
                getAapsLogger().error(LTag.PUMPCOMM, "Invalid type: " + byteToInt + ", only percent (0x01) or absolute (0x02) supported");
                setFailed(true);
                return;
            }
            int byteToInt2 = getByteToInt(byteBufferPrefixDecode);
            int byteToInt3 = getByteToInt(byteBufferPrefixDecode);
            int byteToInt4 = getByteToInt(byteBufferPrefixDecode);
            int byteToInt5 = getByteToInt(byteBufferPrefixDecode);
            int byteToInt6 = getByteToInt(byteBufferPrefixDecode);
            int byteToInt7 = getByteToInt(byteBufferPrefixDecode);
            int i4 = (byteToInt2 << 8) | byteToInt3;
            int i5 = (byteToInt4 << 8) | byteToInt5;
            if (i4 == 0 && i5 == 0) {
                getLenomedPump().setTbStatus(0);
                getLenomedPump().setTbTime(0);
                getLenomedPump().setTempBasalStart(0L);
                getLenomedPump().setTempBasalAbsoluteRate(0.0d);
                getLenomedPump().setTempBasalDuration(0L);
                getLenomedPump().setTempBasalPercent(0);
                getLenomedPump().setOtpNumber(1);
                getLenomedPump().setTempBasalInProgress(false);
                getAapsLogger().debug(LTag.PUMPCOMM, "No temporary basal rate set (startTimeMin and endTimeMin are 0)");
                setFailed(false);
                return;
            }
            StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
            String str = String.format("%02d:%02d", Arrays.copyOf(new Object[]{Integer.valueOf(i4 / 60), Integer.valueOf(i4 % 60)}, 2));
            Intrinsics.checkNotNullExpressionValue(str, "format(...)");
            StringCompanionObject stringCompanionObject2 = StringCompanionObject.INSTANCE;
            String str2 = String.format("%02d:%02d", Arrays.copyOf(new Object[]{Integer.valueOf(i5 / 60), Integer.valueOf(i5 % 60)}, 2));
            Intrinsics.checkNotNullExpressionValue(str2, "format(...)");
            long jNow = getDateUtil().now();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(jNow);
            calendar.set(11, i4 / 60);
            calendar.set(12, i4 % 60);
            calendar.set(13, 0);
            calendar.set(14, 0);
            long timeInMillis = calendar.getTimeInMillis();
            if (timeInMillis > jNow) {
                calendar.add(5, -1);
                timeInMillis = calendar.getTimeInMillis();
            }
            long j2 = timeInMillis;
            calendar.setTimeInMillis(j2);
            calendar.set(11, i5 / 60);
            calendar.set(12, i5 % 60);
            calendar.set(13, 0);
            calendar.set(14, 0);
            long timeInMillis2 = calendar.getTimeInMillis();
            if (i5 < i4) {
                calendar.add(5, 1);
                timeInMillis2 = calendar.getTimeInMillis();
            }
            int i6 = calendar.get(11);
            Double[][] pumpProfiles = getLenomedPump().getPumpProfiles();
            if (pumpProfiles != null) {
                Double[][] dArr = pumpProfiles;
                int activeProfile = getLenomedPump().getActiveProfile();
                if (activeProfile >= 0 && activeProfile < 4) {
                    baseAmount = dArr[getLenomedPump().getActiveProfile()][i6].doubleValue();
                } else {
                    getAapsLogger().error(LTag.PUMPCOMM, "Invalid active profile: " + getLenomedPump().getActiveProfile() + ", using baseAmount");
                    baseAmount = getLenomedPump().getBaseAmount();
                }
            } else {
                TempBasalInquireResponsePacket tempBasalInquireResponsePacket = this;
                getAapsLogger().warn(LTag.PUMPCOMM, "pumpProfiles is null, using baseAmount");
                baseAmount = getLenomedPump().getBaseAmount();
            }
            double d = baseAmount;
            if (byteToInt == 1) {
                i2 = byteToInt7;
                j = timeInMillis2;
                i = byteToInt6;
            } else {
                j = timeInMillis2;
                i = byteToInt6;
                double d2 = ((double) byteToInt6) + (((double) byteToInt7) / 100.0d);
                i2 = 100;
                if (d > 0.0d) {
                    double d3 = d2 / d;
                    d = d;
                    i2 = (int) (d3 * ((double) 100));
                } else {
                    d = d;
                    getAapsLogger().error(LTag.PUMPCOMM, "Current basal rate is 0, cannot calculate percent for absolute TBR");
                }
            }
            double d4 = (((double) i2) * d) / 100.0d;
            getLenomedPump().setTbStatus(byteToInt);
            getLenomedPump().setTbTime(i4);
            getLenomedPump().setTempBasalStart(j2);
            getLenomedPump().setTempBasalDuration(RangesKt.coerceAtLeast(j - j2, 0L));
            getLenomedPump().setTempBasalPercent(i2);
            getLenomedPump().setTempBasalAbsoluteRate(d4);
            getLenomedPump().setOtpNumber(1);
            getLenomedPump().setTempBasalInProgress(true);
            getAapsLogger().debug(LTag.PUMPCOMM, "Result --> type=" + byteToInt + ", startTime=" + str + ", endTime=" + str2 + ", percent=" + i2 + "%, rate=" + d4 + " U/h, currentBasalRate=" + d + " U/h, startTimestamp=" + j2 + ", duration=" + getLenomedPump().getTempBasalDuration() + " ms, rateInt=" + i + ", rateFrac=" + byteToInt7);
            setFailed(false);
        } catch (Exception e) {
            getAapsLogger().error(LTag.PUMPCOMM, "Error parsing data: " + e.getMessage());
            setFailed(true);
        }
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}