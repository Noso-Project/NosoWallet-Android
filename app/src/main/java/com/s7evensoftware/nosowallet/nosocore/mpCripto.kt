package com.s7evensoftware.nosowallet.nosocore

import androidx.compose.runtime.MutableState
import com.s7evensoftware.nosowallet.*
import com.s7evensoftware.nosowallet.model.*
import com.s7evensoftware.nosowallet.ui.footer.SyncState
import com.s7evensoftware.nosowallet.util.Log
import io.realm.kotlin.Realm
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import kotlin.experimental.xor

class mpCripto {
    companion object {

        fun CreateNewAddress(): WalletObject {
            var MyData = WalletObject()
            var Address:String
            var KeysPair: KeyPair

            KeysPair = SignerUtils.GenerateECKeyPair(KeyType.SECP256K1)
            Address = GetAddressFromPublicKey(KeysPair.PublicKey!!)

            MyData.Hash = Address
            MyData.Custom = ""
            MyData.PublicKey = KeysPair.PublicKey
            MyData.PrivateKey = KeysPair.PrivateKey
            MyData.Balance = 0
            MyData.Pending = 0
            MyData.Score = 0
            MyData.LastOP = 0
            return MyData
        }

        fun CreateNewAddress(public:String, private:String): WalletObject {
            var MyData = WalletObject()
            var Address:String
            var KeysPair: KeyPair

            KeysPair = KeyPair()
            KeysPair.PublicKey = public
            KeysPair.PrivateKey = private
            Address = GetAddressFromPublicKey(KeysPair.PublicKey!!)

            MyData.Hash = Address
            MyData.Custom = ""
            MyData.PublicKey = KeysPair.PublicKey
            MyData.PrivateKey = KeysPair.PrivateKey
            MyData.Balance = 0
            MyData.Pending = 0
            MyData.Score = 0
            MyData.LastOP = 0
            return MyData
        }

        fun XorEncode(key:String, source: String):String{
            var result = ""

            for(i in source.indices){
                val c:Byte = if(key.isNotEmpty()){
                    key[0+(i % key.length)].code.toByte().xor(source[i].code.toByte())
                }else{
                    source[i].code.toByte()
                }
                val twoDigitsFix = if(Integer.toHexString(c.toInt()).length == 1){ "0${Integer.toHexString(c.toInt())}"}else{ Integer.toHexString(c.toInt()) }
                result += twoDigitsFix.lowercase()
            }

            return result
        }

        fun XorDecode(key:String, source: String):String{
            var result = ""

            for(i in 0 until source.length/2){
                var c:Byte = source.substring(i*2,i*2+2).toInt(16).toByte()
                if(key.isNotEmpty()){
                    c = key[0+(i % key.length)].code.toByte().xor(c)
                }
                result += c.toInt().toChar()
            }

            return result
        }

        fun GetAddressFromPublicKey(PubKey:String):String {
            var PubSHAHashed:String
            var Hash1:String
            var Hash2:String
            var clave:String
            var sumatoria:Int

            PubSHAHashed = HashSha256String(PubKey)
            Hash1 = HashMD160String(PubSHAHashed)
            Hash1 = BMHexto58(Hash1, BigInteger("58"))
            sumatoria = BMB58resumen(Hash1)
            clave = BMDecto58(sumatoria.toString())
            Hash2 = Hash1+clave
            return CoinChar +Hash2
        }

        suspend fun SendTo(
            origin:String,
            destination:String,
            amount:Long,
            reference:String,
            lastBlock:Long,
            sendFromAll:Boolean,
            addressList:MutableList<WalletObject>,
            lastNodeSelected: NodeInfo?,
            syncStatus:MutableState<SyncState>,
            realmDB: Realm
        ):String {
            var CurrTime:Long
            var Fee:Long
            var Remaining:Long
            var CoinsAvailable:Long
            var ArrayTrfrs = ArrayList<OrderData>()
            var Counter = 0
            var OrderHashString:String
            var TrxLine = 0
            var ResultOrderID:String
            var OrderString:String

            CurrTime = System.currentTimeMillis()/1000
            Fee = GetFee(amount)
            Remaining = amount+Fee
            CoinsAvailable = mpFunctions.GetAddressBalanceFromSummary(
                origin,
                realmDB
            ) - mpFunctions.getAddressPendingPays(origin, addressList, realmDB)
            if(Remaining <= CoinsAvailable || sendFromAll){
                OrderHashString = CurrTime.toString()

                //Order list with origin in front
                @Suppress("UNCHECKED_CAST")
                for(wallet in addressList){
                    if(wallet.Hash == origin){
                        addressList.remove(wallet)
                        addressList.add(0, wallet)
                        break
                    }
                }

                var Amount = amount // Amount == needed Amount
                while(Amount > 0){
                    if((addressList[Counter].Balance- mpFunctions.getAddressPendingPays(
                            addressList[Counter].Hash!!,
                            addressList,
                            realmDB
                        ))>0){
                        TrxLine += 1
                        ArrayTrfrs.add(
                            mpFunctions.SendFundsFromAddress(
                                addressList[Counter].Hash!!,
                                destination,
                                Amount,
                                Fee,
                                reference,
                                CurrTime,
                                TrxLine,
                                lastBlock,
                                addressList[Counter]
                            )
                        )
                        Fee -= ArrayTrfrs.last().AmountFee
                        Amount -= ArrayTrfrs.last().AmountTrf
                        OrderHashString += ArrayTrfrs.last().TrfrID
                        addressList[Counter].Outgoing = ArrayTrfrs.last().AmountTrf
                    }
                    Counter++
                }

                for(tr in ArrayTrfrs){
                    tr.OrderID = mpFunctions.getOrderHash(TrxLine.toString() + OrderHashString)
                    Log.e("mpCripto", "new OrderID -> ${tr.OrderID}")
                    tr.OrderLines = TrxLine
                }
                ResultOrderID = mpFunctions.getOrderHash(TrxLine.toString() + OrderHashString)
                Log.e("mpCripto", "ResultOrderID: $ResultOrderID")
                OrderString = mpFunctions.getPTCEcn("ORDER") +"ORDER "+TrxLine.toString()+" $"
                for(tr in ArrayTrfrs){
                    OrderString += mpFunctions.getStringFromOrder(tr) +" $"
                }
                OrderString = OrderString.substring(0, OrderString.length-2)
                return mpNetwork.sendOrder(OrderString, lastNodeSelected, syncStatus)
            }else{
                Log.e(
                    "mpCripto",
                    "Origin with not enough funds, available: $CoinsAvailable, required: $Remaining"
                )
                return MISSING_FUNDS
            }
        }

        fun getTransferHash(order:String):String {
            var result = HashSha256String(order)
            result = BMHexto58(result, BigInteger("58"))
            var b58sum = BMB58resumen(result)
            var clave = BMDecto58(b58sum.toString())
            return "tR$result$clave"
        }

        fun GetFee(amount:Long):Long {
            val result = amount/ Comisiontrfr
            if(result < MinimunFee){
                return MinimunFee
            }
            return result
        }

        fun getStringSigned(stringtoSign:String, privateKey: String):String {
            var MessageAsBytes:ByteArray
            var Signature: ByteArray

            MessageAsBytes = mpParser.SpecialBase64Decode(stringtoSign)

            Signature = SignerUtils.SignMessage(
                MessageAsBytes,
                Base64.decode(privateKey),
                KeyType.SECP256K1
            )

            return String(org.bouncycastle.util.encoders.Base64.encode(Signature))
        }

        fun VerifySignedString(
            stringtoverify:String,
            signedhash:String,
            publickey:String
        ):Boolean {
            var MessageAsBytes = org.bouncycastle.util.encoders.Base64.decode(stringtoverify)
            var Signature = org.bouncycastle.util.encoders.Base64.decode(signedhash)
            return SignerUtils.VerifySignature(
                Signature,
                MessageAsBytes,
                Base64.decode(publickey),
                KeyType.SECP256K1
            )
        }

        //Returns the SHA256 of a String in CAPITAL
        fun HashSha256String(StringToHash:String):String {
            var Source = StringToHash.toByteArray()
            val MessageDigestInstance = MessageDigest.getInstance("SHA-256")
            val Digest = MessageDigestInstance.digest(Source)
            val result = Digest.fold("", { str, it -> str + "%02x".format(it) })
            return result.uppercase() //Display the digest in capital letter ?? ECO: not sure why
        }

        //Returns the SHA1 of a String in CAPITAL
        fun HashSha1String(StringToHash:String):String {
            var Source = StringToHash.toByteArray()
            val MessageDigestInstance = MessageDigest.getInstance("SHA-1")
            val Digest = MessageDigestInstance.digest(Source)
            val result = Digest.fold("", { str, it -> str + "%02x".format(it) })
            return result
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

        fun getMD5of(string:String):String {
            val encoded = encodeMD5String(string)
            return toHex(encoded)
        }

        private fun encodeMD5String(source: String):ByteArray {
            val digest = MessageDigest.getInstance("MD5")
            return digest.digest(source.toByteArray())
        }

        private fun toHex(encoded:ByteArray):String {
            val hexCoded = StringBuilder()
            for (element in encoded) {
                val hex = Integer.toHexString(0xff and element.toInt())
                if (hex.length == 1) {
                    hexCoded.append('0')
                }
                hexCoded.append(hex)
            }
            return hexCoded.toString()
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

        fun BMDividir(numeroA:String, Divisor:Int): DivResult {
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

        // Method replicated form Nosowallet
        fun BMHexto58(numerohex:String, alphabetnumber:BigInteger):String {
            var decimalValue:String
            var ResultadoDiv: DivResult
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
            var ResultadoDiv: DivResult
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