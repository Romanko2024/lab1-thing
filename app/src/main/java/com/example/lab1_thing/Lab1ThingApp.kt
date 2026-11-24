package com.example.lab1_thing

import android.app.Application
import com.example.lab1_thing.db.AppDataBase
import com.example.lab1_thing.repository.AppRepositoryImpl

class Lab1ThingApp : Application() {
    private val appDataBase by lazy { AppDataBase.getDatabase(this) }

    val appRepository by lazy {
        AppRepositoryImpl(appDao = appDataBase.appDao())
    }
}