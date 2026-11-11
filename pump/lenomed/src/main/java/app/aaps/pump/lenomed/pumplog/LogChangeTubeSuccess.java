package app.aaps.pump.lenomed.pumplog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LogChangeTubeSuccess.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class LogChangeTubeSuccess {

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    public static final byte LOG_KIND = 24;
    private final byte batteryRemain;
    private final String data;
    private final String dttm;
    private final byte kind;
    private final short primeAmount;
    private final short remainAmount;
    private final byte type;

    public /* synthetic */ LogChangeTubeSuccess(String str, String str2, byte b, short s, short s2, byte b2, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, str2, b, s, s2, b2);
    }

    private LogChangeTubeSuccess(String str, String str2, byte b, short s, short s2, byte b2) {
        this.data = str;
        this.dttm = str2;
        this.primeAmount = s;
        this.remainAmount = s2;
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

    public final short getPrimeAmount() {
        return this.primeAmount;
    }

    public final short getRemainAmount() {
        return this.remainAmount;
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
        StringBuilder sb = new StringBuilder("LOG_CHANGE_TUBE_SUCCESS{LOG_KIND=24, data='");
        sb.append(this.data).append("', dttm='");
        sb.append(this.dttm).append("', type=");
        sb.append((int) this.type);
        sb.append(", kind=").append((int) this.kind);
        sb.append(", primeAmount=").append((int) this.primeAmount);
        sb.append(", remainAmount=").append((int) this.remainAmount);
        sb.append(", batteryRemain=").append((int) this.batteryRemain);
        sb.append('}');
        String string = sb.toString();
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        return string;
    }

    /* JADX INFO: compiled from: LogChangeTubeSuccess.kt */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final LogChangeTubeSuccess parse(String data) {
            Intrinsics.checkNotNullParameter(data, "data");
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(PumpLogUtil.INSTANCE.hexStringToByteArray(data));
            byteBufferWrap.order(ByteOrder.LITTLE_ENDIAN);
            PumpLogUtil pumpLogUtil = PumpLogUtil.INSTANCE;
            Intrinsics.checkNotNull(byteBufferWrap);
            return new LogChangeTubeSuccess(data, pumpLogUtil.getDttm(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getShort(byteBufferWrap), PumpLogUtil.INSTANCE.getShort(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), null);
        }
    }
}