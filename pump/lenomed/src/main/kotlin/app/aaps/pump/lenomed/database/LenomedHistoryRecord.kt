package app.aaps.pump.lenomed.database

import androidx.room.Entity

/**
 * One decoded history/log line from the pump.
 *
 * Rewritten as Kotlin from the decompiled Java. The decompilation lost two things that cannot be
 * recovered from bytecode and had to be reconstructed:
 *
 *  - the Room annotations (CLASS retention, so JADX never emits them) - without `@Entity` no
 *    `_Impl` is generated and Room throws on first use;
 *  - the primary key. The original was composite, inferred from [LenomedHistoryRecordDao.countByTimestamp]:
 *    callers ask "do I already hold (logType, pumpUid, timestamp)?" before inserting, which is only
 *    meaningful if that exact tuple is what identifies a row. Keeping the same tuple as the key is
 *    what makes `createOrUpdate` idempotent per log line.
 *
 * Property order and defaults are taken from the synthetic constructor the decompiler recovered.
 */
@Entity(
    tableName = LenomedHistoryDatabase.TABLE_DIACONN_HISTORY,
    primaryKeys = ["timestamp", "code", "pumpUid"]
)
data class LenomedHistoryRecord(
    val timestamp: Long,
    val code: Byte,
    var pumpUid: String = "",
    var value: Double = 0.0,
    var bolusType: String = "None",
    var stringValue: String = "",
    var duration: Int = 0,
    var dailyBasal: Double = 0.0,
    var dailyBolus: Double = 0.0,
    var alarm: String = "",
    var lognum: Int = 0,
    var wrappingCount: Int = 0,
    var refillType: String = "",
    var basalType: String = "",
    var dateTimeString: String = ""
)
