package app.aaps.pump.lenomed.packet;

import androidx.constraintlayout.core.motion.utils.TypedValues;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.collections.ArraysKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/* JADX INFO: compiled from: TimeSettingPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0012\n\u0002\b\u0002\n\u0002\u0010\u0005\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B#\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007¢\u0006\u0004\b\b\u0010\tJ\u0010\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u0007H\u0016J\u0010\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0007H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u000e¢\u0006\u0002\n\u0000R\u001e\u0010\n\u001a\u00020\u000b8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u0014\u0010\u0016\u001a\u00020\u0017X\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019¨\u0006\u001a"}, d2 = {"Lapp/aaps/pump/lenomed/packet/TimeSettingPacket;", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "injector", "Ldagger/android/HasAndroidInjector;", "time", "", TypedValues.CycleType.S_WAVE_OFFSET, "", "<init>", "(Ldagger/android/HasAndroidInjector;JI)V", "lenomedPump", "Lapp/aaps/pump/lenomed/LenomedPump;", "getLenomedPump", "()Lapp/aaps/pump/lenomed/LenomedPump;", "setLenomedPump", "(Lapp/aaps/pump/lenomed/LenomedPump;)V", "encode", "", "msgSeq", "packBcd", "", "value", "friendlyName", "", "getFriendlyName", "()Ljava/lang/String;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class TimeSettingPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private int offset;
    private long time;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public TimeSettingPacket( long j, int i) {
        super();
        this.time = j;
        this.offset = i;
        setMsgType((byte) -79);
        getAapsLogger().debug(LTag.PUMPCOMM, "Time Sync between Phone and Pump");
        this.friendlyName = "PUMP_TIME_SETTING_REQUEST";
    }

    public /* synthetic */ TimeSettingPacket(HasAndroidInjector hasAndroidInjector, long j, int i, int i2, DefaultConstructorMarker defaultConstructorMarker) {
        this(hasAndroidInjector, (i2 & 2) != 0 ? System.currentTimeMillis() : j, (i2 & 4) != 0 ? 2 : i);
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
        ByteBuffer byteBufferPrefixEncode = prefixEncode(getMsgType(), 4, (byte) 9);
        DateTime dateTimeWithZone = new DateTime(this.time).withZone(DateTimeZone.forOffsetHoursMinutes(this.offset, 0));
        int year = dateTimeWithZone.getYear() % 100;
        int monthOfYear = dateTimeWithZone.getMonthOfYear();
        int dayOfMonth = dateTimeWithZone.getDayOfMonth();
        int hourOfDay = dateTimeWithZone.getHourOfDay();
        int minuteOfHour = dateTimeWithZone.getMinuteOfHour();
        dateTimeWithZone.getSecondOfMinute();
        byteBufferPrefixEncode.put(packBcd(year));
        byteBufferPrefixEncode.put(packBcd(monthOfYear));
        byteBufferPrefixEncode.put(packBcd(dayOfMonth));
        byteBufferPrefixEncode.put(packBcd(hourOfDay));
        byteBufferPrefixEncode.put(packBcd(minuteOfHour));
        byte[] bArrArray = byteBufferPrefixEncode.array();
        Intrinsics.checkNotNullExpressionValue(bArrArray, "array(...)");
        byte[] bArrCopyOfRange = ArraysKt.copyOfRange(bArrArray, byteBufferPrefixEncode.position() - 6, byteBufferPrefixEncode.position());
        getAapsLogger().debug(LTag.PUMPCOMM, "Generated time bytes: " + (bArrCopyOfRange[0] & 255) + ", " + (bArrCopyOfRange[1] & 255) + ", " + (bArrCopyOfRange[2] & 255) + ", " + (bArrCopyOfRange[3] & 255) + ", " + (bArrCopyOfRange[4] & 255) + ", " + (bArrCopyOfRange[5] & 255));
        return suffixEncode(byteBufferPrefixEncode);
    }

    private final byte packBcd(int value) {
        return (byte) (((value / 10) << 4) | (value % 10));
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}