package com.s7evensoftware.nosowallet

import kotlin.math.absoluteValue

class mpCoin {
    companion object {
        fun GetFee(monto:Long):Long{
            if(monto/Comisiontrfr < MinimunFee){
                return MinimunFee
            }
            return monto / Comisiontrfr
        }

        fun Long2Currency(balance:Long):String{
            var balancStr = (balance.absoluteValue).toString()
            while(balancStr.length < 9){
                balancStr = "0"+balancStr
            }
            val result = balancStr.substring(0, balancStr.length-8) + "." + balancStr.substring(balancStr.length-8)
            return if(balance >= 0) result else "-"+result
        }

        fun isCoinFormat(amount:String):Boolean{
            var dotpoint = 0
            var chars_after_point = 0
            for((index, char) in amount.withIndex()){
                if(dotpoint > 0){
                    chars_after_point++
                }
                if(char == '.'){
                    dotpoint = index
                }
            }

            if(chars_after_point == 8){
                return true
            }
            return false
        }
    }
}