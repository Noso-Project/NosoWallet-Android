package com.s7evensoftware.nosowallet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.ArrayList

class mpParser {
    companion object{

        fun showExplorer(importWalletTask: ActivityResultLauncher<Intent>) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
            }
            importWalletTask.launch(intent)
        }

        fun CustomizeAddress(target:WalletObject, customName:String, lastBlock:Long):Int{
            var TrfrHash:String;var OrderHash:String
            var CurrTime:Long
            var cont:Int

            if(target.Custom?.isEmpty() == true){
                return R.string.customize_error_1
            }

            if(customName.length < 5 || customName.length > 40){
                return R.string.customize_error_2
            }

            if(IsValidAddress(customName)){
                return R.string.customize_error_3
            }

            if(target.Balance < CustomizationFee){
                return R.string.customize_error_4
            }

            val tempWallet = target.Hash?.let { DBManager.getWallet(it) }

            if(tempWallet?.Custom?.isEmpty() != true){
                return R.string.customize_error_1
            }

            if(DBManager.isAliasUsed(customName)){
                return R.string.customize_error_5
            }

            CurrTime = System.currentTimeMillis()/1000
            TrfrHash = mpCripto.getTransferHash("$CurrTime${target.Hash}$customName$lastBlock")
            OrderHash = mpFunctions.getOrderHash("1$CurrTime$TrfrHash")
            return 0
        }

        fun SpecialBase64Decode(input:String):ByteArray {
            val indexList = ArrayList<Int>()

            for(c in input){
                indexList.add(B64Alphabet.indexOf(c))
            }

            var binaryString = ""
            for(i in indexList){
                var binary = Integer.toBinaryString(i)
                while(binary.length < 6) binary = "0"+binary
                binaryString += binary
            }

            var strAux = binaryString
            var tempByteArray = ArrayList<Byte>()

            while (strAux.length >= 8){
                val currentGroup = strAux.substring(0, 8)
                val intVal = Integer.parseInt(currentGroup, 2)
                Log.e("Parser","Int value -> $intVal")
                tempByteArray.add(intVal.toByte())
                strAux = strAux.substring(8)
            }

            val encodedByteArray = ByteArray(tempByteArray.size)

            for((i,b) in tempByteArray.withIndex()){
                encodedByteArray[i] = b
            }

            return encodedByteArray
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

        fun ImportQRWallet(keys:String, addressList: ArrayList<WalletObject>, pendingList: ArrayList<PendingData>):Boolean{
            val tokens = StringTokenizer(keys)
            if(tokens.countTokens() == 2){
                val publicKey = tokens.nextToken()
                val privateKey = tokens.nextToken()

                val testSignature = mpCripto.getStringSigned("VERIFICATION",privateKey)
                val verification = mpCripto.VerifySignedString("VERIFICATION", testSignature, publicKey)

                if(verification){
                    Log.e("mpParser","Valid keys found, inserting")
                    return mpFunctions.InsertAddress(mpCripto.CreateNewAddress(publicKey, privateKey), addressList, pendingList)
                }
            }
            return false
        }

        fun ImportWallet(context: Context, resultCode: Int, data: Intent?, addressList: ArrayList<WalletObject>, pendingList: ArrayList<PendingData>):Int{
            if(resultCode == Activity.RESULT_OK){
                data?.data.also {
                    if(getFileExtension(getFileName(context, it!!)) == ".pkw" || getFileExtension(getFileName(context,
                            it
                        )) == ".pkw.bak"
                    ){
                        Log.e("mParser","Wallet File Imported - OK")
                        return parseExternalWallet(context, it, addressList,pendingList)
                    }else{
                        Log.e("mParser","Wrong file extension - ERR")
                        return -2
                    }
                }
            }
            return -3
        }

        fun ExportWallet(resultCode: Int, data: Intent?, listaDirecciones: ArrayList<WalletObject>):Int {
            if(resultCode == Activity.RESULT_OK){
                data?.data.also {
                    return mpDisk.ExportWallet(it, listaDirecciones)
                }
            }

            if(resultCode == Activity.RESULT_CANCELED){
                return -1
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
            fileRef: File,
            addressList: ArrayList<WalletObject>,
            pendingList: ArrayList<PendingData>,
            isGhost: Boolean
        ):Boolean{
            val fileReference = FileInputStream(fileRef)
            var bytes = ByteArray(fileRef.length().toInt())

            try{
                val buffer = BufferedInputStream(fileReference)
                buffer.read(bytes, 0, bytes.size)
                buffer.close()
            }catch (e:Exception){
                fileReference.close()
                return false
            }
            return parseWallet(bytes, addressList, pendingList, isGhost) > 0
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
            return parseWallet(bytes, addressList, pendingList, false)
        }

        fun parseWallet(upbytes:ByteArray, addressList: ArrayList<WalletObject>, pendingList: ArrayList<PendingData>, isGhost:Boolean):Int{
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
                    }else{
                        Log.e("mpParser","Ignoring wallet, already exists: "+newWallet.Hash)
                    }
                }else{
                    Log.e("mpParser", "Ignoring invalid wallet : $Address")
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
            if(isGhost){
                mpDisk.SaveErased(addressList)
            }else{
                mpDisk.SaveWallet(addressList)
            }
            return nuevos
        }

        fun getFileName(context: Context, uri: Uri): String {
            var result:String? = null
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
                result = uri.path?:""
                val cut = result.lastIndexOf('/')
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
            var extPart = ".null"
            try{
                extPart = fileName.substring(fileName.length-4)
                // In case the file has a backup file extension format: *.pkw.bak
                if(extPart.uppercase() == ".BAK"){
                    extPart = fileName.substring(fileName.length-8)
                }
            }catch (e:Exception){
                Log.e("mpParser","Failed to obtaine extension: "+e.message)
            }
            return extPart.lowercase()
        }
    }
}