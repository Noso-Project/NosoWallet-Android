package com.s7evensoftware.nosowallet


import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    var SettingsDialog:AlertDialog? = null
    var SettingsAddress = MutableLiveData("")
    var SettingsPort = MutableLiveData("")
    var SettingsServerSelected:ServerObject? = null
    var isSettingsOpen = false

    var AdddressList = MutableLiveData(ArrayList<WalletObject>())
    var PendingList = MutableLiveData(ArrayList<PendingData>())
    var UpdateBalanceTrigger = MutableLiveData(0)
    var AvailableBalance = MutableLiveData(0L)
    var SYNC_ENABLED = true
    var SYNC_FAILURES = 0
    var SYNC_DELAY:Long = DEFAULT_SYNC_DELAY

    var LastBlock = MutableLiveData(0L)
    var LastSummary = MutableLiveData("")
    var LastPendingCount = MutableLiveData(0L)
    var WalletSynced = MutableLiveData(false)
    var ConnectionError = MutableLiveData(false)

    var RealTimeValue = MutableLiveData(System.currentTimeMillis())

    var isSendFundsOpen = MutableLiveData(false)
    var SendFunds_TO = ""
    var SendFunds_FROM = ""
    var SendFunds_Amount = 0L
    var SendFubds_Ref= ""
    var LastNodeSelected:NodeInfo? = null
    var TriggerSuccessError = MutableLiveData(0)

}