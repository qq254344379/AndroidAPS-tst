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
@Metadata(d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0012\n\u0002\b\u0002\n\u0002\u0010\u0005\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B%\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007¢\u0006\u0004\b\b\u0010\tJ\u0010\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u0005H\u0017J\u0010\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0005H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u000e¢\u0006\u0002\n\u0000R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007X\u0082\u000e¢\u0006\u0002\n\u0000R\u001e\u0010\n\u001a\u00020\u000b8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u0014\u0010\u0016\u001a\u00020\u0017X\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019¨\u0006\u001a"}, d2 = {"Lapp/aaps/pump/lenomed/packet/BasalSettingPacket;", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "injector", "Ldagger/android/HasAndroidInjector;", "pattern", "", "segments", "", "<init>", "(Ldagger/android/HasAndroidInjector;ILjava/util/List;)V", "lenomedPump", "Lapp/aaps/pump/lenomed/LenomedPump;", "getLenomedPump", "()Lapp/aaps/pump/lenomed/LenomedPump;", "setLenomedPump", "(Lapp/aaps/pump/lenomed/LenomedPump;)V", "encode", "", "msgSeq", "toBCD", "", "value", "friendlyName", "", "getFriendlyName", "()Ljava/lang/String;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
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