package app.aaps.pump.lenomed.database

import android.content.Context

/**
 * Process-wide access to [LenomedHistoryDatabase].
 *
 * The driver obtained this database by Dagger field injection into its service and activities. The
 * dev branch replaced Dagger with Metro, which has no field injection, and both those classes are
 * still Java - Metro cannot generate a `MembersInjector` for a class it has no Kotlin IR for. So
 * rather than leave the field unset (a guaranteed `UninitializedPropertyAccessException` the first
 * time the history screen loads), the one instance Metro builds is published here and read back.
 *
 * Two `Room.databaseBuilder` instances on the same file would be two connections to one SQLite file,
 * hence the cache: [get] builds at most one, whoever asks first.
 */
object LenomedHistoryDatabases {

    @Volatile
    private var instance: LenomedHistoryDatabase? = null

    /** Called by the Metro provider so the database DI built is the one everyone else gets. */
    fun install(database: LenomedHistoryDatabase) {
        instance = database
    }

    @JvmStatic
    fun get(context: Context): LenomedHistoryDatabase =
        instance ?: synchronized(this) {
            instance ?: LenomedHistoryDatabase.build(context.applicationContext).also { instance = it }
        }
}
