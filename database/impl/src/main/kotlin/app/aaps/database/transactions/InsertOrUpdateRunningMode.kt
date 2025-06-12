package app.aaps.database.transactions

import app.aaps.database.entities.RunningMode

class InsertOrUpdateRunningMode(val runningMode: RunningMode) : Transaction<InsertOrUpdateRunningMode.TransactionResult>() {

    override fun run(): TransactionResult {
        val result = TransactionResult()

        val current = database.runningModeDao.findById(runningMode.id)
        if (current == null) {
            database.runningModeDao.insertNewEntry(runningMode)
            result.inserted.add(runningMode)
        } else {
            database.runningModeDao.updateExistingEntry(runningMode)
            result.updated.add(runningMode)
        }
        return result
    }

    class TransactionResult {

        val inserted = mutableListOf<RunningMode>()
        val updated = mutableListOf<RunningMode>()
    }
}