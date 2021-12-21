package com.s7evensoftware.nosowallet

class mpCoin {
    companion object {
        fun GetFee(monto:Int):Int{
            if(monto/ Comisiontrfr < MinimunFee){
                return MinimunFee
            }
            return monto/ Comisiontrfr
        }
    }
}