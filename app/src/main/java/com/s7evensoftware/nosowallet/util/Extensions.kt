package com.s7evensoftware.nosowallet.util

import java.text.SimpleDateFormat
import java.util.*
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

fun Long.toDateTime():String {
    if(this == 0L) return "-/-/- -:-"
    val cal = Calendar.getInstance()
    val dF = SimpleDateFormat("dd/M/yyyy H:mm", Locale.getDefault())
    cal.timeInMillis = this

    return dF.format(cal.time)
}