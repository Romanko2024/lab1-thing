package com.example.lab1_thing

import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import com.example.lab1_thing.model.MyListable
import com.example.lab1_thing.repository.AppRepository
import com.example.lab1_thing.repository.AppRepositoryImpl

@Serializable
sealed interface HomeScreen : NavKey {
    @Serializable
    data object Main : HomeScreen

    @Serializable
    data object Details : HomeScreen
}

class HomeViewModel : ViewModel() {

    private val appRepository: AppRepository = AppRepositoryImpl()
    val catalogItems: StateFlow<List<MyListable>>
        field = MutableStateFlow(appRepository.getAllData())
    val labelText: StateFlow<String>
        field = MutableStateFlow("Початковий текст на Головній")

    val homeBackStack: StateFlow<List<HomeScreen>>
        field = MutableStateFlow<List<HomeScreen>>(listOf(HomeScreen.Main))

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
}
