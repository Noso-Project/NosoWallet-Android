package com.s7evensoftware.nosowallet

import android.util.Log
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.MessageDigest

class mpCripto {
    companion object {
        fun CreateNewAddress():WalletObject {
            var MyData = WalletObject()
            var Address:String
            var KeysPair:KeyPair

            KeysPair = SignerUtils.GenerateECKeyPair(KeyType.SECP256K1)
            Address = GetAddressFromPublicKey(KeysPair.PublicKey!!)

            MyData.Hash = Address
            MyData.Custom = ""
            MyData.PublicKey = KeysPair.PublicKey
            MyData.PrivateKey = KeysPair.PrivateKey
            MyData.Balance = 0
            MyData.Pending = 0
            MyData.Score = 0
            MyData.LasOP = 0
            return MyData
        }

        fun GetAddressFromPublicKey(PubKey:String):String {
            var PubSHAHashed:String
            var Hash1:String
            var Hash2:String
            var clave:String
            var sumatoria:Int

            PubSHAHashed = HashSha256String(PubKey)
            //Log.e("mCripto","PubHashHashed: "+PubSHAHashed)
            MainActivity.output += "\nPubHashHashed: $PubSHAHashed"
            Hash1 = HashMD160String(PubSHAHashed)
            //Log.e("mpCripto","Hash1: "+Hash1)
            MainActivity.output += "\nHash1: $Hash1"

            Hash1 = BMHexto58(Hash1, BigInteger("58"))
            //Log.e("mpCripto","Hash1: "+Hash1)
            MainActivity.output += "\nHash1: $Hash1"

            sumatoria = BMB58resumen(Hash1)
            //Log.e("mpCripto","Sumatoria: "+sumatoria)
            MainActivity.output += "\nSumatoria: $sumatoria"
            clave = BMDecto58(sumatoria.toString())
            //Log.e("mpCripto","Clave: "+clave)
            MainActivity.output += "\nClave: $clave"
            Hash2 = Hash1+clave
            //Log.e("mpCripto","Hash2: "+Hash2)
            MainActivity.output += "\nHash2: $Hash2"
            //Log.e("mpCripto","New Address: "+CoinChar+Hash2)
            MainActivity.output += "\nNew Address: $CoinChar$Hash2"
            return CoinChar+Hash2
        }

        //Returns the SHA256 of a String in CAPITAL
        fun HashSha256String(StringToHash:String):String {
            var Source = StringToHash.toByteArray()
            val MessageDigestInstance = MessageDigest.getInstance("SHA-256")
            val Digest = MessageDigestInstance.digest(Source)
            val result = Digest.fold("", { str, it -> str + "%02x".format(it) })
            return result.uppercase() //Display the digest in capital letter ?? ECO: not sure why
        }

        fun HashMD160String(StringToHash:String):String {
            val bytesFromString = StringToHash.toByteArray()
            val mdInstance = RIPEMD160Digest()
            mdInstance.update(bytesFromString,0,bytesFromString.size)

            var result = ByteArray(mdInstance.digestSize)
            mdInstance.doFinal(result, 0)
            //Log.e("mpCripto","MD160 result raw: "+String(result))
            //Log.e("mpCripto","MD160 result hex: "+String(Hex.encode(result)))
            return String(Hex.encode(result))
        }

        /* Legacy Kotlin based
        fun BMHexto58(numerohex:String, alphabetnumber:BigInteger):String {
            var decimalValue:BigInteger
            var RDiv_Residuo:BigInteger
            var Resultado = ""
            var AlphabetUsed:String

            if(alphabetnumber == BigInteger("36")){
                AlphabetUsed = B36Alphabet
            }else{
                AlphabetUsed = B58Alphabet
            }

            decimalValue = BMHexToDec(numerohex)
            Log.e("mcripto","Ht58 - decimal: $decimalValue")
            while (decimalValue.toString().length >= 2){
                decimalValue = decimalValue/alphabetnumber
                RDiv_Residuo = decimalValue % alphabetnumber
                Log.e("mcripto","Ht58 - decimal: $decimalValue residuo: $RDiv_Residuo simbolo: "+AlphabetUsed[RDiv_Residuo.toInt()])
                Resultado = AlphabetUsed[RDiv_Residuo.toInt()]+Resultado
            }

            if(decimalValue > alphabetnumber){
                decimalValue = decimalValue/alphabetnumber
                RDiv_Residuo = decimalValue % alphabetnumber
                Log.e("mcripto","Ht58 - decimal: $decimalValue residuo: $RDiv_Residuo simbolo: "+AlphabetUsed[RDiv_Residuo.toInt()])
                Resultado = AlphabetUsed[RDiv_Residuo.toInt()] + Resultado
            }

            if(decimalValue > BigInteger("0")){
                Log.e("mcripto","Ht58 - decimal: $decimalValue  simbolo: "+AlphabetUsed[decimalValue.toInt()])
                Resultado = AlphabetUsed[decimalValue.toInt()]+Resultado
            }
            return Resultado
        }
        */

        fun ClearLeadingCeros(numero:String):String{
            // Using BigInteger parser to remove leading zeroes
            return BigInteger(numero).toString()
        }

        fun BMDividir(numeroA:String, Divisor:Int):DivResult{
            var cociente = ""
            var ThisStep = ""

            for(i in 0..(numeroA.length-1)){
                ThisStep = ThisStep + numeroA[i]
                if(Integer.parseInt(ThisStep) >= Divisor){
                    cociente = cociente+(Integer.parseInt(ThisStep)/Divisor).toString()
                    ThisStep = (Integer.parseInt(ThisStep) % Divisor).toString()
                }else{
                    cociente = cociente +"0"
                }
            }
            val r = DivResult()
            r.Cociente = ClearLeadingCeros(cociente)
            r.Residuo = ClearLeadingCeros(ThisStep)
            return r
        }

        //Pascal Like
        fun BMHexto58(numerohex:String, alphabetnumber:BigInteger):String {
            var decimalValue:String
            var ResultadoDiv:DivResult
            var restante:String
            var Resultado = ""
            var AlphabetUsed:String

            if(alphabetnumber == BigInteger("36")){
                AlphabetUsed = B36Alphabet
            }else{
                AlphabetUsed = B58Alphabet
            }

            decimalValue = BMHexToDec(numerohex).toString()
            while (decimalValue.length >= 2){
                val save = decimalValue
                ResultadoDiv = BMDividir(decimalValue, alphabetnumber.toInt())
                decimalValue = ResultadoDiv.Cociente!!
                restante = ResultadoDiv.Residuo!!
                Resultado = AlphabetUsed[Integer.parseInt(restante)] + Resultado
            }

            if(Integer.parseInt(decimalValue) >= alphabetnumber.toInt()){
                ResultadoDiv = BMDividir(decimalValue, alphabetnumber.toInt())
                decimalValue = ResultadoDiv.Cociente!!
                restante = ResultadoDiv.Residuo!!
                Resultado = AlphabetUsed[Integer.parseInt(restante)] + Resultado
            }

            if(Integer.parseInt(decimalValue) > 0){
                Resultado = AlphabetUsed[Integer.parseInt(decimalValue)]+Resultado
            }
            return Resultado
        }

        /*  Legacy - Kotlin Base
        fun BMDecto58(numero:String):String {
            var decimalValue:BigInteger
            var RDiv_Residuo:BigInteger
            var Resultado = ""

            decimalValue = BigInteger(numero)
            while (decimalValue.toString().length >= 2){
                decimalValue = decimalValue/ BigInteger("58")
                RDiv_Residuo = decimalValue % BigInteger("58")
                Resultado = B58Alphabet[RDiv_Residuo.toInt()]+Resultado
            }

            if(decimalValue > BigInteger("58")){
                decimalValue = decimalValue/BigInteger("58")
                RDiv_Residuo = decimalValue % BigInteger("58")
                Resultado = B58Alphabet[RDiv_Residuo.toInt()] + Resultado
            }

            if(decimalValue > BigInteger("0")){
                Resultado = B58Alphabet[decimalValue.toInt()]+Resultado
            }
            return Resultado
        }*/


        // Pascal based - Not matching for some reason with Kotlin based
        fun BMDecto58(numero:String):String {
            var decimalValue:String
            var ResultadoDiv:DivResult
            var restante:String
            var Resultado = ""

            decimalValue = BigInteger(numero).toString()
            while (decimalValue.length >= 2){
                ResultadoDiv = BMDividir(decimalValue, 58)
                decimalValue = ResultadoDiv.Cociente!!
                restante = ResultadoDiv.Residuo!!
                Resultado = B58Alphabet[Integer.parseInt(restante)] + Resultado
            }

            if(Integer.parseInt(decimalValue) >= 58){
                ResultadoDiv = BMDividir(decimalValue, 58)
                decimalValue = ResultadoDiv.Cociente!!
                restante = ResultadoDiv.Residuo!!
                Resultado = B58Alphabet[Integer.parseInt(restante)] + Resultado
            }

            if(Integer.parseInt(decimalValue) > 0){
                Resultado = B58Alphabet[Integer.parseInt(decimalValue)]+Resultado
            }
            return Resultado
        }

        fun BMB58resumen(numero58:String):Int {
            var total = 0
            for(i in 0..(numero58.length-1)){
                total = total + B58Alphabet.indexOf(numero58[i])
            }
            return total
        }

        fun BMHexToDec(numerohex:String):BigInteger {
            return BigInteger(numerohex, 16)
        }
    }
}