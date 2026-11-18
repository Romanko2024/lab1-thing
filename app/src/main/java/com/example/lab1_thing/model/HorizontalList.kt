package com.example.lab1_thing.model

data class HorizontalList(
    val artistList: List<Artist>,
    override val id: Int
) : MyListable