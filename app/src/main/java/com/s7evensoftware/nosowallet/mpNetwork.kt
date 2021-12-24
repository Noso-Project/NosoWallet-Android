package com.s7evensoftware.nosowallet

import android.content.Context
import android.util.Log
import io.ktor.utils.io.core.*
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.zip.ZipFile

class mpNetwork {
    companion object {

        fun getNodeStatus(address:String, port:Int):NodeInfo{
            val serverAddress = InetSocketAddress(address, port)
            Log.e("mpNetwork","Requesting Node Status to $address")
            try{
                val clientSocket = Socket()
                clientSocket.connect(serverAddress, NODE_TIMEOUT)
                val clientChannel = PrintWriter(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())), true)
                val inputStreamReader = InputStreamReader(clientSocket.getInputStream())
                val bufferReader = BufferedReader(inputStreamReader)

                clientChannel.println("NODESTATUS")
                val response = bufferReader.readLine()
                clientSocket.close()
                return mpFunctions.stringToNodeInfo(response, address, port)
            }catch (t:SocketTimeoutException){
                Log.e("mpNetwork","Request to "+address+" -> Timed Out")
            }catch (e:Exception){
                Log.e("mpNetwork","Unexpected Connection Error")
            }
            return NodeInfo()
        }

        fun getSummary(context: Context, address: String, port: Int):Boolean{

            val serverAddress = InetSocketAddress(address, port)
            Log.e("mpNetwork","Requesting summary to $address")
            try {
                val clientSocket = Socket()
                clientSocket.connect(serverAddress, NODE_TIMEOUT)
                val clientChannel = PrintWriter(
                    BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())),
                    true
                )
                val inputStream = clientSocket.getInputStream()

                mpDisk.CreateSummFile(context)

                clientChannel.println("GETZIPSUMARY")

                val zSumaryFile = File(
                    context.getExternalFilesDir(null)!!.path
                            + File.separator
                            + NOSPath
                            + File.separator
                            + ZipSumaryFileName
                )

                val fos = FileOutputStream(zSumaryFile, false)
                var bytes = ByteArray(8192)
                var read: Int = inputStream.read(bytes)

                while (read != -1) {
                    fos.write(bytes, 0, read)
                    read = inputStream.read(bytes)
                }

                Log.e("Welcome", "Sumary Zip-File Written - OK")

                ZipFile(zSumaryFile.path).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        zip.getInputStream(entry).use { input ->

                            val uzFile = File(
                                context.getExternalFilesDir(null)!!.path +
                                        File.separator +
                                        NOSPath +
                                        File.separator +
                                        entry.name
                            )

                            uzFile.parentFile.mkdirs() // Create parent directories needed

                            File(uzFile.path).outputStream().use { output ->
                                input.copyTo(output)
                                Log.e("Welcome", "Sumary file decompressed - OK")
                                return true
                            }
                        }
                    }
                }
            }catch (t:SocketTimeoutException){
                Log.e("Welcome-Err","Request failed to "+address+" -> Timed Out")
            }catch (e:Exception){
                Log.e("Welcome-Err","Error retrieving summary file: "+e.message!!)
            }
            return false
        }

        fun getPendings(address: String, port: Int):String {
            val serverAddress = InetSocketAddress(address, port)
            Log.e("mpNetwork","Requesting Pendings to $address")
            try{
                val clientSocket = Socket()
                clientSocket.connect(serverAddress, NODE_TIMEOUT)

                val clientChannel = PrintWriter(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())), true)
                val inputStreamReader = InputStreamReader(clientSocket.getInputStream())
                val bufferReader = BufferedReader(inputStreamReader)

                clientChannel.println("NSLPEND")

                bufferReader.use { response ->
                    val line = response.readLine()
                    response.close()
                    return line
                }
            }catch (t: SocketTimeoutException){
                Log.e("mpNetwork","Request to $address -> Timed Out")
            }catch (e:Exception){
                Log.e("mpNetwork","Unexpected Connection Error")
            }
            return ""
        }
    }
}