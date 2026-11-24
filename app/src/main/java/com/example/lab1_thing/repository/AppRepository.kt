package com.example.lab1_thing.repository

import com.example.lab1_thing.model.Artist
import com.example.lab1_thing.model.MyListable
import com.example.lab1_thing.model.Track

interface AppRepository {

    // ── DB ──
    suspend fun addArtistsToDb(artists: List<Artist>): List<Long>
    suspend fun addTracksToDb(tracks: List<Track>)
    suspend fun getArtistsFromDb(): List<Artist>
    suspend fun getTracksFromDb(): List<Track>
    suspend fun getAllDataFromDb(): List<MyListable>

    suspend fun updateArtistInDb(artist: Artist)
    suspend fun updateTrackInDb(track: Track)

    suspend fun deleteArtist(artist: Artist)
    suspend fun deleteTrack(track: Track)

    suspend fun clearAllData()

    // ── Local test data ──
    fun getLocalArtists(): List<Artist>
    fun getLocalTracks(): List<Track>
    fun getAllLocalData(): List<MyListable>
}