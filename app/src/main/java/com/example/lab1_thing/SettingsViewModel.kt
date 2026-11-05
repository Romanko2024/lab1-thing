package com.example.lab1_thing

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel : ViewModel() {

    val labelText: StateFlow<String>
        field = MutableStateFlow("Початковий текст у Налаштуваннях")

    fun updateText(newText: String) {
        labelText.value = newText
    }
}
