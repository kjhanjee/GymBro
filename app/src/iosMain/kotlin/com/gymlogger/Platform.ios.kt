package com.gymlogger

import androidx.room.Room
import androidx.room.RoomDatabase
import com.gymlogger.data.AppDatabase
import platform.Foundation.NSHomeDirectory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = NSHomeDirectory() + "/gym_logger_db.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath
    )
}

private var dataStoreInstance: DataStore<Preferences>? = null

actual fun getDataStore(): DataStore<Preferences> {
    return dataStoreInstance ?: PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
            val path = requireNotNull(documentDirectory).path + "/settings.preferences_pb"
            path.toPath()
        }
    ).also { dataStoreInstance = it }
}
