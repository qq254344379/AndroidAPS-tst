package app.aaps.pump.lenomed.database;

import io.reactivex.rxjava3.core.Single;
import java.util.List;
import kotlin.Metadata;

/* JADX INFO: compiled from: LenomedHistoryRecordDao.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0005\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0006\b'\u0018\u00002\u00020\u0001B\u0007¢\u0006\u0004\b\u0002\u0010\u0003J$\u0010\u0004\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u00060\u00052\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bH'J\u0010\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u0007H'J\u0012\u0010\u000f\u001a\u0004\u0018\u00010\u00072\u0006\u0010\u0010\u001a\u00020\u0011H'J\u001f\u0010\u0012\u001a\u0004\u0018\u00010\u00132\u0006\u0010\u0014\u001a\u00020\u000b2\u0006\u0010\u0010\u001a\u00020\u0011H'¢\u0006\u0002\u0010\u0015J \u0010\u0016\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u000b2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\b\u001a\u00020\tH'J \u0010\u0017\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u000b2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0018\u001a\u00020\u0011H'¨\u0006\u0019"}, d2 = {"Lapp/aaps/pump/lenomed/database/LenomedHistoryRecordDao;", "", "<init>", "()V", "allFromByType", "Lio/reactivex/rxjava3/core/Single;", "", "Lapp/aaps/pump/lenomed/database/LenomedHistoryRecord;", "timestamp", "", "type", "", "createOrUpdate", "", "lenomedHistoryRecord", "getLastRecord", "pumpUid", "", "getMaxLogNumForType", "", "logType", "(BLjava/lang/String;)Ljava/lang/Integer;", "countByTimestamp", "countByDateTimeString", "dateTimeString", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public abstract class LenomedHistoryRecordDao {
    public abstract Single<List<LenomedHistoryRecord>> allFromByType(long timestamp, byte type);

    public abstract int countByDateTimeString(byte logType, String pumpUid, String dateTimeString);

    public abstract int countByTimestamp(byte logType, String pumpUid, long timestamp);

    public abstract void createOrUpdate(LenomedHistoryRecord lenomedHistoryRecord);

    public abstract LenomedHistoryRecord getLastRecord(String pumpUid);

    public abstract Integer getMaxLogNumForType(byte logType, String pumpUid);
}