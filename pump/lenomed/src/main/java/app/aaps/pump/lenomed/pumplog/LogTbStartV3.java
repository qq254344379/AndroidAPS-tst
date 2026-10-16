package app.aaps.pump.lenomed.pumplog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import kotlin.Metadata;
import kotlin.UShort;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.apache.commons.lang3.StringUtils;

/* JADX INFO: compiled from: LogTbStartV3.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class LogTbStartV3 {

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    public static final byte LOG_KIND = 18;
    private final String data;
    private final String dttm;
    private final byte kind;
    private final String tbDttm;
    private final short tbInjectRateRatio;
    private final byte tbTime;
    private final byte type;

    public /* synthetic */ LogTbStartV3(String str, String str2, byte b, byte b2, short s, String str3, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, str2, b, b2, s, str3);
    }

    private LogTbStartV3(String str, String str2, byte b, byte b2, short s, String str3) {
        this.data = str;
        this.dttm = str2;
        this.tbTime = b2;
        this.tbInjectRateRatio = s;
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

    public final byte getTbTime() {
        return this.tbTime;
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
        StringBuilder sb = new StringBuilder("LOG_TB_START_V3{LOG_KIND=18, data='");
        sb.append(this.data).append("', dttm='");
        sb.append(this.dttm).append("', type=");
        sb.append((int) this.type);
        sb.append(", kind=").append((int) this.kind);
        sb.append(", tbTime=").append((int) this.tbTime);
        sb.append(", tbInjectRateRatio=").append(this.tbInjectRateRatio & UShort.MAX_VALUE);
        if (!StringUtils.equals(this.tbDttm, PumpLogUtil.INSTANCE.getDttm("ffffffff"))) {
            sb.append(", tbDttm=").append(this.tbDttm);
        }
        sb.append('}');
        String string = sb.toString();
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        return string;
    }

    /* JADX INFO: compiled from: LogTbStartV3.kt */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final LogTbStartV3 parse(String data) {
            Intrinsics.checkNotNullParameter(data, "data");
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(PumpLogUtil.INSTANCE.hexStringToByteArray(data));
            byteBufferWrap.order(ByteOrder.LITTLE_ENDIAN);
            PumpLogUtil pumpLogUtil = PumpLogUtil.INSTANCE;
            Intrinsics.checkNotNull(byteBufferWrap);
            return new LogTbStartV3(data, pumpLogUtil.getDttm(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getByte(byteBufferWrap), PumpLogUtil.INSTANCE.getShort(byteBufferWrap), PumpLogUtil.INSTANCE.getDttm(byteBufferWrap), null);
        }
    }
}