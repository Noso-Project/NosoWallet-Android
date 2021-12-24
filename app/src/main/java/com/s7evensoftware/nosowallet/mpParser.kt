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

        fun ImportWallet(context: Context, resultCode: Int, data: Intent?, addressList: ArrayList<WalletObject>, pendingList: ArrayList<PendingData>):Int{
            if(resultCode == Activity.RESULT_OK){
                data?.data.also {
                    if(getFileExtension(it!!.lastPathSegment!!).equals(".pkw") || getFileExtension(it!!.lastPathSegment!!).equals(".pkw.bak")){
                        Log.e("mParser","Wallet File Imported - OK")
                        return parseExternalWallet(context, it!!, addressList,pendingList)
                    }else{
                        Log.e("mParser","Wrong file extension - ERR")
                        return -2
                    }
                }
            }
            return -3
        }

        fun ExportWallet(context: Context, resultCode: Int, data: Intent?, listaDirecciones: ArrayList<WalletObject>):Int {
            if(resultCode == Activity.RESULT_OK){
                data?.data.also {
                    return mpDisk.ExportWallet(context, it, listaDirecciones)
                }
            }
            return R.string.general_export_error
        }

        fun WalletExists(wallet:WalletObject, listaDirecciones: ArrayList<WalletObject>):Boolean {
            for(w in listaDirecciones){
                if(wallet.Hash.equals(w.Hash)){
                    return true
                }
            }
            return false
        }

        fun parseInternalWallet(
            context: Context,
            fileRef: File,
            addressList: ArrayList<WalletObject>,
            pendingList: ArrayList<PendingData>
        ):Boolean{
            val fileReference = FileInputStream(fileRef)
            var bytes = ByteArray(fileRef.length().toInt())

            try{
                val buffer = BufferedInputStream(fileReference)
                buffer.read(bytes, 0, bytes.size)
                buffer.close()
            }catch (e:Exception){
                fileReference?.close()
                return false
            }
            return parseWallet(context, bytes, addressList, pendingList) > 0
        }

        fun parseExternalWallet(context: Context, uriRef: Uri, addressList: ArrayList<WalletObject>, pendingList: ArrayList<PendingData>):Int{
            val resolver = context.contentResolver
            val fileReference = resolver.openInputStream(uriRef)
            var bytes = ByteArray(getFileSize(context, uriRef).toInt())

            try{
                val buffer = BufferedInputStream(fileReference)
                buffer.read(bytes, 0, bytes.size)
                buffer.close()
            }catch (e:Exception){
                fileReference?.close()
                return -1
            }
            return parseWallet(context, bytes, addressList, pendingList)
        }

        fun parseWallet(context:Context, upbytes:ByteArray, addressList: ArrayList<WalletObject>, pendingList: ArrayList<PendingData>):Int{
            var nuevos = 0
            var current:ByteArray? = upbytes.copyOfRange(0, 625)
            var bytes = upbytes.copyOfRange(626, upbytes.size)

            while(current != null){
                // Current Block = Wallet
                var Address = String(current.copyOfRange(1,current[0].toInt()+1))
                var Custom = String(current.copyOfRange(43,43+current[42].toInt()+1))
                var PublicKey = String(current.copyOfRange(83,83+current[82].toInt()))
                var PrivateKey = String(current.copyOfRange(339,339+current[338].toInt()))

                if(IsValidAddress(Address)){
                    val newWallet = WalletObject()
                    newWallet.Hash = Address
                    newWallet.Custom = Custom
                    newWallet.PublicKey = PublicKey
                    newWallet.PrivateKey = PrivateKey

                    if(!WalletExists(newWallet,addressList)){
                        addressList.add(newWallet)
                        pendingList.add(PendingData())
                        nuevos++

                        Log.e("mpParser","## Wallet Info: ")
                        Log.e("mpParser","# Address: $Address")
                        Log.e("mpParser","# PublicKey: $PublicKey")
                        Log.e("mpParser","# PrivateKey: $PrivateKey")
                    }else{
                        Log.e("mpParser","Ignoring wallet already exists: "+newWallet.Hash)
                    }
                }else{
                    Log.e("mpParser","Ignoring invalid wallet : "+Address)
                }

                // Search next block
                if(bytes.size >= 626){
                    current = bytes.copyOfRange(0, 625)
                    bytes = bytes.copyOfRange(626, bytes.size)
                }else{
                    Log.e("mpParser","No more blocks")
                    current = null
                }
            }
            mpDisk.SaveWallet(context, addressList)
            return nuevos
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