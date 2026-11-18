package com.example.lab1_thing.repository

import com.example.lab1_thing.model.MyListable

interface AppRepository {
    fun getAllData(): List<MyListable>
}