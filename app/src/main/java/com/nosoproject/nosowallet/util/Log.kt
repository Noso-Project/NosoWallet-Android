package com.nosoproject.nosowallet.util

import android.util.Log
import com.nosoproject.nosowallet.nosocore.mpDisk

class Log {
    companion object {
        fun e(origin:String, content:String){
            Log.e(origin, content)
            mpDisk.appendLog(origin, content)
        }
    }
}