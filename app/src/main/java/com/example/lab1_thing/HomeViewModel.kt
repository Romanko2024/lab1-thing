package com.example.lab1_thing

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {
    private val _labelText = MutableStateFlow("Початковий текст на Головній")
    val labelText: StateFlow<String> = _labelText.asStateFlow()

    fun updateText(newText: String) {
        _labelText.value = newText
    }
}