package app.aaps.pump.lenomed.pumplog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LogInjection1DayBasal.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class LogInjection1DayBasal {

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    public static final byte LOG_KIND = 46;
    private final short amount;
    private final byte batteryRemain;
    private final String data;
    private final String dttm;
    private final byte kind;
    private final byte type;

    public /* synthetic */ LogInjection1DayBasal(String str, String str2, byte b, short s, byte b2, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, str2, b, s, b2);
    }

    private LogInjection1DayBasal(String str, String str2, byte b, short s, byte b2) {
        this.data = str;
        this.dttm = str2;
        this.amount = s;
        this.batteryRemain = b2;
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
        StringBuilder sb = new StringBuilder("LOG_INJECTION_1DAY_BASAL{LOG_KIND=46, data='");
        sb.append(this.data).append("', dttm='");
        sb.append(this.dttm).append("', type=");
        sb.append((int) this.type);
        sb.append(", kind=").append((int) this.kind);
        sb.append(", amount=").append((int) this.amount);
        sb.append(", batteryRemain=").append((int) this.batteryRemain);
        sb.append('}');
        String string = sb.toString();
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        return string;
    }

    /* JADX INFO: compiled from: LogInjection1DayBasal.kt */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final LogInjection1DayBasal parse(String data) {
            Intrinsics.checkNotNullParameter(data, "data");
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(PumpLogUtil.INSTANCE.hexStringToByteArray(data));
            byteBufferWrap.order(ByteOrder.LITTLE_ENDIAN);
            PumpLogUtil pumpLogUtil = PumpLogUtil.INSTANCE;
            Intrinsics.checkNotNull(byteBufferWrap);
            return new LogInjection1DayBasal(data, pumpLogUtil.getDttm(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getShort(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), null);
        }
    }
}