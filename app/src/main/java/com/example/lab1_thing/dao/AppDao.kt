package com.example.lab1_thing.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.example.lab1_thing.entity.ArtistEntity
import com.example.lab1_thing.entity.TrackEntity
import com.example.lab1_thing.entity.TrackWithArtist

@Dao
interface AppDao {

    // ── Artists ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artistEntity: ArtistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllArtists(artistsEntity: List<ArtistEntity>): List<Long>

    @Update
    suspend fun updateArtist(artistEntity: ArtistEntity)

    @Delete
    suspend fun delArtist(artistEntity: ArtistEntity)

    @Query("DELETE FROM artists")
    suspend fun delAllArtists()

    @Query("SELECT * FROM artists")
    suspend fun getArtists(): List<ArtistEntity>

    // ── Tracks ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(trackEntity: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTracks(tracksEntity: List<TrackEntity>)

    @Update
    suspend fun updateTrack(trackEntity: TrackEntity)

    @Delete
    suspend fun delTrack(trackEntity: TrackEntity)

    @Query("DELETE FROM tracks")
    suspend fun delAllTracks()

    @Transaction
    @Query("SELECT * FROM tracks ORDER BY track_title")
    suspend fun getTracks(): List<TrackWithArtist>
}