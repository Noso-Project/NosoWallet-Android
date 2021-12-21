package com.s7evensoftware.nosowallet

import android.content.Context
import android.util.Log
import java.io.*

class mpdisk {
    companion object {

        fun VerificarArchivos(context: Context){
            var contador = 0

            if(!directoryexist(context,UpdatesDirectory)){
                CreateDir(context, UpdatesDirectory)
            }

            if(!directoryexist(context, MarksDirectory)){
                CreateDir(context, MarksDirectory)
            }

            if(!directoryexist(context, LogsDirectory)){
                CreateDir(context, LogsDirectory)
            }

            if(!directoryexist(context, ExceptLogFilename)){
                //CreateDir(context, UpdatesDirectory)
            }

            if(!fileexist(context, MainActivity.UserOptions.Wallet)){
                Log.e("mpCripto","Crear wallet: "+MainActivity.UserOptions.Wallet)
                CrearWallet(context)
            }else{
                Log.e("mpCripto","Cargar wallet: "+MainActivity.UserOptions.Wallet)
                CargarWallet(context, MainActivity.UserOptions.Wallet)
                mpCripto.GetAddressFromPublicKey(MainActivity.Listadirecciones[0].PublicKey!!)
            }
        }

        fun CrearWallet(context: Context) {
            if(!fileexist(context, WalletFilename)){
                MainActivity.Listadirecciones.add(mpCripto.CreateNewAddress())
                val NOSOroot = File(context.getExternalFilesDir(null)!!.path+File.separator+NOSPath)

                val FileWallet = File(NOSOroot.path, WalletFilename)

                ObjectOutputStream(FileOutputStream(FileWallet)).use {
                    it.writeObject(MainActivity.Listadirecciones)
                    MainActivity.UserOptions.Wallet = WalletFilename
                    Log.e("mpDisk","Escribiendo Informacion de Wallet - OK")
                }
            }
        }

        fun SaveWallet(context: Context){
            val NOSOroot = File(context.getExternalFilesDir(null)!!.path+File.separator+NOSPath)
            val FileWallet = File(NOSOroot.path, WalletFilename)
            ObjectOutputStream(FileOutputStream(FileWallet)).use {
                it.writeObject(MainActivity.Listadirecciones)
                Log.e("mpDisk","Escribiendo Informacion de Wallet - OK")
            }
        }

        fun CargarWallet(context: Context, wallet:String){
            //var contador = 0
            if(fileexist(context, wallet)){
                val FileWallet = File(context.getExternalFilesDir(null)!!.path+File.separator+NOSPath, wallet)
                ObjectInputStream(FileInputStream(FileWallet)).use {
                    MainActivity.Listadirecciones = it.readObject() as ArrayList<WalletObject>
                    Log.e("mpDisk","Cargar Wallet - OK")
                }
            }
        }

        fun CrearArchivoOpciones(context:Context) {
            val Options = Options()
                /*mapOf(
                "language" to 0,
                "Port" to 8080,
                "GetNodes" to false,
                "PoolInfo" to "",
                "Wallet" to "NOSODATA"+File.separator+"wallet.pkw",
                "AutoServer" to false,
                "AutoConnect" to true,
                "Auto_Updater" to false,
                "JustUpdated" to false,
                "VersionPage" to "https://nosocoin.com",
                "UsePool" to false
            )*/

            val NOSOroot = File(context.getExternalFilesDir(null)!!.path+File.separator+NOSPath)

            if(NOSOroot.mkdirs()){
                Log.e("mpDisk","Directory NOSODATA creado - OK")
                val FileOptions = File(NOSOroot.path, OptionsFileName)

                ObjectOutputStream(FileOutputStream(FileOptions)).use {
                    it.writeObject(Options)
                    MainActivity.UserOptions = Options
                    Log.e("mpDisk","Options file written - OK")
                }
            }else{
                Log.e("mpDisk","Creacion de directorio fallo - ERROR")
            }

        /* ALT FILE CREATION METHOD
            val resolver = context.contentResolver
            val FileOptions = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, OptionsFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/psk")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS+File.separator+NOSPath)
            }

            val fileReference = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), FileOptions)
            val outputStream = resolver.openOutputStream(fileReference!!)

            ObjectOutputStream(outputStream)
                .use {
                    it.writeObject(Options)
                    MainActivity.UserOptions = Options
                    Log.e("mpDisk", "Escribi el archivo de opciones")
                }
        */
        }

        fun CargarOpciones(context:Context) {
            val FileOptions = File(context.getExternalFilesDir(null)!!.path+File.separator+NOSPath, OptionsFileName)
            ObjectInputStream(FileInputStream(FileOptions)).use {
                val Options = it.readObject()
                MainActivity.UserOptions = Options as Options
                Log.e("mpDisk","Archivo de configuracion cargado - OK")
            }
        }

        /* Create dir inside NOSODATA */
        fun CreateDir(context: Context, dirName:String){
            val file = File(
                context.getExternalFilesDir(null)!!.path
                        +File.separator
                        +NOSPath
                        +File.separator
                        +dirName)
            if(file.mkdirs()){
                Log.e("mpDisk","Crear Directorio: "+dirName+" - OK")
            }else{
                Log.e("mpDisk","Crear Directorio: "+dirName+" - ERR")
            }
        }

        /* Create Summary file inside NOSODATA */
        fun CreateSummFile(context: Context){
            val file = File(
                context.getExternalFilesDir(null)!!.path
                        +File.separator
                        +NOSPath
                        +File.separator
                        +SummaryFileName)
            if(file.exists()){
                Log.e("mpDisk","Ya existe Summary.zip - OK")
            }else{
                if(file.createNewFile()){
                    Log.e("mpDisk","Summary file created - OK")
                }else{
                    Log.e("mpDisk","Error creating summary.zip - OK")
                }
            }
        }

        /* Check if file exists in NOSODATA directory */
        fun fileexist(context:Context, fileName: String): Boolean {
            val file = File(
                context.getExternalFilesDir(null)!!.path
                        +File.separator
                        +NOSPath
                        +File.separator
                        +fileName)
            return file.exists()
        }

        /* Check if dir exists in NOSODATA directory */
        fun directoryexist(context:Context, pathName: String): Boolean {
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