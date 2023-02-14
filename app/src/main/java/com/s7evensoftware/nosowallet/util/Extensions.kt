package com.s7evensoftware.nosowallet.util

import kotlin.math.absoluteValue

fun Long.toNoso():String {
    var balance = absoluteValue.toString()
    while(balance.length < 9){
        balance = "0$balance"
    }
    val result = balance.substring(0, balance.length-8) + "." + balance.substring(balance.length-8)
    return if(balance.toDouble() >= 0) result else "-$result"
}

fun String.toNoso():Long {
    return replace(".","").replace(",","").toLong()
}