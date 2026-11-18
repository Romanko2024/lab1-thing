package com.example.lab1_thing.repository

import com.example.lab1_thing.model.Artist
import com.example.lab1_thing.model.HorizontalList
import com.example.lab1_thing.model.MyListable
import com.example.lab1_thing.model.Track

class AppRepositoryImpl : AppRepository {

    private val myArtistList: List<Artist> = listOf(
        Artist("Lorem Ipsum", "Iceland", id = 1),
        Artist("Dolor sit amet", "Japan", id = 2),
        Artist("Consectetur", "UK", id = 3),
        Artist("Adipiscing", "Norway", id = 4),
        Artist("Elit-Sed-Do", "Canada", id = 5),
    )

    private val myAppList: List<MyListable> = listOf(
        Track("Tempor Incididunt", 240, myArtistList[0], id = 0),
        Track("ut Labore et Dolore", 195, myArtistList[1], id = 1),
        Track("Magna Aliqua", 210, myArtistList[2], id = 2),
        myArtistList[3],
        Track("Minim Veniam", 180, myArtistList[3], id = 3),
        HorizontalList(myArtistList, id = 100),
        Track("Consequat", 225, myArtistList[2], id = 4),
        Track("Excepteur", 168, myArtistList[1], id = 5),
        myArtistList[0],
        HorizontalList(myArtistList, id = 101),
        Track("Esse Cillum", 200, myArtistList[1], id = 6),
        myArtistList[4],
        Track("Deserunt", 255, myArtistList[3], id = 7),
    )

    override fun getAllData(): List<MyListable> = myAppList
}