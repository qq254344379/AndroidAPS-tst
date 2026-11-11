package app.aaps.pump.lenomed.packet;

import java.util.HashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LenomedSettingResponseMessageHashTable.kt */
/* JADX INFO: loaded from: classes4.dex */
@Singleton
public final class LenomedSettingResponseMessageHashTable {
        private HashMap<Integer, LenomedPacket> messages;

    @Inject
    public LenomedSettingResponseMessageHashTable() {
        this.messages = new HashMap<>();
        put(new BasalSettingResponsePacket());
        put(new TempBasalSettingResponsePacket());
        put(new TimeSettingResponsePacket());
        put(new InjectionSnackSettingResponsePacket());
        put(new InjectionCancelSettingResponsePacket());
        put(new ThresholdSettingResponsePacket());
        put(new LenomedInfusionSwitchResponsePacket());
        put(new BluetoothSwitchResponsePacket());
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