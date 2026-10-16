package app.aaps.pump.lenomed.pumplog;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.StringCompanionObject;
import kotlin.text.CharsKt;
import okio.Utf8;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

/* JADX INFO: compiled from: PumpLogUtil.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0005\n\u0000\n\u0002\u0010\n\n\u0002\b\u0004\n\u0002\u0010\u0012\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\bÆ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u0010\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H\u0007J\u0010\u0010\u0004\u001a\u00020\u00052\u0006\u0010\b\u001a\u00020\u0005H\u0007J\u000e\u0010\t\u001a\u00020\n2\u0006\u0010\u0006\u001a\u00020\u0007J\u000e\u0010\u000b\u001a\u00020\f2\u0006\u0010\u0006\u001a\u00020\u0007J\u000e\u0010\r\u001a\u00020\n2\u0006\u0010\b\u001a\u00020\u0005J\u000e\u0010\u000e\u001a\u00020\n2\u0006\u0010\b\u001a\u00020\u0005J\u000e\u0010\r\u001a\u00020\n2\u0006\u0010\u000f\u001a\u00020\nJ\u000e\u0010\u000e\u001a\u00020\n2\u0006\u0010\u000f\u001a\u00020\nJ\u000e\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u0005J \u0010\u0013\u001a\u00020\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0017¨\u0006\u0019"}, d2 = {"Lapp/aaps/pump/lenomed/pumplog/PumpLogUtil;", "", "<init>", "()V", "getDttm", "", "buffer", "Ljava/nio/ByteBuffer;", "data", "getByte", "", "getShort", "", "getType", "getKind", "b", "hexStringToByteArray", "", "s", "isPumpVersionGe", "", "pumpVersion", "major", "", "minor", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class PumpLogUtil {
    public static final PumpLogUtil INSTANCE = new PumpLogUtil();

    public final byte getKind(byte b) {
        return (byte) (b & Utf8.REPLACEMENT_BYTE);
    }

    public final byte getType(byte b) {
        return (byte) ((b >> 6) & 3);
    }

    private PumpLogUtil() {
    }

    public final String getDttm(ByteBuffer buffer) {
        Intrinsics.checkNotNullParameter(buffer, "buffer");
        byte b = buffer.get();
        byte b2 = buffer.get();
        byte b3 = buffer.get();
        byte b4 = buffer.get();
        StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
        String str = String.format("%02x%02x%02x%02x", Arrays.copyOf(new Object[]{Byte.valueOf(b4), Byte.valueOf(b3), Byte.valueOf(b2), Byte.valueOf(b)}, 4));
        Intrinsics.checkNotNullExpressionValue(str, "format(...)");
        long j = Long.parseLong(str, CharsKt.checkRadix(16));
        long time = new Date(0L).getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UCT"));
        String str2 = simpleDateFormat.format(new Date(time + (j * ((long) 1000))));
        Intrinsics.checkNotNullExpressionValue(str2, "format(...)");
        return str2;
    }

    public final String getDttm(String data) {
        Intrinsics.checkNotNullParameter(data, "data");
        byte[] bArrHexStringToByteArray = hexStringToByteArray(data);
        byte b = bArrHexStringToByteArray[0];
        byte b2 = bArrHexStringToByteArray[1];
        byte b3 = bArrHexStringToByteArray[2];
        byte b4 = bArrHexStringToByteArray[3];
        StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
        String str = String.format("%02x%02x%02x%02x", Arrays.copyOf(new Object[]{Byte.valueOf(b4), Byte.valueOf(b3), Byte.valueOf(b2), Byte.valueOf(b)}, 4));
        Intrinsics.checkNotNullExpressionValue(str, "format(...)");
        long j = Long.parseLong(str, CharsKt.checkRadix(16));
        long time = new Date(0L).getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String str2 = simpleDateFormat.format(new Date(time + (j * ((long) 1000))));
        Intrinsics.checkNotNullExpressionValue(str2, "format(...)");
        return str2;
    }

    public final byte getByte(ByteBuffer buffer) {
        Intrinsics.checkNotNullParameter(buffer, "buffer");
        return buffer.get();
    }

    public final short getShort(ByteBuffer buffer) {
        Intrinsics.checkNotNullParameter(buffer, "buffer");
        return buffer.getShort();
    }

    public final byte getType(String data) {
        Intrinsics.checkNotNullParameter(data, "data");
        return getType(hexStringToByteArray(data)[4]);
    }

    public final byte getKind(String data) {
        Intrinsics.checkNotNullParameter(data, "data");
        return getKind(hexStringToByteArray(data)[4]);
    }

    public final byte[] hexStringToByteArray(String s) {
        Intrinsics.checkNotNullParameter(s, "s");
        int length = s.length();
        byte[] bArr = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bArr[i / 2] = (byte) ((CharsKt.digitToInt(s.charAt(i), 16) << 4) + CharsKt.digitToInt(s.charAt(i + 1), 16));
        }
        return bArr;
    }

    public final boolean isPumpVersionGe(String pumpVersion, int major, int minor) {
        String strReplaceAll = RegExUtils.replaceAll(pumpVersion, "[^\\d\\.]", "");
        String str = StringUtils.split(strReplaceAll, ".")[0];
        Intrinsics.checkNotNullExpressionValue(str, "get(...)");
        int i = Integer.parseInt(str);
        String str2 = StringUtils.split(strReplaceAll, ".")[1];
        Intrinsics.checkNotNullExpressionValue(str2, "get(...)");
        int i2 = Integer.parseInt(str2);
        if (i > major) {
            return true;
        }
        return i >= major && i2 >= minor;
    }
}