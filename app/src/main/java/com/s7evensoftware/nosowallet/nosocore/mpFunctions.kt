package com.s7evensoftware.nosowallet.nosocore

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.s7evensoftware.nosowallet.model.*
import com.s7evensoftware.nosowallet.ui.footer.SyncState
import com.s7evensoftware.nosowallet.util.Log
import io.realm.kotlin.Realm
import io.realm.kotlin.query.RealmResults
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class mpFunctions {
    companion object {

        fun UpdateWalletFromSummary(addressList: ArrayList<WalletObject>, realmDB: Realm){
            for(wallet in addressList){
                wallet.Hash?.let {
                    wallet.Custom = GetCustomFromSummary(it, realmDB)
                    wallet.Balance = GetAddressBalanceFromSummary(it, realmDB)
                }
            }
        }

        fun GetCustomFromSummary(address:String, realm: Realm):String {
            return DBManager.getCustom(address, realm)
        }

        fun GetAddressBalanceFromSummary(address:String, realm: Realm):Long {
            return DBManager.getAddressBalance(address, realm)
        }

        fun updateNodeList(){

        }

        suspend fun Concensus(
            nodeArray:ArrayList<NodeInfo>,
            addressList: SnapshotStateList<WalletObject>,
            lastBlock:MutableState<Long>,
            lastSummary:MutableState<String>,
            lastPendingCount:MutableState<Long>,
            lastNodeSelected:MutableState<NodeInfo?>,
            currentTime:MutableState<Long>,
            syncDelay:MutableState<Long>,
            syncStatus:MutableState<SyncState>,
            realmDB: Realm
        ): ConcensusResult? {
            var result: ConcensusResult? = null
            var selectedNode: NodeInfo?
            var ArrT:ArrayList<ConcensusData>
            var CTime = 0L
            var CBlock = 0L
            var CBranch = ""
            var CPending = 0L

            // Get the consensus unix time
            ArrT = ArrayList()
            for(node in nodeArray){
                addValue(node.UTCTime.toString(), ArrT)
                CTime = getHighest(ArrT).toLong()
            }

            // Get the consensus block number
            ArrT = ArrayList()
            for(node in nodeArray){
                addValue(node.Lastblock.toString(), ArrT)
                CBlock = getHighest(ArrT).toLong()
            }

            // Get the consensus summary
            ArrT = ArrayList()
            for(node in nodeArray){
                addValue(node.Branch, ArrT)
                CBranch = getHighest(ArrT)
            }

            // Get the consensus pendings
            ArrT = ArrayList()
            for(node in nodeArray){
                addValue(node.Pendings.toString(), ArrT)
                CPending = getHighest(ArrT).toLong()
            }

            if(
                syncStatus.value == SyncState.Synced &&
                CBlock == lastBlock.value &&
                CBranch.equals(lastSummary.value)
            ){
                syncStatus.value = SyncState.Synced // Reports Connection Success
            }

            //Select a random server for the upcoming requests
            selectedNode = getRandomNode(nodeArray, CBlock, CBranch, CPending)
            lastNodeSelected.value = selectedNode

            if(CBlock > lastBlock.value || DBManager.getSummarySize(realmDB) == 0){
                result = ConcensusResult()
                result.LastBlock = CBlock
                result.LastBranch = CBranch
                result.Address = selectedNode.Address
                result.Port = selectedNode.Port
                lastPendingCount.value = 0
            }

            if(CPending > lastPendingCount.value || result != null){
                val pending_String = mpNetwork.getPendings(
                    address = selectedNode.Address,
                    port = selectedNode.Port,
                    syncStatus = syncStatus
                )

                if(pending_String != "ERROR"){
                    ProcessPendings(
                        pending_String,
                        CPending,
                        addressList,
                        lastPendingCount,
                        realmDB
                    )
                }else{
                    mpDisk.appendLog("mpFunctions", "Request pendings failed")
                }
            }

//            if(currentTime.value/1000 > (CTime-1) && currentTime.value/1000 < (CTime+1)){
//                Log.e("mpFuncion", "Clock sync not needed")
//            }else{
//                currentTime.value = CTime*1000
//            }

            return result
        }

        private fun getRandomNode(NODEarray: ArrayList<NodeInfo>, block:Long, brach:String, pendings:Long): NodeInfo {
            val candidateServer = ArrayList<NodeInfo>()
            for(server in NODEarray){
                if(
                    server.Branch == brach &&
                    server.Lastblock == block &&
                    server.Pendings == pendings &&
                    (DELTA_TRIGGER || server.Delta == 0L)
                ){
                    candidateServer.add(server)
                }
            }

            if(candidateServer.size > 0){
                ThreadLocalRandom.current().nextInt(candidateServer.size).let {
                    return candidateServer[it]
                }
            }
            return NodeInfo()
        }

        fun getRandomServer(ServerList: RealmResults<ServerObject>?): ServerObject {
            ServerList?.let {
                if(ServerList.size > 0){
                    ThreadLocalRandom.current().nextInt(ServerList.size).let {
                        return ServerList[it]
                    }
                }
            }

            val seedList = DBManager.seedsList.map { nodeAddress ->
                ServerObject().apply {
                    Address = nodeAddress
                }
            }

            ThreadLocalRandom.current().nextInt(seedList.size).let {
                return seedList[it]
            }
        }

        fun getRandomServer(ServerList: ArrayList<NodeInfo>): NodeInfo {
            if(ServerList.size > 0){
                ThreadLocalRandom.current().nextInt(ServerList.size).let {
                    return ServerList[it]
                }
            }

            val seedList = DBManager.seedsList.map { nodeAddress ->
                NodeInfo().apply {
                    Address = nodeAddress
                }
            }

            ThreadLocalRandom.current().nextInt(seedList.size).let {
                return seedList[it]
            }
        }

        fun SendFundsFromAddress(
            origin:String,
            destination:String,
            amount:Long,
            fee:Long,
            reference:String,
            orderTime:Long,
            line:Int,
            lastBlock:Long,
            wallet: WalletObject
        ): OrderData {
            var AvailableAmount:Long; var AmountTrfr:Long;var FeeTrfr:Long
            var OrderInfo = OrderData()

            AvailableAmount = wallet.Balance-wallet.Outgoing
            if(AvailableAmount > fee){
                FeeTrfr = fee
            }else{
                FeeTrfr = AvailableAmount
            }

            if(AvailableAmount > (amount+fee)){
                AmountTrfr = amount
            }else{
                AmountTrfr = AvailableAmount-fee
            }

            if(AmountTrfr < 0){
                AmountTrfr = 0
            }

            OrderInfo.OrderID = ""
            OrderInfo.OrderLines = 1
            OrderInfo.OrderType = "TRFR"
            OrderInfo.TimeStamp = orderTime
            OrderInfo.Reference = reference
            OrderInfo.TrxLine = line
            OrderInfo.Sender = wallet.PublicKey
            OrderInfo.Address = wallet.Hash
            OrderInfo.Receiver = destination
            OrderInfo.AmountFee = FeeTrfr
            OrderInfo.AmountTrf = AmountTrfr
            OrderInfo.Signature = mpCripto.getStringSigned(
                orderTime.toString() +
                        origin +
                        destination +
                        AmountTrfr.toString() +
                        FeeTrfr.toString() +
                        line.toString(),
                wallet.PrivateKey!!
            )
            OrderInfo.TrfrID = getTransferHash(
                orderTime.toString()+
                        origin+
                        destination+
                        amount.toString()+
                        lastBlock.toString()
            )
            return OrderInfo
        }

        fun getStringFromOrder(order: OrderData):String {
            return order.OrderType+" "+
                    order.OrderID+" "+
                    order.OrderLines.toString()+" "+
                    order.OrderType+" "+
                    order.TimeStamp.toString()+" "+
                    order.Reference+" "+
                    order.TrxLine.toString()+" "+
                    order.Sender+" "+
                    order.Address+" "+
                    order.Receiver+" "+
                    order.AmountFee.toString()+" "+
                    order.AmountTrf.toString()+" "+
                    order.Signature+" "+
                    order.TrfrID
        }

        fun getPTCEcn(ordertype:String):String {
            return "NSL"+ordertype+" "+ Protocol.toString()+" "+ ProgramVersion +" "+(System.currentTimeMillis()/1000)+" "
        }

        fun getOrderHash(textLine:String):String {
            var Result = mpCripto.HashSha256String(textLine)
            return "OR"+ mpCripto.BMHexto58(Result, BigInteger("36"))
        }

        fun getTransferHash(textLine:String):String {
            var Resultado:String
            var Sumatoria:String;var Clave:String

            Resultado = mpCripto.HashSha256String(textLine)
            Resultado = mpCripto.BMHexto58(Resultado, BigInteger("58"))
            Sumatoria = mpCripto.BMB58resumen(Resultado).toString()
            Clave = mpCripto.BMDecto58(Sumatoria)
            return "tR$Resultado$Clave"
        }

        fun getAddressPendingPays(
            address:String,
            addressList: List<WalletObject>,
            realmDB: Realm
        ):Long {
            val walletIndex = walletAddressIndex(address, addressList, realmDB)
            if(walletIndex != -1){
                return addressList[walletIndex].Outgoing
            }
            return 0L
        }

        fun ProcessPendings(
            input: String,
            CPending:Long,
            addressList: SnapshotStateList<WalletObject>,
            lastPendingCount: MutableState<Long>,
            realmDB: Realm
        ){
            var ThisOrder:String
            var Add_index:Int
            val tokens = StringTokenizer(input)

            val updatedWallet = addressList.toMutableList()

            //Clear pendings before recalculate
            for(wallet in updatedWallet){
                wallet.Outgoing = 0L
                wallet.Incoming = 0L
            }

            while(tokens.hasMoreTokens()){
                ThisOrder = tokens.nextToken()
                if(ThisOrder != ""){
                    val pendingInfo = stringToOrderData(ThisOrder)
                    if(pendingInfo.TO_Type.equals("TRFR")){
                        Add_index = walletAddressIndex(pendingInfo.TO_Sender, updatedWallet, realmDB)
                        if(Add_index >= 0){
                            updatedWallet[Add_index].Outgoing = updatedWallet[Add_index].Outgoing+pendingInfo.TO_Amount+pendingInfo.TO_Fee
                        }
                        Add_index = walletAddressIndex(pendingInfo.TO_Receiver, updatedWallet, realmDB)
                        if(Add_index >= 0){
                            updatedWallet[Add_index].Incoming = updatedWallet[Add_index].Incoming+pendingInfo.TO_Amount
                        }
                    }
                }
            }
            addressList.clear()
            addressList.addAll(updatedWallet)
            lastPendingCount.value = CPending // Update Pendings Count
        }

        fun getPoolListFromCFG(
            input: String
        ): List<HashMap<Int, String>> {
            if(input.isEmpty()) return listOf()

            val poolList = mutableListOf<HashMap<Int, String>>()
            val blockTokens = StringTokenizer(input)
            val values = ArrayList<String>()

            while(blockTokens.hasMoreTokens()){
                values.add(blockTokens.nextToken())
            }

            val poolTokens = StringTokenizer(values[4], ":")
            while (poolTokens.hasMoreTokens()){
                val poolData = StringTokenizer(poolTokens.nextToken(), ";")
                if(poolData.hasMoreTokens()){
                    val pool = HashMap<Int, String>()
                    pool[0] = poolData.nextToken() // Pool Address
                    pool[1] = poolData.nextToken() // Pool Port
                    poolList.add(pool)
                }
            }

            return poolList
        }

        fun AddressSummaryIndex(address: String, addressSummary: ArrayList<SumaryData>):Int {
            if(address != "" && addressSummary.size > 0){
                for((index, wallet) in addressSummary.withIndex()){
                    if(wallet.Hash.equals(address) || wallet.Custom.equals(address)){
                        return index
                    }
                }
            }
            return -1
        }

        fun walletAddressIndex(address:String, addressList:List<WalletObject>, realmDB: Realm):Int{
            if(!mpParser.IsValidAddress(address) && DBManager.getSummarySize(realmDB) < 0 ){
                return -1
            }

            for((index,wallet) in addressList.withIndex()){
                if(wallet.Hash.equals(address) || wallet.Custom.equals(address)){
                    return index
                }
            }
            return -1
        }

        fun InsertAddress(wallet: WalletObject, addressList: List<WalletObject>): WalletObject? {
            if(!mpParser.WalletExists(wallet, addressList)){
                val newList = addressList.toMutableList()
                newList.add(wallet)
                mpDisk.SaveWallet(newList)
                Log.e("mpParser", "## Wallet Info: ")
                Log.e("mpParser", "# Address: ${wallet.Hash}")
                return wallet
            }else{
                Log.e("mpParser", "Ignoring wallet, already exists: ${wallet.Hash}")
                return null
            }
        }

        fun getMaximumToSend(amount:Long):Long {
            var Available:Long
            var Maximum:Long
            var Fee:Long
            var SenT:Long
            var Diff:Long

            Available = amount
            Maximum = (Available * Comisiontrfr) / (Comisiontrfr + 1)
            Fee = Maximum / Comisiontrfr
            SenT = Maximum + Fee
            Diff = Available-SenT
            return Maximum + Diff
        }

        fun stringToOrderData(input:String): PendingInfo {
            val tokens = StringTokenizer(input.replace(",", " "))
            val values = ArrayList<String>()
            while(tokens.hasMoreTokens()){
                values.add(tokens.nextToken())
            }
            val pendingInfo = PendingInfo()
            pendingInfo.TO_Type = values[0]
            pendingInfo.TO_Sender = values[1]
            pendingInfo.TO_Receiver = values[2]
            pendingInfo.TO_Amount = values[3].toLong()
            pendingInfo.TO_Fee = values[4].toLong()
            return pendingInfo
        }

        fun stringToNodeInfo(input: String, address: String, port: Int): NodeInfo {
            val tokens = StringTokenizer(input)
            val values = ArrayList<String>()
            while(tokens.hasMoreTokens()){
                values.add(tokens.nextToken())
            }

            val nodeInfo = NodeInfo()
            nodeInfo.Address = address
            nodeInfo.Port = port
            nodeInfo.Connections = values[1].toLong()
            nodeInfo.Lastblock = values[2].toLong()
            nodeInfo.Pendings = values[3].toLong()
            nodeInfo.Delta = values[4].toLong()
            nodeInfo.Branch = values[5]
            nodeInfo.Version = values[6]
            nodeInfo.UTCTime = values[7].toLong()
            return nodeInfo
        }

        fun getHighest(ArrT:ArrayList<ConcensusData>):String {
            var Maximum = 0
            var MaxIndex = 0

            for(cd in ArrT){
                if(cd.Count > Maximum){
                    Maximum = cd.Count
                    MaxIndex = ArrT.indexOf(cd)
                }
            }
            return ArrT[MaxIndex].Value
        }

        fun addValue(Tvalue:String, ArrT: ArrayList<ConcensusData>){
            var Added = false
            var ThisItem = ConcensusData()

            for(cd in ArrT){
                if(Tvalue.equals(cd.Value)){
                    cd.Count++
                    Added = true
                }
            }

            if(!Added){
                ThisItem.Value = Tvalue
                ThisItem.Count = 1
                ArrT.add(ThisItem)
            }
        }

        fun getDateFromUNIX(time:Long):String{
            try {
                val formatter = SimpleDateFormat("dd/MM/yyyy")
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                return formatter.format(time)
            }catch (e:Exception){
                Log.e("mpFunctions", "Error parsing date")
            }
            return "00/00/0000"
        }

        fun getTimeFromUNIX(time:Long):String{
            try {
                val formatter = SimpleDateFormat("HH:mm:ss")
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                return formatter.format(time)
            }catch (e:Exception){
                Log.e("mpFunctions", "Error parsing date")
            }
            return "00:00:00"
        }

        fun parseMNString(response: String?): List<ServerObject> {
            val resultMNList = mutableListOf<ServerObject>()
            response?.let {
                val tokens = StringTokenizer(it);tokens.nextToken() // Ignore Block Number

                while(tokens.hasMoreTokens()){
                    var rawNodeInfo = tokens.nextToken()
                        rawNodeInfo = rawNodeInfo.replace(":", " ")
                        rawNodeInfo = rawNodeInfo.replace(";", " ")

                    val tokenNodeInfo = StringTokenizer(rawNodeInfo)
                    val nodeValues = ArrayList<String>()
                    while(tokenNodeInfo.hasMoreTokens()){
                        nodeValues.add(tokenNodeInfo.nextToken())
                    }

                    val nodeInfo = ServerObject()
                    nodeInfo.Address = nodeValues[0]
                    nodeInfo.Port = nodeValues[1].toInt()
                    nodeInfo.NosoAddress = nodeValues[2]
                    nodeInfo.Count = nodeValues[3].toInt()

                    if(resultMNList.isEmpty()){
                        resultMNList.add(nodeInfo)
                    }else{
                        resultMNList.add(nodeInfo)
                    }
                }
            }
            return resultMNList
        }
    }
}