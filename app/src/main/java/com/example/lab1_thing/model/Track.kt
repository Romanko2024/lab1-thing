package com.example.lab1_thing.model

data class Track(
    val title: String,
    val durationSec: Int,
    val artist: Artist?,
    override val id: Int
) : MyListable