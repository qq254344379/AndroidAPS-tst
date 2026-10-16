package app.aaps.pump.lenomed.common;

import kotlin.Metadata;

/* JADX INFO: compiled from: RecordTypes.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u0005\n\u0002\b\b\bÆ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003R\u000e\u0010\u0004\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000¨\u0006\r"}, d2 = {"Lapp/aaps/pump/lenomed/common/RecordTypes;", "", "<init>", "()V", "RECORD_TYPE_BASAL", "", "RECORD_TYPE_DAILY", "RECORD_TYPE_ALARM", "RECORD_TYPE_BOLUS", "RECORD_TYPE_EXTENDED_BOLUS", "RECORD_TYPE_REFILL", "RECORD_TYPE_BASAL_HOURLY", "RECORD_TYPE_TB", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class RecordTypes {
    public static final RecordTypes INSTANCE = new RecordTypes();
    public static final byte RECORD_TYPE_ALARM = 3;
    public static final byte RECORD_TYPE_BASAL = 1;
    public static final byte RECORD_TYPE_BASAL_HOURLY = 7;
    public static final byte RECORD_TYPE_BOLUS = 4;
    public static final byte RECORD_TYPE_DAILY = 2;
    public static final byte RECORD_TYPE_EXTENDED_BOLUS = 5;
    public static final byte RECORD_TYPE_REFILL = 6;
    public static final byte RECORD_TYPE_TB = 8;

    private RecordTypes() {
    }
}