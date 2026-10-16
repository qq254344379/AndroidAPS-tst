package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
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

/* JADX INFO: compiled from: TimeSettingResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u0012\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u0010\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fH\u0016J\u0010\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u0011H\u0002R\u001e\u0010\u0006\u001a\u00020\u00078\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u0014\u0010\u0013\u001a\u00020\u0014X\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016¨\u0006\u0017"}, d2 = {"Lapp/aaps/pump/lenomed/packet/TimeSettingResponsePacket;", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "injector", "Ldagger/android/HasAndroidInjector;", "<init>", "(Ldagger/android/HasAndroidInjector;)V", "lenomedPump", "Lapp/aaps/pump/lenomed/LenomedPump;", "getLenomedPump", "()Lapp/aaps/pump/lenomed/LenomedPump;", "setLenomedPump", "(Lapp/aaps/pump/lenomed/LenomedPump;)V", "handleMessage", "", "data", "", "parseBCD", "", "bcd", "friendlyName", "", "getFriendlyName", "()Ljava/lang/String;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class TimeSettingResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    private final int parseBCD(int bcd) {
        return (((bcd >> 4) & 15) * 10) + (bcd & 15);
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public TimeSettingResponsePacket() {
        super();
        setMsgType((byte) -79);
        getAapsLogger().debug(LTag.PUMPCOMM, "TimeSettingResponsePacket init");
        this.friendlyName = "PUMP_TIME_SETTING_RESPONSE";
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
        getAapsLogger().debug(LTag.PUMPCOMM, "Raw data: " + ArraysKt.joinToString$default(data, (CharSequence) null, (CharSequence) null, (CharSequence) null, 0, (CharSequence) null, new Function1() { // from class: app.aaps.pump.lenomed.packet.TimeSettingResponsePacket$$ExternalSyntheticLambda0
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return TimeSettingResponsePacket.handleMessage$lambda$0(((Byte) obj).byteValue());
            }
        }, 31, (Object) null));
        if (data.length < 6) {
            getAapsLogger().error(LTag.PUMPCOMM, "Time setting response too short: " + data.length + " bytes");
            setFailed(true);
            return;
        }
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        try {
            int byteToInt = getByteToInt(byteBufferPrefixDecode);
            int byteToInt2 = getByteToInt(byteBufferPrefixDecode);
            int byteToInt3 = getByteToInt(byteBufferPrefixDecode);
            int byteToInt4 = getByteToInt(byteBufferPrefixDecode);
            int byteToInt5 = getByteToInt(byteBufferPrefixDecode);
            int bcd = parseBCD(byteToInt) + 2000;
            int bcd2 = parseBCD(byteToInt2);
            int bcd3 = parseBCD(byteToInt3);
            int bcd4 = parseBCD(byteToInt4);
            int bcd5 = parseBCD(byteToInt5);
            getLenomedPump().setYear(byteToInt);
            getLenomedPump().setMonth(byteToInt2);
            getLenomedPump().setDay(byteToInt3);
            getLenomedPump().setHour(byteToInt4);
            getLenomedPump().setMinute(byteToInt5);
            StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
            String str = String.format("%04d-%02d-%02d %02d:%02d", Arrays.copyOf(new Object[]{Integer.valueOf(bcd), Integer.valueOf(bcd2), Integer.valueOf(bcd3), Integer.valueOf(bcd4), Integer.valueOf(bcd5)}, 5));
            Intrinsics.checkNotNullExpressionValue(str, "format(...)");
            getAapsLogger().debug(LTag.PUMPCOMM, "Pump reported time: " + str);
            long jNow = getDateUtil().now();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(jNow);
            int i = calendar.get(1);
            int i2 = calendar.get(2) + 1;
            int i3 = calendar.get(5);
            int i4 = calendar.get(11);
            int i5 = calendar.get(12);
            int iAbs = Math.abs(((bcd4 - i4) * 60) + (bcd5 - i5));
            boolean z = bcd == i && bcd2 == i2 && bcd3 == i3;
            if (z && iAbs <= 2) {
                getAapsLogger().debug(LTag.PUMPCOMM, "Time setting successful and verified: " + str + " (within ±2 min)");
                getLenomedPump().setOtpNumber(1);
                setFailed(false);
                return;
            }
            AAPSLogger aapsLogger = getAapsLogger();
            LTag lTag = LTag.PUMPCOMM;
            StringCompanionObject stringCompanionObject2 = StringCompanionObject.INSTANCE;
            String str2 = String.format("%04d-%02d-%02d %02d:%02d", Arrays.copyOf(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5)}, 5));
            Intrinsics.checkNotNullExpressionValue(str2, "format(...)");
            aapsLogger.warn(lTag, "Time setting response mismatch! Expected ≈ " + str2 + ", Received: " + str + " (diff: " + iAbs + "min, dateMatch: " + z + ")");
            setFailed(true);
        } catch (Exception e) {
            getAapsLogger().error(LTag.PUMPCOMM, "Error parsing time setting response: " + e.getMessage(), e);
            setFailed(true);
        }
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}