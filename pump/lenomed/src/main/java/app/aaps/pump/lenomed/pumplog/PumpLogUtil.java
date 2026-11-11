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