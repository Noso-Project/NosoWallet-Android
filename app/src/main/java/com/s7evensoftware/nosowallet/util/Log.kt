package com.s7evensoftware.nosowallet.util

import android.util.Log
import com.s7evensoftware.nosowallet.nosocore.mpDisk

class Log {
    companion object {
        fun e(origin:String, content:String){
            Log.e(origin, content)
            mpDisk.appendLog(origin, content)
        }
    }
}