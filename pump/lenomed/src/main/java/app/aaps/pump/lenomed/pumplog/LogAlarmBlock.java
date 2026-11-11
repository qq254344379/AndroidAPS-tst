package app.aaps.pump.lenomed.pumplog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LogAlarmBlock.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class LogAlarmBlock {

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    public static final byte LOG_KIND = 41;
    private final byte ack;
    private final byte alarmLevel;
    private final short amount;
    private final byte batteryRemain;
    private final String data;
    private final String dttm;
    private final byte kind;
    private final byte reason;
    private final byte type;

    public /* synthetic */ LogAlarmBlock(String str, String str2, byte b, byte b2, byte b3, short s, byte b4, byte b5, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, str2, b, b2, b3, s, b4, b5);
    }

    private LogAlarmBlock(String str, String str2, byte b, byte b2, byte b3, short s, byte b4, byte b5) {
        this.data = str;
        this.dttm = str2;
        this.alarmLevel = b2;
        this.ack = b3;
        this.amount = s;
        this.reason = b4;
        this.batteryRemain = b5;
        this.type = PumpLogUtil.INSTANCE.getType(b);
        this.kind = PumpLogUtil.INSTANCE.getKind(b);
    }

    public final String getData() {
        return this.data;
    }

    public final String getDttm() {
        return this.dttm;
    }

    public final short getAmount() {
        return this.amount;
    }

    public final byte getReason() {
        return this.reason;
    }

    public final byte getBatteryRemain() {
        return this.batteryRemain;
    }

    public final byte getType() {
        return this.type;
    }

    public final byte getKind() {
        return this.kind;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("LOG_ALARM_BLOCK{LOG_KIND=41, data='");
        sb.append(this.data).append("', dttm='");
        sb.append(this.dttm).append("', type=");
        sb.append((int) this.type);
        sb.append(", kind=").append((int) this.kind);
        sb.append(", alarmLevel=").append((int) this.alarmLevel);
        sb.append(", ack=").append((int) this.ack);
        sb.append(", amount=").append((int) this.amount);
        sb.append(", reason=").append((int) this.reason);
        sb.append(", batteryRemain=").append((int) this.batteryRemain);
        sb.append('}');
        String string = sb.toString();
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        return string;
    }

    /* JADX INFO: compiled from: LogAlarmBlock.kt */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final LogAlarmBlock parse(String data) {
            Intrinsics.checkNotNullParameter(data, "data");
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(PumpLogUtil.INSTANCE.hexStringToByteArray(data));
            byteBufferWrap.order(ByteOrder.LITTLE_ENDIAN);
            PumpLogUtil pumpLogUtil = PumpLogUtil.INSTANCE;
            Intrinsics.checkNotNull(byteBufferWrap);
            return new LogAlarmBlock(data, pumpLogUtil.getDttm(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getShort(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), null);
        }
    }
}