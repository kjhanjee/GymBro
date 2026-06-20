package com.gymlogger

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gymlogger.data.AppDatabase
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File

object AndroidPlatform {
    lateinit var context: Context
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val appContext = AndroidPlatform.context
    val dbFile = appContext.getDatabasePath("gym_logger_db.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

private var dataStoreInstance: DataStore<Preferences>? = null

actual fun getDataStore(): DataStore<Preferences> {
    return dataStoreInstance ?: PreferenceDataStoreFactory.create(
        produceFile = { File(AndroidPlatform.context.filesDir, "settings.preferences_pb") }
    ).also { dataStoreInstance = it }
}
