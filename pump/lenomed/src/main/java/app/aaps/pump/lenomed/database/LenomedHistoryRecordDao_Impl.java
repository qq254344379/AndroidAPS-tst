package app.aaps.pump.lenomed.database;

import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.rxjava3.RxRoom;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteConnection;
import androidx.sqlite.SQLiteStatement;
import io.reactivex.rxjava3.core.Single;
import java.util.ArrayList;
import java.util.List;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.reflect.KClass;
import net.openid.appauth.ResponseTypeValues;

/* JADX INFO: compiled from: LenomedHistoryRecordDao_Impl.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0005\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0007\u0018\u0000 \u001d2\u00020\u0001:\u0001\u001dB\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u0010\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\bH\u0016J$\u0010\f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u000e0\r2\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012H\u0016J\u0012\u0010\u0013\u001a\u0004\u0018\u00010\b2\u0006\u0010\u0014\u001a\u00020\u0015H\u0016J\u001f\u0010\u0016\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0018\u001a\u00020\u00122\u0006\u0010\u0014\u001a\u00020\u0015H\u0016¢\u0006\u0002\u0010\u0019J \u0010\u001a\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00122\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u000f\u001a\u00020\u0010H\u0016J \u0010\u001b\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00122\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u001c\u001a\u00020\u0015H\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004¢\u0006\u0002\n\u0000R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007X\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006\u001e"}, d2 = {"Lapp/aaps/pump/lenomed/database/LenomedHistoryRecordDao_Impl;", "Lapp/aaps/pump/lenomed/database/LenomedHistoryRecordDao;", "__db", "Landroidx/room/RoomDatabase;", "<init>", "(Landroidx/room/RoomDatabase;)V", "__insertAdapterOfLenomedHistoryRecord", "Landroidx/room/EntityInsertAdapter;", "Lapp/aaps/pump/lenomed/database/LenomedHistoryRecord;", "createOrUpdate", "", "lenomedHistoryRecord", "allFromByType", "Lio/reactivex/rxjava3/core/Single;", "", "timestamp", "", "type", "", "getLastRecord", "pumpUid", "", "getMaxLogNumForType", "", "logType", "(BLjava/lang/String;)Ljava/lang/Integer;", "countByTimestamp", "countByDateTimeString", "dateTimeString", "Companion", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LenomedHistoryRecordDao_Impl extends LenomedHistoryRecordDao {

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    private final RoomDatabase __db;
    private final EntityInsertAdapter<LenomedHistoryRecord> __insertAdapterOfLenomedHistoryRecord;

    public LenomedHistoryRecordDao_Impl(RoomDatabase __db) {
        Intrinsics.checkNotNullParameter(__db, "__db");
        this.__db = __db;
        this.__insertAdapterOfLenomedHistoryRecord = new EntityInsertAdapter<LenomedHistoryRecord>() { // from class: app.aaps.pump.lenomed.database.LenomedHistoryRecordDao_Impl.1
            @Override // androidx.room.EntityInsertAdapter
            protected String createQuery() {
                return "INSERT OR REPLACE INTO `LenomedHistory` (`timestamp`,`code`,`value`,`bolusType`,`stringValue`,`duration`,`dailyBasal`,`dailyBolus`,`alarm`,`lognum`,`wrappingCount`,`pumpUid`,`refillType`,`basalType`,`dateTimeString`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // androidx.room.EntityInsertAdapter
            public void bind(SQLiteStatement statement, LenomedHistoryRecord entity) {
                Intrinsics.checkNotNullParameter(statement, "statement");
                Intrinsics.checkNotNullParameter(entity, "entity");
                statement.mo9167bindLong(1, entity.getTimestamp());
                statement.mo9167bindLong(2, entity.getCode());
                statement.mo9166bindDouble(3, entity.getValue());
                statement.mo9169bindText(4, entity.getBolusType());
                statement.mo9169bindText(5, entity.getStringValue());
                statement.mo9167bindLong(6, entity.getDuration());
                statement.mo9166bindDouble(7, entity.getDailyBasal());
                statement.mo9166bindDouble(8, entity.getDailyBolus());
                statement.mo9169bindText(9, entity.getAlarm());
                statement.mo9167bindLong(10, entity.getLognum());
                statement.mo9167bindLong(11, entity.getWrappingCount());
                statement.mo9169bindText(12, entity.getPumpUid());
                statement.mo9169bindText(13, entity.getRefillType());
                statement.mo9169bindText(14, entity.getBasalType());
                statement.mo9169bindText(15, entity.getDateTimeString());
            }
        };
    }

    @Override // app.aaps.pump.lenomed.database.LenomedHistoryRecordDao
    public void createOrUpdate(final LenomedHistoryRecord lenomedHistoryRecord) {
        Intrinsics.checkNotNullParameter(lenomedHistoryRecord, "lenomedHistoryRecord");
        DBUtil.performBlocking(this.__db, false, true, new Function1() { // from class: app.aaps.pump.lenomed.database.LenomedHistoryRecordDao_Impl$$ExternalSyntheticLambda3
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return LenomedHistoryRecordDao_Impl.createOrUpdate$lambda$0(this.f$0, lenomedHistoryRecord, (SQLiteConnection) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit createOrUpdate$lambda$0(LenomedHistoryRecordDao_Impl lenomedHistoryRecordDao_Impl, LenomedHistoryRecord lenomedHistoryRecord, SQLiteConnection _connection) {
        Intrinsics.checkNotNullParameter(_connection, "_connection");
        lenomedHistoryRecordDao_Impl.__insertAdapterOfLenomedHistoryRecord.insert(_connection, lenomedHistoryRecord);
        return Unit.INSTANCE;
    }

    @Override // app.aaps.pump.lenomed.database.LenomedHistoryRecordDao
    public Single<List<LenomedHistoryRecord>> allFromByType(final long timestamp, final byte type) {
        final String str = "SELECT * FROM LenomedHistory WHERE timestamp >= ? AND code = ? ORDER BY timestamp DESC";
        return RxRoom.createSingle(this.__db, true, false, new Function1() { // from class: app.aaps.pump.lenomed.database.LenomedHistoryRecordDao_Impl$$ExternalSyntheticLambda2
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return LenomedHistoryRecordDao_Impl.allFromByType$lambda$0(str, timestamp, type, (SQLiteConnection) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final List allFromByType$lambda$0(String str, long j, byte b, SQLiteConnection _connection) {
        Intrinsics.checkNotNullParameter(_connection, "_connection");
        SQLiteStatement sQLiteStatementPrepare = _connection.prepare(str);
        try {
            sQLiteStatementPrepare.mo9167bindLong(1, j);
            sQLiteStatementPrepare.mo9167bindLong(2, b);
            int columnIndexOrThrow = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "timestamp");
            int columnIndexOrThrow2 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, ResponseTypeValues.CODE);
            int columnIndexOrThrow3 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "value");
            int columnIndexOrThrow4 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "bolusType");
            int columnIndexOrThrow5 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "stringValue");
            int columnIndexOrThrow6 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, TypedValues.TransitionType.S_DURATION);
            int columnIndexOrThrow7 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "dailyBasal");
            int columnIndexOrThrow8 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "dailyBolus");
            int columnIndexOrThrow9 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "alarm");
            int columnIndexOrThrow10 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "lognum");
            int columnIndexOrThrow11 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "wrappingCount");
            int columnIndexOrThrow12 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "pumpUid");
            int columnIndexOrThrow13 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "refillType");
            int columnIndexOrThrow14 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "basalType");
            int columnIndexOrThrow15 = SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "dateTimeString");
            ArrayList arrayList = new ArrayList();
            while (sQLiteStatementPrepare.step()) {
                int i = columnIndexOrThrow14;
                ArrayList arrayList2 = arrayList;
                int i2 = columnIndexOrThrow2;
                int i3 = columnIndexOrThrow3;
                int i4 = columnIndexOrThrow15;
                arrayList2.add(new LenomedHistoryRecord(sQLiteStatementPrepare.getLong(columnIndexOrThrow), (byte) sQLiteStatementPrepare.getLong(columnIndexOrThrow2), sQLiteStatementPrepare.getDouble(columnIndexOrThrow3), sQLiteStatementPrepare.getText(columnIndexOrThrow4), sQLiteStatementPrepare.getText(columnIndexOrThrow5), (int) sQLiteStatementPrepare.getLong(columnIndexOrThrow6), sQLiteStatementPrepare.getDouble(columnIndexOrThrow7), sQLiteStatementPrepare.getDouble(columnIndexOrThrow8), sQLiteStatementPrepare.getText(columnIndexOrThrow9), (int) sQLiteStatementPrepare.getLong(columnIndexOrThrow10), (int) sQLiteStatementPrepare.getLong(columnIndexOrThrow11), sQLiteStatementPrepare.getText(columnIndexOrThrow12), sQLiteStatementPrepare.getText(columnIndexOrThrow13), sQLiteStatementPrepare.getText(i), sQLiteStatementPrepare.getText(i4)));
                columnIndexOrThrow2 = i2;
                columnIndexOrThrow15 = i4;
                arrayList = arrayList2;
                columnIndexOrThrow14 = i;
                columnIndexOrThrow3 = i3;
            }
            return arrayList;
        } finally {
            sQLiteStatementPrepare.close();
        }
    }

    @Override // app.aaps.pump.lenomed.database.LenomedHistoryRecordDao
    public LenomedHistoryRecord getLastRecord(final String pumpUid) {
        Intrinsics.checkNotNullParameter(pumpUid, "pumpUid");
        final String str = "SELECT * from LenomedHistory WHERE pumpUid = ? ORDER BY timestamp DESC LIMIT 1";
        return (LenomedHistoryRecord) DBUtil.performBlocking(this.__db, true, false, new Function1() { // from class: app.aaps.pump.lenomed.database.LenomedHistoryRecordDao_Impl$$ExternalSyntheticLambda4
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return LenomedHistoryRecordDao_Impl.getLastRecord$lambda$0(str, pumpUid, (SQLiteConnection) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final LenomedHistoryRecord getLastRecord$lambda$0(String str, String str2, SQLiteConnection _connection) {
        Intrinsics.checkNotNullParameter(_connection, "_connection");
        SQLiteStatement sQLiteStatementPrepare = _connection.prepare(str);
        try {
            sQLiteStatementPrepare.mo9169bindText(1, str2);
            return sQLiteStatementPrepare.step() ? new LenomedHistoryRecord(sQLiteStatementPrepare.getLong(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "timestamp")), (byte) sQLiteStatementPrepare.getLong(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, ResponseTypeValues.CODE)), sQLiteStatementPrepare.getDouble(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "value")), sQLiteStatementPrepare.getText(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "bolusType")), sQLiteStatementPrepare.getText(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "stringValue")), (int) sQLiteStatementPrepare.getLong(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, TypedValues.TransitionType.S_DURATION)), sQLiteStatementPrepare.getDouble(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "dailyBasal")), sQLiteStatementPrepare.getDouble(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "dailyBolus")), sQLiteStatementPrepare.getText(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "alarm")), (int) sQLiteStatementPrepare.getLong(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "lognum")), (int) sQLiteStatementPrepare.getLong(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "wrappingCount")), sQLiteStatementPrepare.getText(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "pumpUid")), sQLiteStatementPrepare.getText(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "refillType")), sQLiteStatementPrepare.getText(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "basalType")), sQLiteStatementPrepare.getText(SQLiteStatementUtil.getColumnIndexOrThrow(sQLiteStatementPrepare, "dateTimeString"))) : null;
        } finally {
            sQLiteStatementPrepare.close();
        }
    }

    @Override // app.aaps.pump.lenomed.database.LenomedHistoryRecordDao
    public Integer getMaxLogNumForType(final byte logType, final String pumpUid) {
        Intrinsics.checkNotNullParameter(pumpUid, "pumpUid");
        final String str = "SELECT MAX(lognum) FROM LenomedHistory WHERE code = ? AND pumpUid = ?";
        return (Integer) DBUtil.performBlocking(this.__db, true, false, new Function1() { // from class: app.aaps.pump.lenomed.database.LenomedHistoryRecordDao_Impl$$ExternalSyntheticLambda5
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return LenomedHistoryRecordDao_Impl.getMaxLogNumForType$lambda$0(str, logType, pumpUid, (SQLiteConnection) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Integer getMaxLogNumForType$lambda$0(String str, byte b, String str2, SQLiteConnection _connection) {
        Intrinsics.checkNotNullParameter(_connection, "_connection");
        SQLiteStatement sQLiteStatementPrepare = _connection.prepare(str);
        try {
            sQLiteStatementPrepare.mo9167bindLong(1, b);
            sQLiteStatementPrepare.mo9169bindText(2, str2);
            Integer numValueOf = null;
            if (sQLiteStatementPrepare.step() && !sQLiteStatementPrepare.isNull(0)) {
                numValueOf = Integer.valueOf((int) sQLiteStatementPrepare.getLong(0));
            }
            return numValueOf;
        } finally {
            sQLiteStatementPrepare.close();
        }
    }

    @Override // app.aaps.pump.lenomed.database.LenomedHistoryRecordDao
    public int countByTimestamp(final byte logType, final String pumpUid, final long timestamp) {
        Intrinsics.checkNotNullParameter(pumpUid, "pumpUid");
        final String str = "SELECT COUNT(*) FROM LenomedHistory WHERE code = ? AND pumpUid = ? AND timestamp = ?";
        return ((Number) DBUtil.performBlocking(this.__db, true, false, new Function1() { // from class: app.aaps.pump.lenomed.database.LenomedHistoryRecordDao_Impl$$ExternalSyntheticLambda1
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return Integer.valueOf(LenomedHistoryRecordDao_Impl.countByTimestamp$lambda$0(str, logType, pumpUid, timestamp, (SQLiteConnection) obj));
            }
        })).intValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final int countByTimestamp$lambda$0(String str, byte b, String str2, long j, SQLiteConnection _connection) {
        Intrinsics.checkNotNullParameter(_connection, "_connection");
        SQLiteStatement sQLiteStatementPrepare = _connection.prepare(str);
        try {
            sQLiteStatementPrepare.mo9167bindLong(1, b);
            sQLiteStatementPrepare.mo9169bindText(2, str2);
            sQLiteStatementPrepare.mo9167bindLong(3, j);
            return sQLiteStatementPrepare.step() ? (int) sQLiteStatementPrepare.getLong(0) : 0;
        } finally {
            sQLiteStatementPrepare.close();
        }
    }

    @Override // app.aaps.pump.lenomed.database.LenomedHistoryRecordDao
    public int countByDateTimeString(final byte logType, final String pumpUid, final String dateTimeString) {
        Intrinsics.checkNotNullParameter(pumpUid, "pumpUid");
        Intrinsics.checkNotNullParameter(dateTimeString, "dateTimeString");
        final String str = "SELECT COUNT(*) FROM LenomedHistory WHERE code = ? AND pumpUid = ? AND dateTimeString = ?";
        return ((Number) DBUtil.performBlocking(this.__db, true, false, new Function1() { // from class: app.aaps.pump.lenomed.database.LenomedHistoryRecordDao_Impl$$ExternalSyntheticLambda0
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return Integer.valueOf(LenomedHistoryRecordDao_Impl.countByDateTimeString$lambda$0(str, logType, pumpUid, dateTimeString, (SQLiteConnection) obj));
            }
        })).intValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final int countByDateTimeString$lambda$0(String str, byte b, String str2, String str3, SQLiteConnection _connection) {
        Intrinsics.checkNotNullParameter(_connection, "_connection");
        SQLiteStatement sQLiteStatementPrepare = _connection.prepare(str);
        try {
            sQLiteStatementPrepare.mo9167bindLong(1, b);
            sQLiteStatementPrepare.mo9169bindText(2, str2);
            sQLiteStatementPrepare.mo9169bindText(3, str3);
            return sQLiteStatementPrepare.step() ? (int) sQLiteStatementPrepare.getLong(0) : 0;
        } finally {
            sQLiteStatementPrepare.close();
        }
    }

    /* JADX INFO: compiled from: LenomedHistoryRecordDao_Impl.kt */
    @Metadata(d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u0010\u0010\u0004\u001a\f\u0012\b\u0012\u0006\u0012\u0002\b\u00030\u00060\u0005¨\u0006\u0007"}, d2 = {"Lapp/aaps/pump/lenomed/database/LenomedHistoryRecordDao_Impl$Companion;", "", "<init>", "()V", "getRequiredConverters", "", "Lkotlin/reflect/KClass;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final List<KClass<?>> getRequiredConverters() {
            return CollectionsKt.emptyList();
        }
    }
}