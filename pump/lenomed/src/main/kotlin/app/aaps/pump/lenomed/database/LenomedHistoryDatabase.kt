package app.aaps.pump.lenomed.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local cache of pump history, kept outside AAPS's own database so a driver sync cannot disturb it.
 *
 * Rebuilt as Kotlin from the decompiled Java so Room can see the annotations it needs to generate
 * `LenomedHistoryDatabase_Impl`. The driver used to obtain this through Dagger field injection; under
 * Metro it comes from [app.aaps.pump.lenomed.di.LenomedPluginBindings.provideLenomedHistoryDatabase].
 */
@Database(
    entities = [LenomedHistoryRecord::class],
    version = 3,
    exportSchema = false
)
abstract class LenomedHistoryDatabase : RoomDatabase() {

    abstract fun historyRecordDao(): LenomedHistoryRecordDao

    companion object {

        /** Name the driver stores its rows under. */
        const val TABLE_DIACONN_HISTORY = "LenomedHistory"

        private const val DATABASE_NAME = "lenomed_database.db"

        fun build(context: Context): LenomedHistoryDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LenomedHistoryDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}
