package com.example.lab1_thing

import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface HomeScreen : NavKey {
    data object Main : HomeScreen
    data object Details : HomeScreen
}

class HomeViewModel : ViewModel() {

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
