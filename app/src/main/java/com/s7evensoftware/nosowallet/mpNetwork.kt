package com.s7evensoftware.nosowallet

import android.content.Context
import io.ktor.utils.io.core.*
import java.io.*
import java.lang.IllegalStateException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.zip.ZipError
import java.util.zip.ZipException
import java.util.zip.ZipFile

class mpNetwork {
    companion object {

        fun getNodeStatus(address:String, port:Int, viewModel: MainViewModel):NodeInfo{
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

                viewModel.SYNC_DELAY = DEFAULT_SYNC_DELAY // Restore Sync Delay
                viewModel.ConnectionError.postValue(false) // Reports Connection Success

                return mpFunctions.stringToNodeInfo(response, address, port)
            }catch (t:SocketTimeoutException){
                Log.e("mpNetwork","Request to $address -> Timed Out")
            }catch (c:ConnectException){ // No internet ?
                viewModel.ConnectionError.postValue(true) // Report Connection Error
                viewModel.SYNC_DELAY = viewModel.SYNC_DELAY+1000   // Inrcease Wait for the next attempt
                Log.e("mpNetwork","Connection error, check the internet")
            }catch (e:Exception){ // Something else....
                viewModel.ConnectionError.postValue(true)
                Log.e("mpNetwork","Unhandled Exception: "+e.printStackTrace().toString())
            }
            return NodeInfo()
        }

        fun getSummary(context: Context, address: String, port: Int, viewModel: MainViewModel):Boolean{

            val serverAddress = InetSocketAddress(address, port)
            Log.e("mpNetwork","Requesting summary to $address")

            val zSumaryFile = File(
                context.getExternalFilesDir(null)!!.path
                        + File.separator
                        + NOSPath
                        + File.separator
                        + ZipSumaryFileName
            )

            try {
                val clientSocket = Socket()
                clientSocket.connect(serverAddress, NODE_TIMEOUT)
                val clientChannel = PrintWriter(
                    BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())),
                    true
                )
                val inputStream = clientSocket.getInputStream()

                mpDisk.CreateSummFile()

                clientChannel.println("GETZIPSUMARY")

                val fos = FileOutputStream(zSumaryFile, false)
                var bytes = ByteArray(8192)

                var read: Int = inputStream.read(bytes)

                // Find zipheader to start zip file recreation
                var breakpoint = 0
                for(index in 0..bytes.size-1){
                    if(
                        bytes[index].toInt() == 0x50 &&
                        bytes[index+1].toInt() == 0x4b &&
                        bytes[index+2].toInt() == 0x03 &&
                        bytes[index+3].toInt() == 0x04
                    ){
                        breakpoint = index
                        break
                    }
                }

                // Copy of range of bytes read starting from zip header
                bytes = bytes.copyOfRange(breakpoint, bytes.size+breakpoint)
                read = read-(breakpoint) // resize number of bytes read

                while (read != -1) {
                    fos.write(bytes, 0,read)
                    read = inputStream.read(bytes)
                }

                fos.flush() // Flush File output stream
                fos.close() // Close file output stream
                clientSocket.close()

            }catch (t:SocketTimeoutException){
                Log.e("Welcome-Err","Request failed to $address$ -> Timed Out")
            }catch (c:ConnectException) { // No internet ?
                viewModel.ConnectionError.postValue(true) // Report Connection Error
                Log.e("mpNetwork", "Connection error, check the internet")
            }catch (e:Exception){ // Something else....
                viewModel.ConnectionError.postValue(true)
                Log.e("mpNetwork","Unhandled Exception: "+e.message)
            }

            try{
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
                                Log.e("Welcome", "Summary file decompressed - OK")
                                return true
                            }
                        }
                    }
                }
            }catch (z:ZipException) {
                Log.e("mpNetwork", "Unzipping error: " + z.message)
            }catch (i:IllegalStateException){
                Log.e("mpNetwork", "IllegalState error: " + i.message)
            }catch (e:Exception){ // Something else....
                Log.e("mpNetwork","Unhandled ZipException: "+e.message)
            }
            return false
        }

        fun sendOrder(orderString:String, viewModel: MainViewModel):String {
            val serverAddress = InetSocketAddress(viewModel.LastNodeSelected?.Address?:"192.210.226.118", viewModel.LastNodeSelected?.Port?:8080)

            Log.e("mpNetwork","Sending Order to ${viewModel.LastNodeSelected?.Address?:"192.210.226.118"} Port: ${viewModel.LastNodeSelected?.Port}")
            Log.e("mpNetwork",orderString)
            try{
                val clientSocket = Socket()
                clientSocket.connect(serverAddress, NODE_TIMEOUT)

                val clientChannel = PrintWriter(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())), true)
                val inputStreamReader = InputStreamReader(clientSocket.getInputStream())
                val bufferReader = BufferedReader(inputStreamReader)

                clientChannel.println(orderString)

                bufferReader.use { response ->
                    val line = response.readLine()
                    response.close()
                    Log.e("mpNetwork","Response after order is: $line")
                    return line
                }
            }catch (t: SocketTimeoutException){
                Log.e("mpNetwork","Order sent failed to ${viewModel.LastNodeSelected?.Address?:"192.210.226.118"} -> Timed Out")
            }catch (c:ConnectException){ // No internet ?
                viewModel.ConnectionError.postValue(true) // Report Connection Error
                Log.e("mpNetwork","Connection error, check the internet")
            }catch (e:Exception){ // Something else....
                viewModel.ConnectionError.postValue(true)
                Log.e("mpNetwork","Unhandled Exception: "+e.printStackTrace().toString())
            }
            return ""
        }

        fun getPendings(address: String, port: Int, viewModel: MainViewModel):String {
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
            }catch (c:ConnectException){ // No internet ?
                viewModel.ConnectionError.postValue(true) // Report Connection Error
                Log.e("mpNetwork","Connection error, check the internet")
            }catch (e:Exception){ // Something else....
                viewModel.ConnectionError.postValue(true)
                Log.e("mpNetwork","Unhandled Exception: "+e.printStackTrace().toString())
            }
            return "ERROR"
        }
    }
}