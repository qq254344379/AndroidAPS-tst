package app.aaps.pump.lenomed.packet;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.pump.lenomed.LenomedPump;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.StringCompanionObject;
import kotlin.ranges.IntRange;

/* JADX INFO: compiled from: BasalLimitInquireResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class BasalLimitInquireResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;
    private int result;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public BasalLimitInquireResponsePacket() {
        super();
        setMsgType((byte) -92);
        getAapsLogger().debug(LTag.PUMPCOMM, "BasalLimitInquireResponsePacket init");
        this.friendlyName = "PUMP_BASAL_LIMIT_INQUIRE_RESPONSE";
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

    public final int getResult() {
        return this.result;
    }

    public final void setResult(int i) {
        this.result = i;
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
        int iDefect = LenomedPacket.INSTANCE.defect(data);
        if (iDefect != 0) {
            getAapsLogger().debug(LTag.PUMPCOMM, "BasalLimitInquireResponsePacket Got some Error: " + iDefect);
            setFailed(true);
            return;
        }
        int i = 0;
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        try {
            int byteToInt = getByteToInt(byteBufferPrefixDecode);
            if (1 > byteToInt || byteToInt >= 3) {
                getAapsLogger().error(LTag.PUMPCOMM, "Invalid pattern: " + byteToInt + ", expected 1 (Basal A) or 2 (Basal B)");
                setFailed(true);
                return;
            }
            getLenomedPump().setSystemBasePattern(byteToInt);
            this.result = byteToInt;
            int byteToInt2 = getByteToInt(byteBufferPrefixDecode);
            if (byteToInt2 != 48) {
                getAapsLogger().error(LTag.PUMPCOMM, "Invalid segment count: " + byteToInt2 + ", expected 0x30 (48 segments)");
                setFailed(true);
                return;
            }
            int i2 = 24;
            if (getLenomedPump().getPumpProfiles() == null) {
                LenomedPump lenomedPump = getLenomedPump();
                Double[][] dArr = new Double[4][];
                for (int i3 = 0; i3 < 4; i3++) {
                    Double[] dArr2 = new Double[24];
                    for (int i4 = 0; i4 < 24; i4++) {
                        dArr2[i4] = Double.valueOf(0.0d);
                    }
                    dArr[i3] = dArr2;
                }
                lenomedPump.setPumpProfiles(dArr);
            }
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            ArrayList arrayList3 = new ArrayList();
            int i5 = 0;
            for (int i6 = 48; i5 < i6; i6 = 48) {
                int byteToInt3 = getByteToInt(byteBufferPrefixDecode);
                int byteToInt4 = getByteToInt(byteBufferPrefixDecode);
                int byteToInt5 = getByteToInt(byteBufferPrefixDecode);
                int byteToInt6 = getByteToInt(byteBufferPrefixDecode);
                int i7 = ((byteToInt3 >> 4) * 10) + (byteToInt3 & 15);
                int i8 = ((byteToInt4 >> 4) * 10) + (byteToInt4 & 15);
                if (i7 >= 0 && i7 < i2) {
                    Integer[] numArr = new Integer[2];
                    numArr[i] = Integer.valueOf(i);
                    numArr[1] = 30;
                    if (SetsKt.setOf((Object[]) numArr).contains(Integer.valueOf(i8))) {
                        double d = ((double) byteToInt5) + (((double) byteToInt6) / 100.0d);
                        arrayList.add(Double.valueOf(d));
                        StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
                        String str = String.format("%02d:%02d", Arrays.copyOf(new Object[]{Integer.valueOf(i7), Integer.valueOf(i8)}, 2));
                        Intrinsics.checkNotNullExpressionValue(str, "format(...)");
                        arrayList2.add(str);
                        if (i8 == 0) {
                            arrayList3.add(Double.valueOf(d));
                        }
                        i5++;
                        i2 = 24;
                        i = 0;
                    }
                }
                getAapsLogger().error(LTag.PUMPCOMM, "Invalid time in segment " + i5 + ": " + i7 + ":" + i8);
                setFailed(true);
                return;
            }
            if (arrayList3.size() != 24) {
                getAapsLogger().error(LTag.PUMPCOMM, "Invalid hourly rates count: " + arrayList3.size() + ", expected 24");
                setFailed(true);
                return;
            }
            int i9 = byteToInt - 1;
            for (int i10 = 0; i10 < 24; i10++) {
                Object[][] pumpProfiles = getLenomedPump().getPumpProfiles();
                Intrinsics.checkNotNull(pumpProfiles);
                pumpProfiles[i9][i10] = arrayList3.get(i10);
            }
            StringBuilder times = new StringBuilder();
            for (int hour = 0; hour <= 23; hour++) {
                if (times.length() > 0) {
                    times.append(", ");
                }
                times.append(String.format("%02d:00", hour));
            }
            getAapsLogger().debug(LTag.PUMPCOMM, "Result --> pattern: " + byteToInt + " (Basal " + (byteToInt == 1 ? "" : "B") + "), hourly rates: " + arrayList3 + ", times: [" + times + "]");
            getAapsLogger().debug(LTag.PUMPCOMM, "Full 48-segment rates for reference: " + arrayList + ", times: " + arrayList2);
            setFailed(false);
        } catch (Exception e) {
            getAapsLogger().error(LTag.PUMPCOMM, "Error parsing payload: " + e.getMessage());
            setFailed(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final CharSequence handleMessage$lambda$0(int i) {
        StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
        String str = String.format("%02d:00", Arrays.copyOf(new Object[]{Integer.valueOf(i)}, 1));
        Intrinsics.checkNotNullExpressionValue(str, "format(...)");
        return str;
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}