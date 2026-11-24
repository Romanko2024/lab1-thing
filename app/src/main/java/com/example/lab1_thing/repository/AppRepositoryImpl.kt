package com.example.lab1_thing.repository

import com.example.lab1_thing.dao.AppDao
import com.example.lab1_thing.model.Artist
import com.example.lab1_thing.model.HorizontalList
import com.example.lab1_thing.model.MyListable
import com.example.lab1_thing.model.Track
import com.example.lab1_thing.util.toDomain
import com.example.lab1_thing.util.toEntity

class AppRepositoryImpl(
    private val appDao: AppDao
) : AppRepository {

    // ── Локальні тестові дані ──
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

    // ── DB ──
    override suspend fun addArtistsToDb(artists: List<Artist>): List<Long> {
        return appDao.insertAllArtists(artists.map { it.toEntity() })
    }

    override suspend fun addTracksToDb(tracks: List<Track>) {
        appDao.insertAllTracks(tracks.map { it.toEntity() })
    }

    override suspend fun getArtistsFromDb(): List<Artist> {
        return appDao.getArtists().map { it.toDomain() }
    }

    override suspend fun getTracksFromDb(): List<Track> {
        return appDao.getTracks().map { it.toDomain() }
    }

    override suspend fun getAllDataFromDb(): List<MyListable> {
        return getArtistsFromDb().plus(getTracksFromDb())
    }

    override suspend fun updateArtistInDb(artist: Artist) {
        appDao.updateArtist(artist.toEntity())
    }

    override suspend fun updateTrackInDb(track: Track) {
        appDao.updateTrack(track.toEntity())
    }

    override suspend fun deleteArtist(artist: Artist) {
        appDao.delArtist(artist.toEntity())
    }

    override suspend fun deleteTrack(track: Track) {
        appDao.delTrack(track.toEntity())
    }

    override suspend fun clearAllData() {
        appDao.delAllTracks()
        appDao.delAllArtists()
    }

    // ── Local ──
    override fun getLocalArtists(): List<Artist> = myArtistList
    override fun getLocalTracks(): List<Track> = myAppList.filterIsInstance<Track>()
    override fun getAllLocalData(): List<MyListable> = myAppList
}