package com.example.lab1_thing.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = arrayOf("artist_id"),
            childColumns = arrayOf("artist_id"),
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class TrackEntity(
    @ColumnInfo(name = "track_title")
    val title: String,
    val durationSec: Int,
    @ColumnInfo(name = "artist_id", index = true)
    val artistId: Int?,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "track_id")
    val id: Int = 0
)