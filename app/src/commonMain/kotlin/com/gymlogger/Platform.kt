package com.gymlogger

import androidx.room.RoomDatabase
import com.gymlogger.data.AppDatabase
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

expect fun getDataStore(): DataStore<Preferences>
