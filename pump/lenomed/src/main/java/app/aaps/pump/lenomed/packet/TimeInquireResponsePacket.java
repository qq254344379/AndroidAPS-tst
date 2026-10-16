package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedSupportKt;
import app.aaps.pump.lenomed.LenomedPump;
import java.util.Arrays;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.StringCompanionObject;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

/* JADX INFO: compiled from: TimeInquireResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class TimeInquireResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public TimeInquireResponsePacket() {
        super();
        setMsgType((byte) -90);
        getAapsLogger().debug(LTag.PUMPBTCOMM, "TimeInquireResponsePacket init");
        this.friendlyName = "PUMP_TIME_INQUIRE_RESPONSE";
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
    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
        if (LenomedPacket.INSTANCE.defect(data) != 0) {
            getAapsLogger().debug(LTag.PUMPBTCOMM, "TimeInquireResponsePacket Got some Error");
            setFailed(true);
            return;
        }
        setFailed(false);
        try {
            byte[] bArr = new byte[6];
            LenomedPacket.INSTANCE.prefixDecode(data).get(bArr);
            String strJoinToString$default = LenomedSupportKt.bytesToHexString(bArr, "");
            getAapsLogger().debug(LTag.PUMPBTCOMM, "Raw time hex: " + strJoinToString$default);
            String strSubstring = strJoinToString$default.substring(0, 2);
            Intrinsics.checkNotNullExpressionValue(strSubstring, "substring(...)");
            int i = Integer.parseInt(strSubstring) + 2000;
            String strSubstring2 = strJoinToString$default.substring(2, 4);
            Intrinsics.checkNotNullExpressionValue(strSubstring2, "substring(...)");
            int i2 = Integer.parseInt(strSubstring2);
            String strSubstring3 = strJoinToString$default.substring(4, 6);
            Intrinsics.checkNotNullExpressionValue(strSubstring3, "substring(...)");
            int i3 = Integer.parseInt(strSubstring3);
            String strSubstring4 = strJoinToString$default.substring(6, 8);
            Intrinsics.checkNotNullExpressionValue(strSubstring4, "substring(...)");
            int i4 = Integer.parseInt(strSubstring4);
            String strSubstring5 = strJoinToString$default.substring(8, 10);
            Intrinsics.checkNotNullExpressionValue(strSubstring5, "substring(...)");
            int i5 = Integer.parseInt(strSubstring5);
            String strSubstring6 = strJoinToString$default.substring(10, 12);
            Intrinsics.checkNotNullExpressionValue(strSubstring6, "substring(...)");
            int i6 = Integer.parseInt(strSubstring6);
            getAapsLogger().debug(LTag.PUMPBTCOMM, "Parsed time: " + i + "-" + i2 + "-" + i3 + StringUtils.SPACE + i4 + ":" + i5 + ":" + i6);
            getLenomedPump().setYear(i);
            getLenomedPump().setMonth(i2);
            getLenomedPump().setDay(i3);
            getLenomedPump().setHour(i4);
            getLenomedPump().setMinute(i5);
            getLenomedPump().setSecond(i6);
            getLenomedPump().setPumpTime(new DateTime(getLenomedPump().getYear(), getLenomedPump().getMonth(), getLenomedPump().getDay(), getLenomedPump().getHour(), getLenomedPump().getMinute(), getLenomedPump().getSecond()).getMillis());
            StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
            String str = String.format("%04d-%02d-%02d %02d:%02d:%02d", Arrays.copyOf(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5), Integer.valueOf(i6)}, 6));
            Intrinsics.checkNotNullExpressionValue(str, "format(...)");
            getAapsLogger().debug(LTag.PUMPBTCOMM, "Parsed time: " + str);
            setFailed(false);
        } catch (Exception e) {
            getAapsLogger().error(LTag.PUMPBTCOMM, "Error parsing payload: " + e.getMessage());
            setFailed(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final CharSequence handleMessage$lambda$0(byte b) {
        String str = String.format("%02X", Arrays.copyOf(new Object[]{Integer.valueOf(b & 255)}, 1));
        Intrinsics.checkNotNullExpressionValue(str, "format(...)");
        return str;
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}