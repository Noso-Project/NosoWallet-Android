package com.s7evensoftware.nosowallet

import android.content.Context
import android.net.Uri
import java.io.*
import java.math.BigInteger

class mpDisk {
    companion object {

        lateinit var context:Context

        @JvmName("setContext1") // fix for declaration name
        fun setContext(context: Context){
            this.context = context
        }

        fun VerifyFiles(addressList: ArrayList<WalletObject>, pendingList:ArrayList<PendingData> ){
            if(!directoryexist(UpdatesDirectory)){
                CreateDir(UpdatesDirectory)
            }

            if(!directoryexist(LogsDirectory)){
                CreateDir(LogsDirectory)
            }

            if(!fileexist(WalletFilename)){
                Log.e("mpDisk","Wallet Creation: "+ WalletFilename)
                CreateWallet(addressList, pendingList)
            }else{
                Log.e("mpDisk","Loading Wallet: "+WalletFilename)
                if(addressList.size < 1){
                    LoadWallet(WalletFilename, addressList, pendingList)
                }
            }

            if(fileexist(SumaryFilePath)){
                Log.e("mpDisk", "Loading Summary $SumaryFilePath")
                LoadSummary()
            }
        }

        //New method, loaded to DB
        fun LoadSummary(){
            val summRef = File(context.getExternalFilesDir(null)!!.path
                    +File.separator
                    +NOSPath
                    +File.separator
                    +SumaryDirectory
                    +File.separator
                    +SumaryFileName)
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
                Log.e("mpDisk","$count Wallets Loaded from Summary")
                buffer.close()
                DBManager.clearSummary() // Erase current Summary data
                DBManager.addSummaryFromList(addressSummary) // Write list to the DB
                DeleteSummaryFiles() // Erase .zip and extracted data
            }catch (e:Exception){
                fileReference.close()
            }
        }

        fun DeleteSummaryFiles(){
            try{
                val summRef = File(context.getExternalFilesDir(null)!!.path
                        +File.separator
                        +NOSPath
                        +File.separator
                        +SumaryDirectory)

                for(child in summRef.listFiles()){
                    child.delete()
                }
                summRef.delete()

                val zipFile = File(context.getExternalFilesDir(null)!!.path
                        +File.separator
                        +NOSPath
                        +File.separator
                        +ZipSumaryFileName)
                zipFile.delete()
                Log.e("mpDisk","Delete summary zip and unzipped files - OK")
            }catch (s:SecurityException){
                Log.e("mpDisk","Error while deleting files: "+s.message)
            }catch (e:java.lang.Exception){
                Log.e("mpDisk","Unhandled Error while deleting files: "+e.message)
            }
        }

        fun CreateWallet(addressList: ArrayList<WalletObject>, pendingList: ArrayList<PendingData>) {
            if(!fileexist(WalletFilename)){
                addressList.add(mpCripto.CreateNewAddress())
                pendingList.add(PendingData())
                SaveWallet(addressList)
            }
        }

        fun SaveWallet(listaDirecciones: ArrayList<WalletObject>): Int{
            val NOSOroot = File(context.getExternalFilesDir(null)!!.path+File.separator+NOSPath)
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
                val backUpWallet = File(FileWallet.parentFile.path+File.separator+WalletBackUpFile)
                FileWallet.copyTo(backUpWallet, true)
                Log.e("mpDisk","Wallet file written - OK")
                return R.string.general_export_success
            }catch (e:Exception){
                dataoutputStream.close()
                outputByteArray.close()
                Log.e("mpDisk","Unable to write wallet file - ERR")
                return R.string.general_export_error
            }
        }

        fun ExportWallet(FileWallet: Uri?, listaDirecciones:ArrayList<WalletObject>): Int {
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
                Log.e("mpDisk","Unable to write wallet file - ERR")
                return R.string.general_export_error
            }
        }

        private fun writeWalletFile(dataoutputStream: DataOutputStream, listaDirecciones: ArrayList<WalletObject>) {
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
            addressList: ArrayList<WalletObject>,
            pendingList: ArrayList<PendingData>
        ){
            if(fileexist(wallet)){
                mpParser.parseInternalWallet(
                    File(context.getExternalFilesDir(null)!!.path
                            +File.separator
                            +NOSPath
                            +File.separator
                            +wallet),
                    addressList,
                    pendingList
                )
            }
        }


        fun CreateOptionsFile() {
            val NOSOroot = File(context.getExternalFilesDir(null)!!.path+File.separator+NOSPath)

            if(NOSOroot.mkdirs()){
                Log.e("mpDisk","Directory NOSODATA created - OK")
            }else{
                Log.e("mpDisk","Directory creation failed - ERROR")
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
                        +NOSPath
                        +File.separator
                        +dirName)
            if(file.mkdirs()){
                Log.e("mpDisk","Directory creation: $dirName - OK")
            }else{
                Log.e("mpDisk","Directory creation: $dirName - ERR")
            }
        }

        /* Create Summary file inside NOSODATA */
        fun CreateSummFile(){
            val file = File(
                context.getExternalFilesDir(null)!!.path
                        +File.separator
                        +NOSPath
                        +File.separator
                        + ZipSumaryFileName)
            if(!file.exists()){
                if(file.createNewFile()){
                    Log.e("mpDisk","Summary file created - OK")
                }else{
                    Log.e("mpDisk","Error creating summary.zip - ERR")
                }
            }
        }

        fun appendLog(origen:String, content:String){
            val fileLog = File(
                context.getExternalFilesDir(null)!!.path
                        +File.separator
                        +NOSPath
                        +File.separator
                        +LogsDirectory
                        +File.separator
                        +LogsFilename)

            if(!fileLog.exists()){
                fileLog.parentFile.mkdirs()
                fileLog.createNewFile()
            }else{
                if(fileLog.length() >= 1048576){
                    Log.e("mpDisk","Clearing Logfile - OK")
                    fileLog.delete()
                    fileLog.createNewFile()
                }
            }

            try{
                val buffWrt = BufferedWriter(FileWriter(fileLog, true))
                val currentTime = System.currentTimeMillis()
                val formattedLine = mpFunctions.getDateFromUNIX(currentTime)+" "+
                                    mpFunctions.getTimeFromUNIX(currentTime)+" : "+
                                    origen+" -> "+
                                    content
                buffWrt.append(formattedLine)
                buffWrt.newLine()
                buffWrt.close()
            }catch (i:IOException){
                Log.e("mpDisk","Error writing log file: "+i.message)
            }
        }

        /* Check if file exists in NOSODATA directory */
        fun fileexist(fileName: String): Boolean {
            val file = File(
                context.getExternalFilesDir(null)!!.path
                        +File.separator
                        +NOSPath
                        +File.separator
                        +fileName)
            return file.exists()
        }

        /* Check if dir exists in NOSODATA directory */
        fun directoryexist(pathName: String): Boolean {
            val file = File(
                context.getExternalFilesDir(null)!!.path
                +File.separator
                +NOSPath
                +File.separator
                +pathName)
            return file.exists()
        }
    }
}