package app.aaps.pump.lenomed.common;

import kotlin.Metadata;

/* JADX INFO: compiled from: RecordTypes.kt */
/* JADX INFO: loaded from: classes4.dex */
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