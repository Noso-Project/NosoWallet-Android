package com.s7evensoftware.nosowallet


import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.realm.RealmList

class MainViewModel: ViewModel() {
    var SettingsDialog:AlertDialog? = null
    var SettingsAddress = MutableLiveData("")
    var SettingsPort = MutableLiveData("")
    var isSettingsOpen = false

    var AdddressList = MutableLiveData(ArrayList<WalletObject>())
    var PendingList = MutableLiveData(ArrayList<PendingData>())
    var AddressSummary = MutableLiveData(ArrayList<SumaryData>())
    var SYNC_ENABLED = true
    var SYNC_DELAY:Long = 10000

    var LastBlock = MutableLiveData(0L)
    var LastSummary = MutableLiveData("")
    var LastPendingCount = MutableLiveData(0)
    var WalletSynced = false

    var REF_Addresses = false
    var REF_Status = false
    var REF_Nodes = false
    var LastSyncExecution = MutableLiveData(0L)
}