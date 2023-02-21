package com.s7evensoftware.nosowallet.nosocore

import android.content.Context
import android.net.Uri
import com.s7evensoftware.nosowallet.*
import com.s7evensoftware.nosowallet.model.*
import com.s7evensoftware.nosowallet.util.Log
import io.realm.kotlin.Realm
import java.io.*
import java.math.BigInteger

class mpDisk {
    companion object {

        lateinit var context:Context

        @JvmName("setContext1") // fix for declaration name
        fun setContext(context: Context){
            Companion.context = context
        }

        suspend fun VerifyFiles(addressList: List<WalletObject>, realmDB:Realm): WalletsPack {
            val walletsPack = WalletsPack()

            if(!directoryexist(UpdatesDirectory)){
                CreateDir(UpdatesDirectory)
            }

            if(!directoryexist(LogsDirectory)){
                CreateDir(LogsDirectory)
            }

            if(!fileexist(WalletFilename)){
                Log.e("mpDisk", "Wallet Creation: $WalletFilename")
                CreateWallet().let {
                    walletsPack.normalList = it
                }
            }else{
                Log.e("mpDisk", "Loading Wallet: $WalletFilename")
                LoadWallet(WalletFilename, false, addressList).let {
                    walletsPack.normalList = it
                }
            }

            if(fileexist(GhostFilename)){
                Log.e("mpDisk", "Loading Ghost Wallet: $GhostFilename")
                LoadWallet(GhostFilename, true, addressList).let {
                    walletsPack.ghostList = it
                }
            }

            if(fileexist(SumaryFilePath)){
                Log.e("mpDisk", "Loading Summary $SumaryFilePath")
                LoadSummary(realmDB)
            }
            return walletsPack
        }

        //New method, loaded to DB
        suspend fun LoadSummary(realmDB: Realm){
            val summRef = File(
                context.getExternalFilesDir(null)!!.path
                    +File.separator
                    + NOSPath
                    +File.separator
                    + SumaryDirectory
                    +File.separator
                    + SumaryFileName
            )
            val fileReference = FileInputStream(summRef)
            val addressSummary = ArrayList<SumaryData>()
            var bytes = ByteArray(106)

            try{
                var count = 0
                val buffer = BufferedInputStream(fileReference)
                var read = buffer.read(bytes, 0, bytes.size)
                while(read != -1){
                    val sumData = SumaryData()
                    sumData.Hash = String(bytes.copyOfRange(1,bytes[0].toInt()+1))
                    sumData.Custom = String(bytes.copyOfRange(42,42+bytes[41].toInt()))

                    val balanceArray = bytes.copyOfRange(82,90)
                    balanceArray.reverse() // Big Endian to Little Endian conversion
                    sumData.Balance = BigInteger(balanceArray).toLong()

                    val scoreArray = bytes.copyOfRange(91,98)
                    scoreArray.reverse() // Big Endian to Little Endian conversion
                    sumData.Score = BigInteger(scoreArray).toLong()

                    val lastopArray = bytes.copyOfRange(99,106)
                    lastopArray.reverse() // Big Endian to Little Endian conversion
                    sumData.LastOP = BigInteger(lastopArray).toLong()

                    addressSummary.add(sumData)
                    count++
                    read = buffer.read(bytes, 0, bytes.size)
                }
                Log.e("mpDisk", "$count Wallets Loaded from Summary")
                buffer.close()
                DBManager.clearSummary(realmDB) // Erase current Summary data
                DBManager.addSummaryFromList(addressSummary, realmDB) // Write list to the DB
                DeleteSummaryFiles() // Erase .zip and extracted data
            }catch (e:Exception){
                fileReference.close()
            }
        }

        fun DeleteSummaryFiles(){
            try{
                val summRef = File(
                    context.getExternalFilesDir(null)!!.path
                        +File.separator
                        + NOSPath
                        +File.separator
                        + SumaryDirectory
                )

                for(child in summRef.listFiles()){
                    child.delete()
                }
                summRef.delete()

                val zipFile = File(
                    context.getExternalFilesDir(null)!!.path
                        +File.separator
                        + NOSPath
                        +File.separator
                        + ZipSumaryFileName
                )
                zipFile.delete()
                Log.e("mpDisk", "Delete summary zip and unzipped files - OK")
            }catch (s:SecurityException){
                Log.e("mpDisk", "Error while deleting files: " + s.message)
            }catch (e:java.lang.Exception){
                Log.e("mpDisk", "Unhandled Error while deleting files: " + e.message)
            }
        }

        fun CreateWallet(): List<WalletObject> {
            val addressList = mutableListOf<WalletObject>()
            if(!fileexist(WalletFilename)){
                addressList.add(mpCripto.CreateNewAddress())
                SaveWallet(addressList)
            }
            return addressList
        }

        fun SaveWallet(listaDirecciones: List<WalletObject>): Int{
            val NOSOroot = File(context.getExternalFilesDir(null)!!.path+File.separator+ NOSPath)
            val FileWallet = File(NOSOroot.path, WalletFilename)
            val outputByteArray = ByteArrayOutputStream()
            val dataoutputStream = DataOutputStream(outputByteArray)
            writeWalletFile(dataoutputStream, listaDirecciones)

            try{
                //Write wallet.pkw file
                val fileOutputStream = FileOutputStream(FileWallet)
                outputByteArray.writeTo(fileOutputStream)
                outputByteArray.flush();outputByteArray.close()
                fileOutputStream.flush();fileOutputStream.close()

                //Write wallet.pkw.bak file
                val backUpWallet = File(FileWallet.parentFile.path+File.separator+ WalletBackUpFile)
                FileWallet.copyTo(backUpWallet, true)
                Log.e("mpDisk", "Wallet file written - OK")
                return R.string.general_export_success
            }catch (e:Exception){
                dataoutputStream.close()
                outputByteArray.close()
                Log.e("mpDisk", "Unable to write wallet file - ERR")
                return R.string.general_export_error
            }
        }

        fun SaveErased(ghostList: List<WalletObject>) {
            val NOSOroot = File(context.getExternalFilesDir(null)!!.path+File.separator+ NOSPath)
            val FileWallet = File(NOSOroot.path, GhostFilename)
            val outputByteArray = ByteArrayOutputStream()
            val dataoutputStream = DataOutputStream(outputByteArray)
            writeWalletFile(dataoutputStream, ghostList)

            try{
                //Write ghost.pkw file
                val fileOutputStream = FileOutputStream(FileWallet)
                outputByteArray.writeTo(fileOutputStream)
                outputByteArray.flush();outputByteArray.close()
                fileOutputStream.flush();fileOutputStream.close()
                Log.e("mpDisk", "Ghost file written - OK")
            }catch (e:Exception){
                dataoutputStream.close()
                outputByteArray.close()
                Log.e("mpDisk", "Unable to write ghost file wallet file - ERR")
            }
        }

        fun ExportWallet(FileWallet: Uri?, listaDirecciones:List<WalletObject>): Int {
            val outputByteArray = ByteArrayOutputStream()
            val dataoutputStream = DataOutputStream(outputByteArray)
            writeWalletFile(dataoutputStream, listaDirecciones)

            try{
                val fileOutputStream = context.contentResolver.openOutputStream(FileWallet!!)
                outputByteArray.writeTo(fileOutputStream)

                outputByteArray.flush();fileOutputStream?.flush()
                fileOutputStream?.close();outputByteArray.close()
                return R.string.general_export_success
            }catch (e:Exception){
                dataoutputStream.close();outputByteArray.close()
                Log.e("mpDisk", "Unable to write wallet file - ERR")
                return R.string.general_export_error
            }
        }

        private fun writeWalletFile(dataoutputStream: DataOutputStream, listaDirecciones: List<WalletObject>) {
            for(wallet in listaDirecciones){
                var hashArray = wallet.Hash?.toByteArray()?.copyOf(40)
                var customArray:ByteArray? = ByteArray(40)
                if(wallet.Custom != null){
                    customArray = wallet.Custom?.toByteArray()?.copyOf(40)
                }
                val publicArray = wallet.PublicKey?.toByteArray()?.copyOf(255)
                val privateArray = wallet.PrivateKey?.toByteArray()?.copyOf(255)

                dataoutputStream.write(wallet.Hash?.length?.toByte()?.toInt()?:0) //String header -> # of chars
                dataoutputStream.write(hashArray)
                dataoutputStream.write(wallet.Custom?.length?.toByte()?.toInt()?:0) //String header -> # of chars
                dataoutputStream.write(customArray)
                dataoutputStream.write(wallet.PublicKey?.length?.toByte()?.toInt()?:0) //String header -> # of chars
                dataoutputStream.write(publicArray)
                dataoutputStream.write(wallet.PrivateKey?.length?.toByte()?.toInt()?:0) //String header -> # of chars
                dataoutputStream.write(privateArray)
                dataoutputStream.writeLong(wallet.Balance)
                dataoutputStream.writeLong(wallet.Pending)
                dataoutputStream.writeLong(wallet.Score)
                dataoutputStream.writeLong(wallet.LastOP)
            }
        }

        fun LoadWallet(
            wallet: String,
            isGhost:Boolean,
            addressList: List<WalletObject>
        ): List<WalletObject> {
            if(fileexist(wallet)){
                return mpParser.parseInternalWallet(
                    addressList,
                    File(
                        context.getExternalFilesDir(null)!!.path
                                + File.separator
                                + NOSPath
                                + File.separator
                                + wallet
                    ),
                    isGhost
                )
            }
            return listOf()
        }


        fun CreateOptionsFile() {
            val NOSOroot = File(context.getExternalFilesDir(null)!!.path+File.separator+ NOSPath)

            if(NOSOroot.mkdirs()){
                Log.e("mpDisk", "Directory NOSODATA created - OK")
            }else{
                Log.e("mpDisk", "Directory creation failed - ERROR")
            }
        }


        /*Deprecated method
        fun LoadOptions() {
            val FileOptions = File(context.getExternalFilesDir(null)!!.path+File.separator+NOSPath, OptionsFileName)
            ObjectInputStream(FileInputStream(FileOptions)).use {
                val Options = it.readObject()
                MainActivity.UserOptions = Options as Options
                Log.e("mpDisk","Settings filed loaded - OK")
            }
        }*/

        /* Create dir inside NOSODATA */
        fun CreateDir(dirName:String){
            val file = File(
                context.getExternalFilesDir(null)!!.path
                        +File.separator
                        + NOSPath
                        +File.separator
                        +dirName)
            if(file.mkdirs()){
                Log.e("mpDisk", "Directory creation: $dirName - OK")
            }else{
                Log.e("mpDisk", "Directory creation: $dirName - ERR")
            }
        }

        /* Create Summary file inside NOSODATA */
        fun CreateSummFile(){
            val file = File(
                context.getExternalFilesDir(null)!!.path
                        +File.separator
                        + NOSPath
                        +File.separator
                        + ZipSumaryFileName
            )
            if(!file.exists()){
                if(file.createNewFile()){
                    Log.e("mpDisk", "Summary file created - OK")
                }else{
                    Log.e("mpDisk", "Error creating summary.zip - ERR")
                }
            }
        }

        fun appendLog(origen:String, content:String, localContext: Context = context){
            val fileLog = File(
                localContext.getExternalFilesDir(null)!!.path
                        +File.separator
                        + NOSPath
                        +File.separator
                        + LogsDirectory
                        +File.separator
                        + LogsFilename
            )

            try{
                if(!fileLog.exists()){
                    fileLog.parentFile.mkdirs()
                    fileLog.createNewFile()
                }else{
                    if(fileLog.length() >= 51200){
                        if(fileLog.delete()){
                            Log.e("mpDisk", "Log file cleared - OK")
                            fileLog.createNewFile()
                        }else{
                            Log.e("mpDisk", "Error clearing Log file - ERR")
                        }
                    }
                }

                val buffWrt = BufferedWriter(FileWriter(fileLog, true))
                val currentTime = System.currentTimeMillis()
                val formattedLine = mpFunctions.getDateFromUNIX(currentTime) +" "+
                        mpFunctions.getTimeFromUNIX(currentTime) +" : "+
                                    origen+" -> "+
                                    content
                buffWrt.append(formattedLine)
                buffWrt.newLine()
                buffWrt.close()
            }catch (i:IOException){
                Log.e("mpDisk", "Error writing log file: " + i.message)
            }
        }

        /* Check if file exists in NOSODATA directory */
        fun fileexist(fileName: String, localContext: Context = context): Boolean {
            val file = File(
                localContext.getExternalFilesDir(null)!!.path
                        +File.separator
                        + NOSPath
                        +File.separator
                        +fileName)
            return file.exists()
        }

        /* Check if dir exists in NOSODATA directory */
        fun directoryexist(pathName: String, localContext: Context = context): Boolean {
            val file = File(
                localContext.getExternalFilesDir(null)!!.path
                +File.separator
                + NOSPath
                +File.separator
                +pathName)
            return file.exists()
        }
    }
}