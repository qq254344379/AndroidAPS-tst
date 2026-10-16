package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.StringCompanionObject;

/* JADX INFO: compiled from: BasalSettingPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class BasalSettingPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private int pattern;
    private List<Integer> segments;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BasalSettingPacket( int i, List<Integer> segments) {
        super();
        Intrinsics.checkNotNullParameter(segments, "segments");
        this.pattern = i;
        this.segments = segments;
        setMsgType((byte) -77);
        getAapsLogger().debug(LTag.PUMPCOMM, "Setting new basal rates for profile with pattern " + this.pattern);
        this.friendlyName = "PUMP_BASAL_SETTING";
    }

    public final LenomedPump getLenomedPump() {
        LenomedPump lenomedPump = this.lenomedPump;
        if (lenomedPump != null) {
            return lenomedPump;
        }
        Intrinsics.throwUninitializedPropertyAccessException("lenomedPump");
        return null;
    }

    public final void setLenomedPump(LenomedPump lenomedPump) {
        Intrinsics.checkNotNullParameter(lenomedPump, "<set-?>");
        this.lenomedPump = lenomedPump;
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public byte[] encode(int msgSeq) {
        ByteBuffer byteBufferPrefixEncode$default = LenomedPacket.prefixEncode$default(this, getMsgType(), (this.segments.size() * 4) + 2, (byte) 0, 4, null);
        byteBufferPrefixEncode$default.put((byte) this.pattern);
        byteBufferPrefixEncode$default.put((byte) 48);
        int i = 0;
        for (Object obj : this.segments) {
            int i2 = i + 1;
            if (i < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            int iIntValue = ((Number) obj).intValue();
            int i3 = i / 2;
            int i4 = i % 2 == 0 ? 0 : 30;
            byteBufferPrefixEncode$default.put(toBCD(i3));
            byteBufferPrefixEncode$default.put(toBCD(i4));
            int i5 = iIntValue / 100;
            int i6 = iIntValue % 100;
            byteBufferPrefixEncode$default.put((byte) i5);
            byteBufferPrefixEncode$default.put((byte) i6);
            AAPSLogger aapsLogger = getAapsLogger();
            LTag lTag = LTag.PUMPCOMM;
            StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
            String str = String.format("%02d:%02d", Arrays.copyOf(new Object[]{Integer.valueOf(i3), Integer.valueOf(i4)}, 2));
            Intrinsics.checkNotNullExpressionValue(str, "format(...)");
            aapsLogger.debug(lTag, "Segment " + i + ": " + str + " --> " + i5 + "." + i6 + " U");
            i = i2;
        }
        return suffixEncode(byteBufferPrefixEncode$default);
    }

    private final byte toBCD(int value) {
        if (value < 0 || value >= 100) {
            throw new IllegalArgumentException(("Value must be between 0 and 99, got " + value).toString());
        }
        return (byte) (((value / 10) << 4) | (value % 10));
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}