package app.aaps.pump.lenomed.packet;

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
        byte[] bArrCopyOfRange = java.util.Arrays.copyOfRange(bArrArray, byteBufferPrefixEncode.position() - 6, byteBufferPrefixEncode.position());
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