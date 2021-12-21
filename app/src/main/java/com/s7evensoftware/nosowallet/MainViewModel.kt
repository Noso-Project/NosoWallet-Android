package com.s7evensoftware.nosowallet


import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    var SettingsDialog:AlertDialog? = null
    var SettingsAddress = MutableLiveData("")
    var SettingsPort = MutableLiveData("")
    var isSettingsOpen = false
}