package app.aaps.pump.lenomed.database;

import android.content.Context;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import ch.qos.logback.core.CoreConstants;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LenomedHistoryDatabase.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\b'\u0018\u0000 \u00062\u00020\u0001:\u0001\u0006B\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\b\u0010\u0004\u001a\u00020\u0005H&¨\u0006\u0007"}, d2 = {"Lapp/aaps/pump/lenomed/database/LenomedHistoryDatabase;", "Landroidx/room/RoomDatabase;", "<init>", "()V", "historyRecordDao", "Lapp/aaps/pump/lenomed/database/LenomedHistoryRecordDao;", "Companion", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public abstract class LenomedHistoryDatabase extends RoomDatabase {

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    public static final int VERSION = 3;

    public abstract LenomedHistoryRecordDao historyRecordDao();

    /* JADX INFO: compiled from: LenomedHistoryDatabase.kt */
    @Metadata(d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u000e\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tR\u000e\u0010\u0004\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000¨\u0006\n"}, d2 = {"Lapp/aaps/pump/lenomed/database/LenomedHistoryDatabase$Companion;", "", "<init>", "()V", "VERSION", "", "build", "Lapp/aaps/pump/lenomed/database/LenomedHistoryDatabase;", CoreConstants.CONTEXT_SCOPE_VALUE, "Landroid/content/Context;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final LenomedHistoryDatabase build(Context context) {
            Intrinsics.checkNotNullParameter(context, "context");
            Context applicationContext = context.getApplicationContext();
            Intrinsics.checkNotNullExpressionValue(applicationContext, "getApplicationContext(...)");
            return (LenomedHistoryDatabase) Room.databaseBuilder(applicationContext, LenomedHistoryDatabase.class, "lenomed_database.db").fallbackToDestructiveMigration().build();
        }
    }
}