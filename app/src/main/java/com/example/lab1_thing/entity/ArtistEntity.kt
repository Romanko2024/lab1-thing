package com.example.lab1_thing.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "artists")
data class ArtistEntity(
    @ColumnInfo(name = "artist_name")
    val name: String,
    val country: String,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "artist_id")
    val id: Int = 0
)