package com.example.lab1_thing.util

import com.example.lab1_thing.entity.ArtistEntity
import com.example.lab1_thing.entity.TrackEntity
import com.example.lab1_thing.entity.TrackWithArtist
import com.example.lab1_thing.model.Artist
import com.example.lab1_thing.model.Track

// domain → entity
fun Artist.toEntity() = ArtistEntity(
    name = this.name,
    country = this.country,
    id = this.id
)

fun Track.toEntity() = TrackEntity(
    title = this.title,
    durationSec = this.durationSec,
    artistId = this.artist?.id,
    id = this.id
)

// entity → domain
fun ArtistEntity.toDomain() = Artist(
    name = this.name,
    country = this.country,
    id = this.id
)

fun TrackWithArtist.toDomain() = Track(
    title = this.trackEntity.title,
    durationSec = this.trackEntity.durationSec,
    artist = this.artistEntity?.toDomain(),
    id = this.trackEntity.id
)