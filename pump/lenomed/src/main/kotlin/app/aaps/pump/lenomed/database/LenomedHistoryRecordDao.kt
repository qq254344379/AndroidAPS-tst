package app.aaps.pump.lenomed.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Single

/**
 * Access to the locally cached pump history.
 *
 * The method signatures come straight from the decompiled driver; the SQL behind them does not, since
 * Room annotations are CLASS retention and JADX cannot emit them. Each query below is the reading the
 * method name and its call sites support:
 *
 *  - [allFromByType] backs the history screen: everything of one log type newer than a cutoff.
 *  - [countByTimestamp] / [countByDateTimeString] are duplicate guards the caller makes before writing,
 *    which is what pinned down the entity's composite primary key.
 *  - [getLastRecord] backs resuming an interrupted sync; [getMaxLogNumForType] continues the pump's
 *    own per-type log numbering.
 */
@Dao
interface LenomedHistoryRecordDao {

    @Query(
        "SELECT * FROM ${LenomedHistoryDatabase.TABLE_DIACONN_HISTORY} " +
            "WHERE timestamp >= :timestamp AND code = :type"
    )
    fun allFromByType(timestamp: Long, type: Byte): Single<List<LenomedHistoryRecord>>

    @Query(
        "SELECT COUNT(*) FROM ${LenomedHistoryDatabase.TABLE_DIACONN_HISTORY} " +
            "WHERE code = :logType AND pumpUid = :pumpUid AND dateTimeString = :dateTimeString"
    )
    fun countByDateTimeString(logType: Byte, pumpUid: String, dateTimeString: String): Int

    @Query(
        "SELECT COUNT(*) FROM ${LenomedHistoryDatabase.TABLE_DIACONN_HISTORY} " +
            "WHERE code = :logType AND pumpUid = :pumpUid AND timestamp = :timestamp"
    )
    fun countByTimestamp(logType: Byte, pumpUid: String, timestamp: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun createOrUpdate(lenomedHistoryRecord: LenomedHistoryRecord)

    @Query(
        "SELECT * FROM ${LenomedHistoryDatabase.TABLE_DIACONN_HISTORY} " +
            "WHERE pumpUid = :pumpUid ORDER BY timestamp DESC LIMIT 1"
    )
    fun getLastRecord(pumpUid: String): LenomedHistoryRecord?

    @Query(
        "SELECT MAX(lognum) FROM ${LenomedHistoryDatabase.TABLE_DIACONN_HISTORY} " +
            "WHERE code = :logType AND pumpUid = :pumpUid"
    )
    fun getMaxLogNumForType(logType: Byte, pumpUid: String): Int?
}
