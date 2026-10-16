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

/* JADX INFO: compiled from: BasalLowerHalfSettingPacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class BasalLowerHalfSettingPacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private final List<Double> lowerRates;
    private final int pattern;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BasalLowerHalfSettingPacket( int i, List<Double> lowerRates) {
        super();
        Intrinsics.checkNotNullParameter(lowerRates, "lowerRates");
        this.pattern = i;
        this.lowerRates = lowerRates;
        if (lowerRates.size() != 12) {
            throw new IllegalArgumentException("Exactly 12 basal rates required for lower half (12-23 hours)");
        }
        if (1 > i || i >= 3) {
            throw new IllegalArgumentException("Pattern must be 1 or 2");
        }
        setMsgType((byte) -73);
        getAapsLogger().debug(LTag.PUMPCOMM, "BasalLowerHalfSettingPacket init - pattern: " + i + ", lower 12 hours");
        this.friendlyName = "PUMP_BASAL_LOWER_HALF_SETTING";
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
        ByteBuffer byteBufferPrefixEncode = prefixEncode(getMsgType(), 12, (byte) 17);
        byteBufferPrefixEncode.put((byte) 18);
        int i = 0;
        for (Object obj : this.lowerRates) {
            int i2 = i + 1;
            if (i < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            double dDoubleValue = ((Number) obj).doubleValue();
            byteBufferPrefixEncode.put((byte) (((double) 10) * dDoubleValue));
            AAPSLogger aapsLogger = getAapsLogger();
            LTag lTag = LTag.PUMPCOMM;
            StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
            String str = String.format("%02d-%02d", Arrays.copyOf(new Object[]{Integer.valueOf(i + 12), Integer.valueOf(i + 13)}, 2));
            Intrinsics.checkNotNullExpressionValue(str, "format(...)");
            aapsLogger.debug(lTag, "Lower segment " + i + ": " + str + " → " + dDoubleValue + " U/h");
            i = i2;
        }
        return suffixEncode(byteBufferPrefixEncode);
    }

    private final byte toBCD(int value) {
        if (value < 0 || value >= 100) {
            throw new IllegalArgumentException(("BCD value must be 0-99, got " + value).toString());
        }
        return (byte) (((value / 10) << 4) | (value % 10));
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}