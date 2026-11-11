package app.aaps.pump.lenomed.packet;

import java.util.HashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LenomedResponseMessageHashTable.kt */
/* JADX INFO: loaded from: classes4.dex */
@Singleton
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
        return lenomedPacket == null ? new LenomedPacket() : lenomedPacket;
    }
}