package app.aaps.pump.lenomed.pumplog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LogResetSysV3.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class LogResetSysV3 {

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    public static final byte LOG_KIND = 1;
    private final byte batteryRemain;
    private final String data;
    private final String dttm;
    private final byte kind;
    private final short rcon1;
    private final short rcon2;
    private final byte reason;
    private final byte type;

    public /* synthetic */ LogResetSysV3(String str, String str2, byte b, byte b2, byte b3, short s, short s2, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, str2, b, b2, b3, s, s2);
    }

    private LogResetSysV3(String str, String str2, byte b, byte b2, byte b3, short s, short s2) {
        this.data = str;
        this.dttm = str2;
        this.batteryRemain = b2;
        this.reason = b3;
        this.rcon1 = s;
        this.rcon2 = s2;
        this.type = PumpLogUtil.INSTANCE.getType(b);
        this.kind = PumpLogUtil.INSTANCE.getKind(b);
    }

    public final String getData() {
        return this.data;
    }

    public final String getDttm() {
        return this.dttm;
    }

    public final byte getBatteryRemain() {
        return this.batteryRemain;
    }

    public final byte getReason() {
        return this.reason;
    }

    public final byte getType() {
        return this.type;
    }

    public final byte getKind() {
        return this.kind;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("LOG_RESET_SYS_V3{LOG_KIND=1, data='");
        sb.append(this.data).append("', dttm='");
        sb.append(this.dttm).append("', type=");
        sb.append((int) this.type);
        sb.append(", kind=").append((int) this.kind);
        sb.append(", batteryRemain=").append((int) this.batteryRemain);
        sb.append(", reason=").append((int) this.reason);
        sb.append(", rcon1=").append((int) this.rcon1);
        sb.append(", rcon2=").append((int) this.rcon2);
        sb.append('}');
        String string = sb.toString();
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        return string;
    }

    /* JADX INFO: compiled from: LogResetSysV3.kt */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final LogResetSysV3 parse(String data) {
            Intrinsics.checkNotNullParameter(data, "data");
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(PumpLogUtil.INSTANCE.hexStringToByteArray(data));
            byteBufferWrap.order(ByteOrder.LITTLE_ENDIAN);
            PumpLogUtil pumpLogUtil = PumpLogUtil.INSTANCE;
            Intrinsics.checkNotNull(byteBufferWrap);
            return new LogResetSysV3(data, pumpLogUtil.getDttm(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getShort(byteBufferWrap), PumpLogUtil.INSTANCE.getShort(byteBufferWrap), null);
        }
    }
}