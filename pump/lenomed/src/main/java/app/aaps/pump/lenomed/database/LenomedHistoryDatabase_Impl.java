package app.aaps.pump.lenomed.database;

import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.room.InvalidationTracker;
import androidx.room.RoomMasterTable;
import androidx.room.RoomOpenDelegate;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.SQLite;
import androidx.sqlite.SQLiteConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Reflection;
import kotlin.reflect.KClass;
import net.openid.appauth.ResponseTypeValues;

/* JADX INFO: compiled from: LenomedHistoryDatabase_Impl.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0010\"\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\b\u0010\u0007\u001a\u00020\bH\u0014J\b\u0010\t\u001a\u00020\nH\u0014J\b\u0010\u000b\u001a\u00020\fH\u0016J\"\u0010\r\u001a\u001c\u0012\b\u0012\u0006\u0012\u0002\b\u00030\u000f\u0012\u000e\u0012\f\u0012\b\u0012\u0006\u0012\u0002\b\u00030\u000f0\u00100\u000eH\u0014J\u0016\u0010\u0011\u001a\u0010\u0012\f\u0012\n\u0012\u0006\b\u0001\u0012\u00020\u00130\u000f0\u0012H\u0016J*\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u00102\u001a\u0010\u0016\u001a\u0016\u0012\f\u0012\n\u0012\u0006\b\u0001\u0012\u00020\u00130\u000f\u0012\u0004\u0012\u00020\u00130\u000eH\u0016J\b\u0010\u0017\u001a\u00020\u0006H\u0016R\u0014\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006\u0018"}, d2 = {"Lapp/aaps/pump/lenomed/database/LenomedHistoryDatabase_Impl;", "Lapp/aaps/pump/lenomed/database/LenomedHistoryDatabase;", "<init>", "()V", "_lenomedHistoryRecordDao", "Lkotlin/Lazy;", "Lapp/aaps/pump/lenomed/database/LenomedHistoryRecordDao;", "createOpenDelegate", "Landroidx/room/RoomOpenDelegate;", "createInvalidationTracker", "Landroidx/room/InvalidationTracker;", "clearAllTables", "", "getRequiredTypeConverterClasses", "", "Lkotlin/reflect/KClass;", "", "getRequiredAutoMigrationSpecClasses", "", "Landroidx/room/migration/AutoMigrationSpec;", "createAutoMigrations", "Landroidx/room/migration/Migration;", "autoMigrationSpecs", "historyRecordDao", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LenomedHistoryDatabase_Impl extends LenomedHistoryDatabase {
    private final Lazy<LenomedHistoryRecordDao> _lenomedHistoryRecordDao = LazyKt.lazy(new Function0() { // from class: app.aaps.pump.lenomed.database.LenomedHistoryDatabase_Impl$$ExternalSyntheticLambda0
        @Override // kotlin.jvm.functions.Function0
        public final Object invoke() {
            return LenomedHistoryDatabase_Impl._lenomedHistoryRecordDao$lambda$0(this.f$0);
        }
    });

    /* JADX INFO: Access modifiers changed from: private */
    public static final LenomedHistoryRecordDao_Impl _lenomedHistoryRecordDao$lambda$0(LenomedHistoryDatabase_Impl lenomedHistoryDatabase_Impl) {
        return new LenomedHistoryRecordDao_Impl(lenomedHistoryDatabase_Impl);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.room.RoomDatabase
    public RoomOpenDelegate createOpenDelegate() {
        return new RoomOpenDelegate() { // from class: app.aaps.pump.lenomed.database.LenomedHistoryDatabase_Impl$createOpenDelegate$_openDelegate$1
            @Override // androidx.room.RoomOpenDelegate
            public void onCreate(SQLiteConnection connection) {
                Intrinsics.checkNotNullParameter(connection, "connection");
            }

            @Override // androidx.room.RoomOpenDelegate
            public void onPostMigrate(SQLiteConnection connection) {
                Intrinsics.checkNotNullParameter(connection, "connection");
            }

            {
                super(3, "58ceb041455b2ad68db6e8d18809d0da", "039ad8f05685608dd59c479871b05d73");
            }

            @Override // androidx.room.RoomOpenDelegate
            public void createAllTables(SQLiteConnection connection) {
                Intrinsics.checkNotNullParameter(connection, "connection");
                SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `LenomedHistory` (`timestamp` INTEGER NOT NULL, `code` INTEGER NOT NULL, `value` REAL NOT NULL, `bolusType` TEXT NOT NULL, `stringValue` TEXT NOT NULL, `duration` INTEGER NOT NULL, `dailyBasal` REAL NOT NULL, `dailyBolus` REAL NOT NULL, `alarm` TEXT NOT NULL, `lognum` INTEGER NOT NULL, `wrappingCount` INTEGER NOT NULL, `pumpUid` TEXT NOT NULL, `refillType` TEXT NOT NULL, `basalType` TEXT NOT NULL, `dateTimeString` TEXT NOT NULL, PRIMARY KEY(`timestamp`))");
                SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_LenomedHistory_code_timestamp` ON `LenomedHistory` (`code`, `timestamp`)");
                SQLite.execSQL(connection, RoomMasterTable.CREATE_QUERY);
                SQLite.execSQL(connection, "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '58ceb041455b2ad68db6e8d18809d0da')");
            }

            @Override // androidx.room.RoomOpenDelegate
            public void dropAllTables(SQLiteConnection connection) {
                Intrinsics.checkNotNullParameter(connection, "connection");
                SQLite.execSQL(connection, "DROP TABLE IF EXISTS `LenomedHistory`");
            }

            @Override // androidx.room.RoomOpenDelegate
            public void onOpen(SQLiteConnection connection) {
                Intrinsics.checkNotNullParameter(connection, "connection");
                this.this$0.internalInitInvalidationTracker(connection);
            }

            @Override // androidx.room.RoomOpenDelegate
            public void onPreMigrate(SQLiteConnection connection) {
                Intrinsics.checkNotNullParameter(connection, "connection");
                DBUtil.dropFtsSyncTriggers(connection);
            }

            @Override // androidx.room.RoomOpenDelegate
            public RoomOpenDelegate.ValidationResult onValidateSchema(SQLiteConnection connection) {
                Intrinsics.checkNotNullParameter(connection, "connection");
                LinkedHashMap linkedHashMap = new LinkedHashMap();
                linkedHashMap.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 1, null, 1));
                linkedHashMap.put(ResponseTypeValues.CODE, new TableInfo.Column(ResponseTypeValues.CODE, "INTEGER", true, 0, null, 1));
                linkedHashMap.put("value", new TableInfo.Column("value", "REAL", true, 0, null, 1));
                linkedHashMap.put("bolusType", new TableInfo.Column("bolusType", "TEXT", true, 0, null, 1));
                linkedHashMap.put("stringValue", new TableInfo.Column("stringValue", "TEXT", true, 0, null, 1));
                linkedHashMap.put(TypedValues.TransitionType.S_DURATION, new TableInfo.Column(TypedValues.TransitionType.S_DURATION, "INTEGER", true, 0, null, 1));
                linkedHashMap.put("dailyBasal", new TableInfo.Column("dailyBasal", "REAL", true, 0, null, 1));
                linkedHashMap.put("dailyBolus", new TableInfo.Column("dailyBolus", "REAL", true, 0, null, 1));
                linkedHashMap.put("alarm", new TableInfo.Column("alarm", "TEXT", true, 0, null, 1));
                linkedHashMap.put("lognum", new TableInfo.Column("lognum", "INTEGER", true, 0, null, 1));
                linkedHashMap.put("wrappingCount", new TableInfo.Column("wrappingCount", "INTEGER", true, 0, null, 1));
                linkedHashMap.put("pumpUid", new TableInfo.Column("pumpUid", "TEXT", true, 0, null, 1));
                linkedHashMap.put("refillType", new TableInfo.Column("refillType", "TEXT", true, 0, null, 1));
                linkedHashMap.put("basalType", new TableInfo.Column("basalType", "TEXT", true, 0, null, 1));
                linkedHashMap.put("dateTimeString", new TableInfo.Column("dateTimeString", "TEXT", true, 0, null, 1));
                LinkedHashSet linkedHashSet = new LinkedHashSet();
                LinkedHashSet linkedHashSet2 = new LinkedHashSet();
                linkedHashSet2.add(new TableInfo.Index("index_LenomedHistory_code_timestamp", false, CollectionsKt.listOf((Object[]) new String[]{ResponseTypeValues.CODE, "timestamp"}), CollectionsKt.listOf((Object[]) new String[]{"ASC", "ASC"})));
                TableInfo tableInfo = new TableInfo(LenomedHistoryDatabaseKt.TABLE_DIACONN_HISTORY, linkedHashMap, linkedHashSet, linkedHashSet2);
                TableInfo tableInfo2 = TableInfo.Companion.read(connection, LenomedHistoryDatabaseKt.TABLE_DIACONN_HISTORY);
                if (!tableInfo.equals(tableInfo2)) {
                    return new RoomOpenDelegate.ValidationResult(false, "LenomedHistory(app.aaps.pump.lenomed.database.LenomedHistoryRecord).\n Expected:\n" + tableInfo + "\n Found:\n" + tableInfo2);
                }
                return new RoomOpenDelegate.ValidationResult(true, null);
            }
        };
    }

    @Override // androidx.room.RoomDatabase
    protected InvalidationTracker createInvalidationTracker() {
        return new InvalidationTracker(this, new LinkedHashMap(), new LinkedHashMap(), LenomedHistoryDatabaseKt.TABLE_DIACONN_HISTORY);
    }

    @Override // androidx.room.RoomDatabase
    public void clearAllTables() {
        super.performClear(false, LenomedHistoryDatabaseKt.TABLE_DIACONN_HISTORY);
    }

    @Override // androidx.room.RoomDatabase
    protected Map<KClass<?>, List<KClass<?>>> getRequiredTypeConverterClasses() {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put(Reflection.getOrCreateKotlinClass(LenomedHistoryRecordDao.class), LenomedHistoryRecordDao_Impl.INSTANCE.getRequiredConverters());
        return linkedHashMap;
    }

    @Override // androidx.room.RoomDatabase
    public Set<KClass<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecClasses() {
        return new LinkedHashSet();
    }

    @Override // androidx.room.RoomDatabase
    public List<Migration> createAutoMigrations(Map<KClass<? extends AutoMigrationSpec>, ? extends AutoMigrationSpec> autoMigrationSpecs) {
        Intrinsics.checkNotNullParameter(autoMigrationSpecs, "autoMigrationSpecs");
        return new ArrayList();
    }

    @Override // app.aaps.pump.lenomed.database.LenomedHistoryDatabase
    public LenomedHistoryRecordDao historyRecordDao() {
        return this._lenomedHistoryRecordDao.getValue();
    }
}