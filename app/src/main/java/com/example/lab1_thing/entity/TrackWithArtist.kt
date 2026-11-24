package com.example.lab1_thing.entity

import androidx.room3.Embedded
import androidx.room3.Relation

data class TrackWithArtist(
    @Embedded val trackEntity: TrackEntity,
    @Relation(
        parentColumns = ["artist_id"],
        entityColumns = ["artist_id"]
    )
    val artistEntity: ArtistEntity?
)