package com.s7evensoftware.nosowallet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import java.io.*

class mpParser {
    companion object{

        fun SendCrypto(Destination: String, Amount: String, Reference: String){
            var reference:String
            var Monto:Int; var Comision:Int
            var MontoToShow:Int;var ComisionToShow:Int
            var Contador:Int
            var Restante:Int
            var ArrayTrfrs = ArrayList<OrderData>()
            var CurrTime:String
            var TrxLinea:Int = 0
            var OrderHashString:String
            var OrderString:String
            var AliasIndex:Int
            var Procesar:Boolean = true
            var ResultOrderID = ""
            var CoinsAvailable:Int

            if(Destination.isBlank() || Destination.isEmpty() || Amount.isEmpty() || Amount.isBlank()){
                Log.e("mpParser","Transferencia fallida, parametros invalidos")
                return
            }

            if(!IsValidAddress(Destination)){
                Log.e("mpParser","Transferencia fallida, invalid destination address")
                return
            }

            Monto = Integer.parseInt(Amount)

            if(Reference.isEmpty() || Reference.isBlank()){
                reference = "null"
            }

            if(Monto < 0){
                Log.e("mpParser","Transferencia fallida, invalid amount (negativa or zero)")
                return
            }

            // Transaction Process
            Comision = mpCoin.GetFee(Monto)
            MontoToShow = Monto
            ComisionToShow = Comision
            Restante = Monto+Comision

            // Get Wallet Balance
            //CoinsAvailable =












        }


        fun showExplorer(importWalletTask: ActivityResultLauncher<Intent>) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
            }
            importWalletTask.launch(intent)
        }

        fun IsValid58(base58Text:String):Boolean {
            for(c in base58Text){
                if(B58Alphabet.indexOf(c) == -1){
                    return false
                }
            }
            return true
        }

        fun IsValidAddress(address:String):Boolean {
            if(address[0].equals('N') && address.length > 20){
                var OrigHash = address.substring(1, address.length-2)
                if(IsValid58(OrigHash)){
                    var Clave = mpCripto.BMDecto58(mpCripto.BMB58resumen(OrigHash).toString())
                    OrigHash = CoinChar+OrigHash+Clave
                    if(OrigHash == address){
                        return true
                    }
                }
            }
            return false
        }

        fun ImportWallet(context: Context, resultCode: Int, data: Intent?){
            var Cartera = ""
            var CarteraFile: File
            var DatoLeido:ArrayList<WalletObject>
            var Contador = 0
            var Nuevos = 0

            if(resultCode == Activity.RESULT_OK){
                data?.data.also {
                    if(parseKotlinWallet(context, it!!)){
                        Log.e("mParser","Kotlin Wallet Imported - OK")
                    }else if(parsePascalWallet(context, it!!)){
                        Log.e("mParser","Pascal Wallet Imported - OK")
                    }else{
                        Log.e("mParser","Unable to parse wallet file - ERR")
                    }
                }
            }
        }

        fun WalletExists(wallet:WalletObject):Boolean {
            for(w in MainActivity.Listadirecciones){
                if(wallet.Hash.equals(w.Hash)){
                    return true
                }
            }
            return false
        }

        fun parseKotlinWallet(context: Context, uriRef: Uri):Boolean{
            val resolver = context.contentResolver
            val fileReference = resolver.openInputStream(uriRef)

            try {
                ObjectInputStream(fileReference).use {
                    val newWallets = it.readObject() as ArrayList<WalletObject>
                    var nuevos = 0
                    for(wallet in newWallets){
                        if(IsValidAddress(wallet.Hash!!)){
                            if(!WalletExists(wallet)){
                                MainActivity.Listadirecciones.add(wallet)
                                nuevos++
                                Log.e("mpParser","## Wallet Info: ")
                                Log.e("mpParser","# Address: "+wallet.Hash)
                                Log.e("mpParser","# PublicKey: "+wallet.PublicKey)
                                Log.e("mpParser","# PrivateKey: "+wallet.PrivateKey)
                            }else{
                                Log.e("mpParser","Ignoring wallet already exists: "+wallet.Hash)
                            }
                        }else{
                            Log.e("mpParser","Ignoring invalid wallet : "+wallet.Hash)
                        }
                    }
                    if(nuevos>0){
                        mpdisk.SaveWallet(context)
                        Log.e("mpParser","Saving with $nuevos new wallet(s)")
                    }
                }
                return true
            }catch (e:Exception){
                Log.e("mpParser","Error: "+e.message)
                fileReference?.close()
                return false
            }
        }

        fun parsePascalWallet(context: Context, uriRef: Uri):Boolean{
            val resolver = context.contentResolver
            val fileReference = resolver.openInputStream(uriRef)
            val bytes = ByteArray(getFileSize(context, uriRef).toInt())

            try{
                val buffer = BufferedInputStream(fileReference)
                buffer.read(bytes, 0, bytes.size)
                buffer.close()
            }catch (e:Exception){
                fileReference?.close()
                return false
            }

            val rawData = String(bytes)
            var impData = rawData
            var nuevos = 0

            while(impData.indexOf("N") != -1){

                var addressPart = ""
                var PublicKey = ""
                var PrivateKey = ""

                impData = impData.substring(impData.indexOf("N"))

                for(c in impData){
                    if(Character.getNumericValue(c) >= 0){
                        addressPart += c
                    }else{
                        impData = impData.substring(impData.indexOf(c))
                        break
                    }
                }

                impData = impData.substring(impData.indexOf("X")+1)
                PublicKey = impData.substring(0, 88)

                impData = impData.substring(89)
                impData = impData.substring(impData.indexOf(",")+1)

                PrivateKey = impData.substring(0, 44)
                impData = impData.substring(45)

                if(IsValidAddress(addressPart)){
                    val newWallet = WalletObject()
                    newWallet.Hash = addressPart
                    newWallet.PublicKey = PublicKey
                    newWallet.PrivateKey = PrivateKey

                    if(!WalletExists(newWallet)){
                        MainActivity.Listadirecciones.add(newWallet)
                        nuevos++

                        Log.e("mpParser","## Wallet Info: ")
                        Log.e("mpParser","# Address: $addressPart")
                        Log.e("mpParser","# PublicKey: $PublicKey")
                        Log.e("mpParser","# PrivateKey: $PrivateKey")
                    }else{
                        Log.e("mpParser","Ignoring wallet already exists: "+newWallet.Hash)
                    }
                }else{
                    Log.e("mpParser","Ignoring invalid wallet : "+addressPart)
                }
            }
            return  true
        }

        fun getFileName(context: Context, uri: Uri): String? {
            var result: String? = null
            if (uri.scheme.equals("content")) {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        result =
                            cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    }
                } finally {
                    cursor?.close()
                }
            }
            if (result == null) {
                result = uri.path
                val cut = result!!.lastIndexOf('/')
                if (cut != -1) {
                    result = result.substring(cut + 1)
                }
            }
            return result
        }

        fun getFileSize(context: Context, uri: Uri): Long {
            var result: Long = 0
            if (uri.scheme.equals("content")) {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        result =
                            cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))
                    }
                } finally {
                    cursor?.close()
                }
            }
            return result
        }

        fun getFileExtension(fileName:String):String {
            var extPart = fileName.substring(fileName.length-4)
            // In case the file has a backup file extension format: *.pkw.bak
            if(extPart.uppercase().equals(".BAK")){
                extPart = fileName.substring(fileName.length-8)
            }
            return extPart
        }
    }
}