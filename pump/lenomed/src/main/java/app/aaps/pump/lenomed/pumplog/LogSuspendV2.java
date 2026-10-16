package app.aaps.pump.lenomed.pumplog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LogSuspendV2.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0005\n\u0002\b\u0011\u0018\u0000 \u00162\u00020\u0001:\u0001\u0016B1\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u0012\u0006\u0010\b\u001a\u00020\u0006¢\u0006\u0004\b\t\u0010\nJ\b\u0010\u0014\u001a\u00020\u0003H\u0016J\u0006\u0010\u0015\u001a\u00020\u0003R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\r\u0010\fR\u0011\u0010\u0007\u001a\u00020\u0006¢\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u000e\u0010\b\u001a\u00020\u0006X\u0082\u0004¢\u0006\u0002\n\u0000R\u0011\u0010\u0010\u001a\u00020\u0006¢\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u000fR\u0011\u0010\u0012\u001a\u00020\u0006¢\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u000f¨\u0006\u0017"}, d2 = {"Lapp/aaps/pump/lenomed/pumplog/LogSuspendV2;", "", "data", "", "dttm", "typeAndKind", "", "batteryRemain", "patternType", "<init>", "(Ljava/lang/String;Ljava/lang/String;BBB)V", "getData", "()Ljava/lang/String;", "getDttm", "getBatteryRemain", "()B", "type", "getType", "kind", "getKind", "toString", "getBasalPattern", "Companion", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LogSuspendV2 {

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    public static final byte LOG_KIND = 3;
    private final byte batteryRemain;
    private final String data;
    private final String dttm;
    private final byte kind;
    private final byte patternType;
    private final byte type;

    public /* synthetic */ LogSuspendV2(String str, String str2, byte b, byte b2, byte b3, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, str2, b, b2, b3);
    }

    private LogSuspendV2(String str, String str2, byte b, byte b2, byte b3) {
        this.data = str;
        this.dttm = str2;
        this.batteryRemain = b2;
        this.patternType = b3;
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

    public final byte getType() {
        return this.type;
    }

    public final byte getKind() {
        return this.kind;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("LOG_SUSPEND_V2{LOG_KIND=3, data='");
        sb.append(this.data).append("', dttm='");
        sb.append(this.dttm).append("', type=");
        sb.append((int) this.type);
        sb.append(", kind=").append((int) this.kind);
        sb.append(", batteryRemain=").append((int) this.batteryRemain);
        sb.append(", patternType=").append((int) this.patternType);
        sb.append('}');
        String string = sb.toString();
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        return string;
    }

    public final String getBasalPattern() {
        byte b = this.patternType;
        if (b == 1) {
            return "Base";
        }
        if (b == 2) {
            return "Life1";
        }
        if (b == 3) {
            return "Life2";
        }
        if (b == 4) {
            return "Life3";
        }
        if (b == 5) {
            return "Dr1";
        }
        if (b == 6) {
            return "Dr2";
        }
        return "No Pattern";
    }

    /* JADX INFO: compiled from: LogSuspendV2.kt */
    @Metadata(d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u0005\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u000e\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tR\u000e\u0010\u0004\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000¨\u0006\n"}, d2 = {"Lapp/aaps/pump/lenomed/pumplog/LogSuspendV2$Companion;", "", "<init>", "()V", "LOG_KIND", "", "parse", "Lapp/aaps/pump/lenomed/pumplog/LogSuspendV2;", "data", "", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final LogSuspendV2 parse(String data) {
            Intrinsics.checkNotNullParameter(data, "data");
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(PumpLogUtil.INSTANCE.hexStringToByteArray(data));
            byteBufferWrap.order(ByteOrder.LITTLE_ENDIAN);
            PumpLogUtil pumpLogUtil = PumpLogUtil.INSTANCE;
            Intrinsics.checkNotNull(byteBufferWrap);
            return new LogSuspendV2(data, pumpLogUtil.getDttm(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), null);
        }
    }
}