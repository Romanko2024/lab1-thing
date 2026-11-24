package com.example.lab1_thing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.example.lab1_thing.model.Artist
import com.example.lab1_thing.model.MyListable
import com.example.lab1_thing.model.Track
import com.example.lab1_thing.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
sealed interface HomeScreen : NavKey {
    @Serializable data object Main : HomeScreen
    @Serializable data object Details : HomeScreen
}

class HomeViewModel(
    private val appRepository: AppRepository
) : ViewModel() {

    val labelText: StateFlow<String>
        field = MutableStateFlow("Початковий текст на Головній")

    val homeBackStack: StateFlow<List<HomeScreen>>
        field = MutableStateFlow<List<HomeScreen>>(listOf(HomeScreen.Main))

    val catalogItems: StateFlow<List<MyListable>>
        field = MutableStateFlow<List<MyListable>>(emptyList())

    fun updateText(newText: String) {
        labelText.value = newText
    }

    fun navigateTo(screen: HomeScreen) {
        homeBackStack.value = homeBackStack.value + screen
    }

    fun navigateBack() {
        if (homeBackStack.value.size > 1) {
            homeBackStack.value = homeBackStack.value.dropLast(1)
        }
    }

    // ── DB CRUD ──
    fun loadAllDataFromDb() {
        viewModelScope.launch {
            catalogItems.value = appRepository.getAllDataFromDb()
        }
    }

    fun loadAllLocalDataToDb() {
        viewModelScope.launch {
            appRepository.addArtistsToDb(appRepository.getLocalArtists())
            appRepository.addTracksToDb(appRepository.getLocalTracks())
            catalogItems.value = appRepository.getAllDataFromDb()
        }
    }

    fun clearDb() {
        viewModelScope.launch {
            appRepository.clearAllData()
            catalogItems.value = appRepository.getAllDataFromDb()
        }
    }

    fun deleteArtist(artist: Artist) {
        viewModelScope.launch {
            appRepository.deleteArtist(artist)
            catalogItems.value = appRepository.getAllDataFromDb()
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            appRepository.deleteTrack(track)
            catalogItems.value = appRepository.getAllDataFromDb()
        }
    }

    fun addArtist(name: String, country: String) {
        viewModelScope.launch {
            appRepository.addArtistsToDb(listOf(Artist(name, country, 0)))
            catalogItems.value = appRepository.getAllDataFromDb()
        }
    }

    fun addTrack(title: String, durationSec: Int, artist: Artist?) {
        viewModelScope.launch {
            appRepository.addTracksToDb(listOf(Track(title, durationSec, artist, 0)))
            catalogItems.value = appRepository.getAllDataFromDb()
        }
    }

    fun updateArtist(artist: Artist) {
        viewModelScope.launch {
            appRepository.updateArtistInDb(artist)
            catalogItems.value = appRepository.getAllDataFromDb()
        }
    }

    fun updateTrack(track: Track) {
        viewModelScope.launch {
            appRepository.updateTrackInDb(track)
            catalogItems.value = appRepository.getAllDataFromDb()
        }
    }
}
