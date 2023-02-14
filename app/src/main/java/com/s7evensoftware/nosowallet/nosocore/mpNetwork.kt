package com.s7evensoftware.nosowallet.nosocore

import android.content.Context
import androidx.compose.runtime.MutableState
import com.s7evensoftware.nosowallet.*
import com.s7evensoftware.nosowallet.model.DEFAULT_SYNC_DELAY
import com.s7evensoftware.nosowallet.model.NODE_TIMEOUT
import com.s7evensoftware.nosowallet.model.NOSPath
import com.s7evensoftware.nosowallet.model.ZipSumaryFileName
import com.s7evensoftware.nosowallet.ui.footer.SyncState
import com.s7evensoftware.nosowallet.util.Log
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.lang.IllegalStateException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.zip.ZipException
import java.util.zip.ZipFile

class mpNetwork {
    companion object {
        fun getMasterNodeList(
            targetAddress:String,
            targetPort:Int,
            mnList:MutableList<ServerObject>,
            syncDelay:MutableState<Long>,
            syncStatus:MutableState<SyncState>
        ):List<ServerObject>{
            val serverAddress = InetSocketAddress(targetAddress, targetPort)
            Log.e("mpNetwork", "Requesting MN List to $targetAddress")
            try{
                val clientSocket = Socket()
                clientSocket.connect(serverAddress, NODE_TIMEOUT)
                val clientChannel = PrintWriter(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())), true)
                val inputStreamReader = InputStreamReader(clientSocket.getInputStream())
                val bufferReader = BufferedReader(inputStreamReader)

                clientChannel.println("NSLMNS")
                val response = bufferReader.readLine()
                clientSocket.close()

                val fullMNList = mpFunctions.parseMNString(response)
                val validatorsAmount:Int = (fullMNList.size/10) + 3
                val fullMNListOrdered = fullMNList.sortedByDescending { server -> server.Count }
                val validators = fullMNListOrdered.subList(0, if(validatorsAmount >= fullMNListOrdered.size){ fullMNListOrdered.size }else{ validatorsAmount})
                mnList.clear()
                mnList.addAll(validators)

                syncDelay.value = DEFAULT_SYNC_DELAY // Restore Sync Delay
                //syncStatus.value = SyncState.Synced // Reports Connection Success
                return validators
            }catch (t:SocketTimeoutException){
                Log.e("mpNetwork", "MN List Request to $targetAddress -> Timed Out")
            }catch (c:ConnectException){ // No internet ?
                syncStatus.value = SyncState.Retrying // Report Connection Error
                syncDelay.value +=1000   // Inrcease Wait for the next attempt
                Log.e("mpNetwork", "Connection error, MN down or check your the internet")
            }catch (e:java.lang.Exception){ // Something else....
                syncStatus.value = SyncState.FatalError
                Log.e("mpNetwork", "Unhandled Exception: " + e.message)
            }
            return listOf()
        }

        fun getNodeStatus(
            targetAddress:String,
            targetPort:Int,
            syncDelay: MutableState<Long>,
            syncStatus:MutableState<SyncState>
        ): NodeInfo {
            val serverAddress = InetSocketAddress(targetAddress, targetPort)
            //Log.e("mpNetwork","Requesting Node Status to $address")
            try{
                val clientSocket = Socket()
                clientSocket.connect(serverAddress, NODE_TIMEOUT)
                val clientChannel = PrintWriter(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())), true)
                val inputStreamReader = InputStreamReader(clientSocket.getInputStream())
                val bufferReader = BufferedReader(inputStreamReader)

                clientChannel.println("NODESTATUS")
                val response = bufferReader.readLine()
                clientSocket.close()

                syncDelay.value = DEFAULT_SYNC_DELAY // Restore Sync Delay
                //syncStatus.value = SyncState.Synced // Reports Connection Success
                return mpFunctions.stringToNodeInfo(response, targetAddress, targetPort)
            }catch (t:SocketTimeoutException){
                Log.e("mpNetwork", "Node Status Request to $targetAddress -> Timed Out")
            }catch (c:ConnectException){ // No internet ?
                syncStatus.value = SyncState.Retrying // Report Connection Error
                syncDelay.value += 1000   // Inrcease Wait for the next attempt
                Log.e(
                    "mpNetwork",
                    "Connection to $targetAddress:$targetPort -> error, Node is down or check the internet"
                )
            }catch (e:java.lang.Exception){ // Something else....
                syncStatus.value = SyncState.FatalError
                Log.e("mpNetwork", "Unhandled Exception: " + e.message)
            }
            return NodeInfo()
        }

        suspend fun getSummary(
            context: Context,
            address: String,
            port: Int,
            syncStatus: MutableState<SyncState>
        ):Boolean{
            val serverAddress = InetSocketAddress(address, port)
            Log.e("mpNetwork", "Requesting summary to $address")

            val zSumaryFile = File(
                context.getExternalFilesDir(null)!!.path
                        + File.separator
                        + NOSPath
                        + File.separator
                        + ZipSumaryFileName
            )

            try {
                withContext(Dispatchers.IO) {
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
                    var bytes = ByteArray(8213)

                    var read: Int = inputStream.read(bytes)
                    var breakpoint = 0

                    while (read != -1) {
                        if (breakpoint == 0) { // Find zipheader to start zip file recreation
                            for (index in 0..bytes.size - 1) {
                                if (
                                    bytes[index].toInt() == 0x50 &&
                                    bytes[index + 1].toInt() == 0x4b &&
                                    bytes[index + 2].toInt() == 0x03 &&
                                    bytes[index + 3].toInt() == 0x04
                                ) {
                                    breakpoint = index
                                    // Copy of range of bytes read starting from zip header
                                    bytes = bytes.copyOfRange(breakpoint, bytes.size)
                                    read -= breakpoint // resize number of bytes read
                                    fos.write(bytes, 0, read)
                                    break
                                }
                            }
                        } else {
                            fos.write(bytes, 0, read)
                        }
                        read = inputStream.read(bytes)
                    }

                    fos.flush() // Flush File output stream
                    fos.close() // Close file output stream
                    clientSocket.close()
                }

            }catch (t:SocketTimeoutException){
                Log.e("Welcome-Err", "Request failed to $address$ -> Timed Out")
            }catch (c:ConnectException) { // No internet ?
                syncStatus.value = SyncState.FatalError
                Log.e("mpNetwork", "Connection error, Node is down or check the internet")
            }catch (e:Exception){ // Something else....
                syncStatus.value = SyncState.FatalError
                Log.e("mpNetwork", "Unhandled Exception: " + e.message)
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

                            uzFile.parentFile?.mkdirs()

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
                Log.e("mpNetwork", "Unhandled ZipException: " + e.message)
            }
            return false
        }

        suspend fun sendOrder(
            orderString:String,
            lastNodeSelected: NodeInfo?,
            syncStatus:MutableState<SyncState>
        ):String {
            val serverAddress = InetSocketAddress(lastNodeSelected?.Address?:"192.210.226.118", lastNodeSelected?.Port?:8080)
            Log.e(
                "mpNetwork",
                "Sending Order to ${lastNodeSelected?.Address ?: "192.210.226.118"} Port: ${lastNodeSelected?.Port}"
            )
            Log.e("mpNetwork", orderString)
            try{
                var responseLine = ""
                withContext(Dispatchers.IO){
                    val clientSocket = Socket()
                    clientSocket.connect(serverAddress, NODE_TIMEOUT)

                    val clientChannel = PrintWriter(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())), true)
                    val inputStreamReader = InputStreamReader(clientSocket.getInputStream())
                    val bufferReader = BufferedReader(inputStreamReader)

                    clientChannel.println(orderString)

                    bufferReader.use { response ->
                        responseLine = response.readLine()
                        response.close()
                        Log.e("mpNetwork", "NodeResponse: $responseLine")
                    }
                }
                return responseLine
            }catch (t: SocketTimeoutException){
                Log.e(
                    "mpNetwork",
                    "Order sent failed to ${lastNodeSelected?.Address ?: "192.210.226.118"} -> Timed Out"
                )
            }catch (c:ConnectException){ // No internet ?
                syncStatus.value = SyncState.FatalError // Report Connection Error
                Log.e("mpNetwork", "Connection error, check the internet")
            }catch (e:Exception){ // Something else....
                syncStatus.value = SyncState.FatalError
                Log.e("mpNetwork", "Unhandled Exception: " + e.printStackTrace().toString())
            }
            return ""
        }

        suspend fun getPendings(
            address: String,
            port: Int,
            syncStatus:MutableState<SyncState>
        ):String {
            val serverAddress = InetSocketAddress(address, port)
            Log.e("mpNetwork", "Requesting Pendings to $address")
            try{
                val clientSocket = Socket()
                var stringResponse = ""

                withContext(Dispatchers.IO){
                    clientSocket.connect(serverAddress, NODE_TIMEOUT)
                    val clientChannel = PrintWriter(BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream())), true)
                    val inputStreamReader = InputStreamReader(clientSocket.getInputStream())
                    val bufferReader = BufferedReader(inputStreamReader)

                    clientChannel.println("NSLPEND")

                    bufferReader.use { response ->
                        stringResponse = response.readLine()
                        response.close()
                    }
                }
                return stringResponse
            }catch (t: SocketTimeoutException){
                Log.e("mpNetwork", "Request to $address -> Timed Out")
            }catch (c:ConnectException){ // No internet ?
                syncStatus.value = SyncState.FatalError// Report Connection Error
                Log.e("mpNetwork", "Connection error, check the internet")
            }catch (e:Exception){ // Something else....
                syncStatus.value = SyncState.FatalError
                Log.e("mpNetwork", "Unhandled Exception: " + e.printStackTrace().toString())
            }
            return "ERROR"
        }
    }
}