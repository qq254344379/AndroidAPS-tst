package app.aaps.pump.lenomed.packet;

import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.sharedPreferences.SP;
import app.aaps.pump.lenomed.LenomedPump;
import app.aaps.pump.lenomed.R;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: ThresholdInquireResponsePacket.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u0012\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u0010\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u001bH\u0017R\u001e\u0010\u0006\u001a\u00020\u00078\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u001e\u0010\f\u001a\u00020\r8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u000e\u0010\u000f\"\u0004\b\u0010\u0010\u0011R\u001e\u0010\u0012\u001a\u00020\u00138\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0014\u0010\u0015\"\u0004\b\u0016\u0010\u0017R\u0014\u0010\u001c\u001a\u00020\u001dX\u0096D¢\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001f¨\u0006 "}, d2 = {"Lapp/aaps/pump/lenomed/packet/ThresholdInquireResponsePacket;", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "injector", "Ldagger/android/HasAndroidInjector;", "<init>", "(Ldagger/android/HasAndroidInjector;)V", "lenomedPump", "Lapp/aaps/pump/lenomed/LenomedPump;", "getLenomedPump", "()Lapp/aaps/pump/lenomed/LenomedPump;", "setLenomedPump", "(Lapp/aaps/pump/lenomed/LenomedPump;)V", "sp", "Lapp/aaps/core/interfaces/sharedPreferences/SP;", "getSp", "()Lapp/aaps/core/interfaces/sharedPreferences/SP;", "setSp", "(Lapp/aaps/core/interfaces/sharedPreferences/SP;)V", "rh", "Lapp/aaps/core/interfaces/resources/ResourceHelper;", "getRh", "()Lapp/aaps/core/interfaces/resources/ResourceHelper;", "setRh", "(Lapp/aaps/core/interfaces/resources/ResourceHelper;)V", "handleMessage", "", "data", "", "friendlyName", "", "getFriendlyName", "()Ljava/lang/String;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class ThresholdInquireResponsePacket extends LenomedPacket {
    private final String friendlyName;

    @Inject
    public LenomedPump lenomedPump;

    @Inject
    public ResourceHelper rh;

    @Inject
    public SP sp;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public ThresholdInquireResponsePacket() {
        super();
        setMsgType((byte) -79);
        getAapsLogger().debug(LTag.PUMPCOMM, "ThresholdInquireResponsePacket init");
        this.friendlyName = "PUMP_THRESHOLD_INQUIRE_RESPONSE";
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

    public final SP getSp() {
        SP sp = this.sp;
        if (sp != null) {
            return sp;
        }
        Intrinsics.throwUninitializedPropertyAccessException("sp");
        return null;
    }

    public final void setSp(SP sp) {
        Intrinsics.checkNotNullParameter(sp, "<set-?>");
        this.sp = sp;
    }

    public final ResourceHelper getRh() {
        ResourceHelper resourceHelper = this.rh;
        if (resourceHelper != null) {
            return resourceHelper;
        }
        Intrinsics.throwUninitializedPropertyAccessException("rh");
        return null;
    }

    public final void setRh(ResourceHelper resourceHelper) {
        Intrinsics.checkNotNullParameter(resourceHelper, "<set-?>");
        this.rh = resourceHelper;
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public void handleMessage(byte[] data) {
        Intrinsics.checkNotNullParameter(data, "data");
        int iDefect = LenomedPacket.INSTANCE.defect(data);
        if (iDefect != 0) {
            getAapsLogger().error(LTag.PUMPCOMM, "ThresholdInquireResponsePacket Got some Error: " + iDefect);
            setFailed(true);
            return;
        }
        setFailed(false);
        ByteBuffer byteBufferPrefixDecode = LenomedPacket.INSTANCE.prefixDecode(data);
        try {
            getLenomedPump().setMaxBolus(getByteToInt(byteBufferPrefixDecode));
            getAapsLogger().debug(LTag.PUMPCOMM, "maxBolus: " + getLenomedPump().getMaxBolus() + " U");
            getLenomedPump().setMaxBolusePerDay(getByteToInt(byteBufferPrefixDecode));
            getAapsLogger().debug(LTag.PUMPCOMM, "maxBolusePerDay: " + getLenomedPump().getMaxBolusePerDay() + " U");
            getLenomedPump().setMaxBasal(getByteToInt(byteBufferPrefixDecode));
            getAapsLogger().debug(LTag.PUMPCOMM, "maxBasal: " + getLenomedPump().getMaxBasal() + " U/h");
            getLenomedPump().setBasalDailyTotalThreshold(getByteToInt(byteBufferPrefixDecode));
            getAapsLogger().debug(LTag.PUMPCOMM, "basalDailyTotalThreshold: " + getLenomedPump().getBasalDailyTotalThreshold() + " U");
            getLenomedPump().setFixedExhaustThreshold(getByteToInt(byteBufferPrefixDecode));
            getAapsLogger().debug(LTag.PUMPCOMM, "fixedExhaustThreshold: " + getLenomedPump().getFixedExhaustThreshold() + " U");
            getSp().putDouble(R.string.key_lenomed_bolus_single, getLenomedPump().getMaxBolus());
            getSp().putDouble(R.string.key_lenomed_bolus_daily_total_threshold, getLenomedPump().getMaxBolusePerDay());
            getSp().putDouble(R.string.key_lenomed_basal_single, getLenomedPump().getMaxBasal());
            getSp().putDouble(R.string.key_lenomed_basal_daily_total_threshold, getLenomedPump().getBasalDailyTotalThreshold());
            getSp().putDouble(R.string.key_lenomed_fixed_exhaust_threshold, getLenomedPump().getFixedExhaustThreshold());
            setFailed(false);
        } catch (Exception e) {
            getAapsLogger().error(LTag.PUMPCOMM, "Error parsing payload: " + e.getMessage());
            setFailed(true);
        }
    }

    @Override // app.aaps.pump.lenomed.packet.LenomedPacket
    public String getFriendlyName() {
        return this.friendlyName;
    }
}