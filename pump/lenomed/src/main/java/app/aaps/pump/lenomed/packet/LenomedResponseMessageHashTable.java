package app.aaps.pump.lenomed.packet;

import java.util.HashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LenomedResponseMessageHashTable.kt */
/* JADX INFO: loaded from: classes4.dex */
@Singleton
@Metadata(d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0011\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u000e\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\tJ\u000e\u0010\u0012\u001a\u00020\t2\u0006\u0010\u0013\u001a\u00020\bR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004¢\u0006\u0002\n\u0000R6\u0010\u0006\u001a\u001e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\t0\u0007j\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\t`\nX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000e¨\u0006\u0014"}, d2 = {"Lapp/aaps/pump/lenomed/packet/LenomedResponseMessageHashTable;", "", "injector", "Ldagger/android/HasAndroidInjector;", "<init>", "(Ldagger/android/HasAndroidInjector;)V", "messages", "Ljava/util/HashMap;", "", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "Lkotlin/collections/HashMap;", "getMessages", "()Ljava/util/HashMap;", "setMessages", "(Ljava/util/HashMap;)V", "put", "", "message", "findMessage", "command", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LenomedResponseMessageHashTable {
        private HashMap<Integer, LenomedPacket> messages;

    @Inject
    public LenomedResponseMessageHashTable() {
        this.messages = new HashMap<>();
        put(new SerialNumInquireResponsePacket());
        put(new BigAPSMainInfoInquireResponsePacket());
        put(new BasalModeInquireResponsePacket());
        put(new BasalLimitInquireResponsePacket());
        put(new TempBasalInquireResponsePacket());
        put(new DailyTotalInquireResponsePacket());
        put(new TimeInquireResponsePacket());
        put(new BigLogInquireResponsePacket());
        put(new BolusStatusInquireResponsePacket());
        put(new InjectionSnackStopResponsePacket());
        put(new ThresholdInquireResponsePacket());
    }

    public final HashMap<Integer, LenomedPacket> getMessages() {
        return this.messages;
    }

    public final void setMessages(HashMap<Integer, LenomedPacket> map) {
        Intrinsics.checkNotNullParameter(map, "<set-?>");
        this.messages = map;
    }

    public final void put(LenomedPacket message) {
        Intrinsics.checkNotNullParameter(message, "message");
        this.messages.put(Integer.valueOf(message.getMsgType() & 255), message);
    }

    public final LenomedPacket findMessage(int command) {
        LenomedPacket lenomedPacket = this.messages.get(Integer.valueOf(command));
        return lenomedPacket == null ? new LenomedPacket(this.injector) : lenomedPacket;
    }
}