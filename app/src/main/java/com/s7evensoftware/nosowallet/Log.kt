package com.s7evensoftware.nosowallet

import android.util.Log

class Log {
    companion object {
        fun e(origin:String, content:String){
            Log.e(origin, content)
            mpDisk.appendLog(origin, content)
        }
    }
}