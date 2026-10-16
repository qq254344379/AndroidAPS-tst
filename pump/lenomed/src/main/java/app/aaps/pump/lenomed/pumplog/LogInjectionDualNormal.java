package app.aaps.pump.lenomed.pumplog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LogInjectionDualNormal.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class LogInjectionDualNormal {

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    public static final byte LOG_KIND = 53;
    private final byte batteryRemain;
    private final String data;
    private final String dttm;
    private final short injectAmount;
    private final byte injectTime;
    private final byte kind;
    private final short setAmount;
    private final byte type;

    public /* synthetic */ LogInjectionDualNormal(String str, String str2, byte b, short s, short s2, byte b2, byte b3, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, str2, b, s, s2, b2, b3);
    }

    private LogInjectionDualNormal(String str, String str2, byte b, short s, short s2, byte b2, byte b3) {
        this.data = str;
        this.dttm = str2;
        this.setAmount = s;
        this.injectAmount = s2;
        this.injectTime = b2;
        this.batteryRemain = b3;
        this.type = PumpLogUtil.INSTANCE.getType(b);
        this.kind = PumpLogUtil.INSTANCE.getKind(b);
    }

    public final String getData() {
        return this.data;
    }

    public final String getDttm() {
        return this.dttm;
    }

    public final short getInjectAmount() {
        return this.injectAmount;
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

    public final int getInjectTime() {
        return this.injectTime & 255;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("LOG_INJECTION_DUAL_NORMAL{LOG_KIND=53, data='");
        sb.append(this.data).append("', dttm='");
        sb.append(this.dttm).append("', type=");
        sb.append((int) this.type);
        sb.append(", kind=").append((int) this.kind);
        sb.append(", setAmount=").append((int) this.setAmount);
        sb.append(", injectAmount=").append((int) this.injectAmount);
        sb.append(", injectTime=").append(this.injectTime & 255);
        sb.append(", batteryRemain=").append((int) this.batteryRemain);
        sb.append('}');
        String string = sb.toString();
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        return string;
    }

    /* JADX INFO: compiled from: LogInjectionDualNormal.kt */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final LogInjectionDualNormal parse(String data) {
            Intrinsics.checkNotNullParameter(data, "data");
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(PumpLogUtil.INSTANCE.hexStringToByteArray(data));
            byteBufferWrap.order(ByteOrder.LITTLE_ENDIAN);
            PumpLogUtil pumpLogUtil = PumpLogUtil.INSTANCE;
            Intrinsics.checkNotNull(byteBufferWrap);
            return new LogInjectionDualNormal(data, pumpLogUtil.getDttm(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getShort(byteBufferWrap), PumpLogUtil.INSTANCE.getShort(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), null);
        }
    }
}