package app.aaps.pump.lenomed.pumplog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import kotlin.Metadata;
import kotlin.UShort;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.apache.commons.lang3.StringUtils;

/* JADX INFO: compiled from: LogTbStopV3.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class LogTbStopV3 {

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    public static final byte LOG_KIND = 19;
    private final String data;
    private final String dttm;
    private final byte kind;
    private final byte reason;
    private final String tbDttm;
    private final short tbInjectRateRatio;
    private final byte type;

    public /* synthetic */ LogTbStopV3(String str, String str2, byte b, short s, byte b2, String str3, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, str2, b, s, b2, str3);
    }

    private LogTbStopV3(String str, String str2, byte b, short s, byte b2, String str3) {
        this.data = str;
        this.dttm = str2;
        this.tbInjectRateRatio = s;
        this.reason = b2;
        this.tbDttm = str3;
        this.type = PumpLogUtil.INSTANCE.getType(b);
        this.kind = PumpLogUtil.INSTANCE.getKind(b);
    }

    public final String getData() {
        return this.data;
    }

    public final String getDttm() {
        return this.dttm;
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

    public final int getTbInjectRateRatio() {
        return this.tbInjectRateRatio & UShort.MAX_VALUE;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("LOG_TB_STOP_V3{LOG_KIND=19, data='");
        sb.append(this.data).append("', dttm='");
        sb.append(this.dttm).append("', type=");
        sb.append((int) this.type);
        sb.append(", kind=").append((int) this.kind);
        sb.append(", tbInjectRateRatio=").append(this.tbInjectRateRatio & UShort.MAX_VALUE);
        sb.append(", reason=").append((int) this.reason);
        if (!StringUtils.equals(this.tbDttm, PumpLogUtil.INSTANCE.getDttm("ffffffff"))) {
            sb.append(", tbDttm=").append(this.tbDttm);
        }
        sb.append('}');
        String string = sb.toString();
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        return string;
    }

    /* JADX INFO: compiled from: LogTbStopV3.kt */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final LogTbStopV3 parse(String data) {
            Intrinsics.checkNotNullParameter(data, "data");
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(PumpLogUtil.INSTANCE.hexStringToByteArray(data));
            byteBufferWrap.order(ByteOrder.LITTLE_ENDIAN);
            PumpLogUtil pumpLogUtil = PumpLogUtil.INSTANCE;
            Intrinsics.checkNotNull(byteBufferWrap);
            return new LogTbStopV3(data, pumpLogUtil.getDttm(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getShort(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getDttm(byteBufferWrap), null);
        }
    }
}