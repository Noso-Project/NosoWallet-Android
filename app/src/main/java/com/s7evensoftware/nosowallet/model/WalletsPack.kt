package com.s7evensoftware.nosowallet.model

import com.s7evensoftware.nosowallet.WalletObject

data class WalletsPack (
    var normalList:List<WalletObject> = listOf(),
    var ghostList:List<WalletObject> = listOf(),
    var summaryList: List<WalletObject> = listOf()
)