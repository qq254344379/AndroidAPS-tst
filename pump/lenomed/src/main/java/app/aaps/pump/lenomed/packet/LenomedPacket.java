package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.utils.DateUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.UShort;
import kotlin.collections.ArraysKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.StringCompanionObject;

/* JADX INFO: compiled from: LenomedPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public class LenomedPacket {
    public static final byte BT_MSG_DATA_LOC = 1;

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    public static final int MSG_LEN = 5;
    public static final int MSG_LEN_BIG = 182;
    public static final int MSG_SEQ_LOC = 2;
    public static final int MSG_TYPE_LOC = 2;
    public static final byte SOP_BIG = -19;
    public static final byte SOP_READ = 4;
    public static final byte SOP_WRITE = 5;
    public static final byte SOP_WRITE_X = 9;
    public static final byte SOP_WRITE_x11 = 17;
    public static final byte SOP_WRITE_x21 = 33;

    @Inject
    public AAPSLogger aapsLogger;

    @Inject
    public DateUtil dateUtil;
    private boolean failed;
    private final String friendlyName;
        private byte msgType;
    private boolean received;

    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
    }

    public LenomedPacket() {
        this.friendlyName = "UNKNOWN_PACKET";
    }

    public final AAPSLogger getAapsLogger() {
        AAPSLogger aAPSLogger = this.aapsLogger;
        if (aAPSLogger != null) {
            return aAPSLogger;
        }
        Intrinsics.throwUninitializedPropertyAccessException("aapsLogger");
        return null;
    }

    public final void setAapsLogger(AAPSLogger aAPSLogger) {
        Intrinsics.checkNotNullParameter(aAPSLogger, "<set-?>");
        this.aapsLogger = aAPSLogger;
    }

    public final DateUtil getDateUtil() {
        DateUtil dateUtil = this.dateUtil;
        if (dateUtil != null) {
            return dateUtil;
        }
        Intrinsics.throwUninitializedPropertyAccessException("dateUtil");
        return null;
    }

    public final void setDateUtil(DateUtil dateUtil) {
        Intrinsics.checkNotNullParameter(dateUtil, "<set-?>");
        this.dateUtil = dateUtil;
    }

    public final boolean getFailed() {
        return this.failed;
    }

    public final void setFailed(boolean z) {
        this.failed = z;
    }

    public final byte getMsgType() {
        return this.msgType;
    }

    public final void setMsgType(byte b) {
        this.msgType = b;
    }

    public String getFriendlyName() {
        return this.friendlyName;
    }

    public final boolean success() {
        return !this.failed;
    }

    public final void setReceived() {
        this.received = true;
    }

    public static /* synthetic */ ByteBuffer prefixEncode$default(LenomedPacket lenomedPacket, byte b, int i, byte b2, int i2, Object obj) {
        if (obj != null) {
            throw new UnsupportedOperationException("Super calls with default arguments not supported in this target, function: prefixEncode");
        }
        if ((i2 & 4) != 0) {
            b2 = 4;
        }
        return lenomedPacket.prefixEncode(b, i, b2);
    }

    protected final ByteBuffer prefixEncode(byte msgType, int dataLength, byte sop) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(dataLength + 5);
        byteBufferAllocate.order(ByteOrder.LITTLE_ENDIAN);
        byteBufferAllocate.put(sop);
        byteBufferAllocate.put(msgType);
        Intrinsics.checkNotNull(byteBufferAllocate);
        return byteBufferAllocate;
    }

    public final ByteBuffer prefixEncode(byte msgType) {
        return prefixEncode(msgType, 0, (byte) 4);
    }

    public final byte[] suffixEncode(ByteBuffer buffer) {
        Intrinsics.checkNotNullParameter(buffer, "buffer");
        if (buffer.remaining() < 2) {
            getAapsLogger().error(LTag.PUMPCOMM, "Buffer overflow: insufficient space for CRC");
            throw new IllegalStateException("Packet size exceeds limit");
        }
        Companion companion = INSTANCE;
        byte[] bArrArray = buffer.array();
        Intrinsics.checkNotNullExpressionValue(bArrArray, "array(...)");
        byte[] crc16ccitt = companion.getCRC16CCITT(bArrArray, buffer.position());
        getAapsLogger().debug(LTag.PUMP, "crc:" + companion.toHex(crc16ccitt));
        buffer.put(crc16ccitt);
        byte[] bArrArray2 = buffer.array();
        Intrinsics.checkNotNullExpressionValue(bArrArray2, "array(...)");
        return bArrArray2;
    }

    public final int getType(byte[] bytes) {
        Intrinsics.checkNotNullParameter(bytes, "bytes");
        return bytes[2] & 255;
    }

    public final int getCmd(byte[] bytes) {
        Intrinsics.checkNotNullParameter(bytes, "bytes");
        if (bytes.length > 2) {
            return bytes[2] & 255;
        }
        return -1;
    }

    public byte[] encode(int msgSeq) {
        return new byte[0];
    }

    public final int getByteToInt(ByteBuffer buffer) {
        Intrinsics.checkNotNullParameter(buffer, "buffer");
        return buffer.get() & 255;
    }

    public final int getShortToInt(ByteBuffer buffer) {
        Intrinsics.checkNotNullParameter(buffer, "buffer");
        return buffer.getShort() & UShort.MAX_VALUE;
    }

    public final int getIntToInt(ByteBuffer buffer) {
        Intrinsics.checkNotNullParameter(buffer, "buffer");
        return buffer.getInt();
    }

    public final int getSeq(byte[] bytes) {
        Intrinsics.checkNotNullParameter(bytes, "bytes");
        return bytes[2] & 255;
    }

    public final boolean isSuccInquireResponseResult(int result) {
        switch (result) {
            case 16:
                break;
            case 17:
                getAapsLogger().error(LTag.PUMPCOMM, "Packet CRC error");
                break;
            case 18:
                getAapsLogger().error(LTag.PUMPCOMM, "Parameter error");
                break;
            case 19:
                getAapsLogger().error(LTag.PUMPCOMM, "Protocol specification error");
                break;
            default:
                getAapsLogger().error(LTag.PUMPCOMM, "System error");
                break;
        }
        return false;
    }

    public final boolean isSuccSettingResponseResult(int result) {
        if (result == 0) {
            return true;
        }
        if (result == 1) {
            getAapsLogger().error(LTag.PUMPCOMM, "Packet exec error");
            return false;
        }
        if (result == 2) {
            getAapsLogger().error(LTag.PUMPCOMM, "Parameter error");
            return false;
        }
        if (result == 3) {
            getAapsLogger().error(LTag.PUMPCOMM, "Protocol specification error");
            return false;
        }
        if (result == 4) {
            getAapsLogger().error(LTag.PUMPCOMM, "Eating timeout, not injectable");
            return false;
        }
        switch (result) {
            case 6:
                getAapsLogger().error(LTag.PUMPCOMM, "Pump canceled operation");
                break;
            case 7:
                getAapsLogger().error(LTag.PUMPCOMM, "Other operations in progress, limited app setup capabilities");
                break;
            case 8:
                getAapsLogger().error(LTag.PUMPCOMM, "During another bolus injection, injection restricted");
                break;
            case 9:
                getAapsLogger().error(LTag.PUMPCOMM, "Basal release required");
                break;
            case 10:
                getAapsLogger().error(LTag.PUMPCOMM, "Canceled due to operation number mismatch");
                break;
            case 11:
                getAapsLogger().error(LTag.PUMPCOMM, "Low battery, injection not possible");
                break;
            case 12:
                getAapsLogger().error(LTag.PUMPCOMM, "Low insulin, injection not possible");
                break;
            case 13:
                getAapsLogger().error(LTag.PUMPCOMM, "Single dose limit exceeded, injection not possible");
                break;
            case 14:
                getAapsLogger().error(LTag.PUMPCOMM, "Daily injection volume exceeded, injection not possible");
                break;
            case 15:
                getAapsLogger().error(LTag.PUMPCOMM, "Basal injection possible after basal setting completed");
                break;
            default:
                switch (result) {
                    case 32:
                        getAapsLogger().error(LTag.PUMPCOMM, "LGS running, injection restricted");
                        break;
                    case 33:
                        getAapsLogger().error(LTag.PUMPCOMM, "LGS status ON, ON command rejected");
                        break;
                    case 34:
                        getAapsLogger().error(LTag.PUMPCOMM, "LGS status OFF, OFF command rejected");
                        break;
                    case 35:
                        getAapsLogger().error(LTag.PUMPCOMM, "Temp basal running, start command rejected");
                        break;
                    case 36:
                        getAapsLogger().error(LTag.PUMPCOMM, "Temp basal not running, stop command rejected");
                        break;
                    default:
                        getAapsLogger().error(LTag.PUMPCOMM, "System error, cannot set");
                        break;
                }
                break;
        }
        return false;
    }

    /* JADX INFO: compiled from: LenomedPacket.kt */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final byte[] getCRC16CCITT(byte[] pSendBuf, int nEnd) {
            Intrinsics.checkNotNullParameter(pSendBuf, "pSendBuf");
            int i = 65535;
            for (int i2 = 0; i2 < nEnd; i2++) {
                i ^= (pSendBuf[i2] & 255) << 8;
                for (int i3 = 0; i3 < 8; i3++) {
                    i = ((32768 & i) != 0 ? (i << 1) ^ 4129 : i << 1) & 65535;
                }
            }
            return new byte[]{(byte) (((65535 & i) >> 8) & 255), (byte) (i & 255)};
        }

        public final ByteBuffer prefixDecode(byte[] bytes) {
            Intrinsics.checkNotNullParameter(bytes, "bytes");
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(bytes);
            byteBufferWrap.order(ByteOrder.LITTLE_ENDIAN);
            byteBufferWrap.position(1);
            Intrinsics.checkNotNull(byteBufferWrap);
            return byteBufferWrap;
        }

        public final int getByteToInt(ByteBuffer buffer) {
            Intrinsics.checkNotNullParameter(buffer, "buffer");
            return buffer.get() & 255;
        }

        public final int getShortToInt(ByteBuffer buffer) {
            Intrinsics.checkNotNullParameter(buffer, "buffer");
            return buffer.getShort() & UShort.MAX_VALUE;
        }

        public final int getIntToInt(ByteBuffer buffer) {
            Intrinsics.checkNotNullParameter(buffer, "buffer");
            return buffer.getInt();
        }

        public final int defect(byte[] bytes) {
            Intrinsics.checkNotNullParameter(bytes, "bytes");
            if (bytes[0] != 119) {
                return 98;
            }
            byte[] bArrCopyOfRange = java.util.Arrays.copyOfRange(bytes, 0, bytes.length - 2);
            return !Arrays.equals(getCRC16CCITT(bArrCopyOfRange, bArrCopyOfRange.length), new byte[]{bytes[bytes.length - 2], bytes[bytes.length - 1]}) ? 99 : 0;
        }

        public final String toHex(byte[] bytes) {
            Intrinsics.checkNotNullParameter(bytes, "bytes");
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
                String str = String.format("%02X ", Arrays.copyOf(new Object[]{Integer.valueOf(b & 255)}, 1));
                Intrinsics.checkNotNullExpressionValue(str, "format(...)");
                sb.append(str);
            }
            String string = sb.toString();
            Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
            return string;
        }

        public final String toNarrowHex(byte[] packet) {
            Intrinsics.checkNotNullParameter(packet, "packet");
            StringBuilder sb = new StringBuilder();
            for (byte b : packet) {
                StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
                String str = String.format("%02X", Arrays.copyOf(new Object[]{Integer.valueOf(b & 255)}, 1));
                Intrinsics.checkNotNullExpressionValue(str, "format(...)");
                sb.append(str);
            }
            String string = sb.toString();
            Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
            return string;
        }
    }
}