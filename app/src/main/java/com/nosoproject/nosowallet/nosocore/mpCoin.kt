package com.nosoproject.nosowallet.nosocore

import com.nosoproject.nosowallet.model.Comisiontrfr
import com.nosoproject.nosowallet.model.MinimunFee
import kotlin.math.absoluteValue

class mpCoin {
    companion object {
        fun GetFee(monto:Long):Long{
            if(monto/ Comisiontrfr < MinimunFee){
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
    }
}