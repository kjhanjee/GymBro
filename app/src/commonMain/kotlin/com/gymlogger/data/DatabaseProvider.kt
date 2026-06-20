package com.gymlogger.data

import com.gymlogger.getDatabaseBuilder

object DatabaseProvider {
    private val INSTANCE by lazy { getDatabaseBuilder().build() }

    fun getDatabase(): AppDatabase {
        return INSTANCE
    }
}
