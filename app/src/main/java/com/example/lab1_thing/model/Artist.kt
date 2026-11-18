package com.example.lab1_thing.model

data class Artist(
    val name: String,
    val country: String,
    override val id: Int
) : MyListable