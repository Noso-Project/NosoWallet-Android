package com.s7evensoftware.nosowallet

import android.util.Log
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList

class mpFunctions {
    companion object {

        fun UpdateWalletFromSummary(addressList: ArrayList<WalletObject>, addressSummary: ArrayList<SumaryData>){
            for(wallet in addressList){
                wallet.Balance = GetAddressBalanceFromSummary(wallet.Hash?:"nohash", addressSummary)
            }
        }

        fun GetAddressBalanceFromSummary(address:String, addressSummary: ArrayList<SumaryData>):Long {
            for(summary in addressSummary){
                if(summary.Hash.equals(address)){
                    return summary.Balance
                }
            }
            return 0L
        }

        fun Concensus(NODEarray:ArrayList<NodeInfo>, viewModel:MainViewModel):NodeInfo? {
            var result:NodeInfo? = null
            var selectedNode:NodeInfo? = null
            var ArrT:ArrayList<ConcensusData>
            var CBlock = 0L
            var CBranch = ""
            var CPending = 0L

            // Get the consensus block number
            ArrT = ArrayList()
            for(node in NODEarray){
                addValue(node.Lastblock.toString(), ArrT)
                CBlock = getHighest(ArrT).toLong()
            }

            // Get the consensus summary
            ArrT = ArrayList()
            for(node in NODEarray){
                addValue(node.Branch, ArrT)
                CBranch = getHighest(ArrT)
            }

            // Get the consensus pendings
            ArrT = ArrayList()
            for(node in NODEarray){
                addValue(node.Pendings.toString(), ArrT)
                CPending = getHighest(ArrT).toLong()
            }

            if(
                !viewModel.WalletSynced &&
                CBlock == viewModel.LastBlock.value &&
                CBranch.equals(viewModel.LastSummary.value)
            ){
                viewModel.WalletSynced = true
            }

            if((CBlock > viewModel.LastBlock.value?:0) || CPending > viewModel.LastPendingCount.value?:0){
                selectedNode = getRandomServer(NODEarray, CBlock, CBranch, CPending)
                Log.e("mpFunctions","Node Selected -> ${selectedNode.Address}:${selectedNode.Port}")
            }

            if(CBlock > viewModel.LastBlock.value?:0){
                result = selectedNode
                viewModel.LastBlock.postValue(CBlock)
                viewModel.LastSummary.postValue(CBranch)
                viewModel.LastPendingCount.postValue(0)
            }

            if(CPending > viewModel.LastPendingCount.value?:0){
                val Pending_String = mpNetwork.getPendings(selectedNode!!.Address,selectedNode.Port)
                ProcessPendings(
                    Pending_String,
                    viewModel.AdddressList.value!!,
                    viewModel.AddressSummary.value!!,
                    viewModel.PendingList.value!!
                )
            }

            return result
        }

        private fun getRandomServer(NODEarray: ArrayList<NodeInfo>, block:Long, brach:String, pendings:Long): NodeInfo {
            Log.e("mpFunctions","Selecting random server")
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
            val randomIndex = ThreadLocalRandom.current().nextInt(candidateServer.size)
            return candidateServer[randomIndex]
        }

        fun ProcessPendings(
            input:String,
            addressList: ArrayList<WalletObject>,
            addressSummary: ArrayList<SumaryData>,
            pendingList: ArrayList<PendingData>
        ){
            var ThisOrder:String
            var Add_index:Int

            val tokens = StringTokenizer(input)

            while(tokens.hasMoreTokens()){
                ThisOrder = tokens.nextToken()
                if(ThisOrder != ""){
                    val pendingInfo = stringToOrderData(ThisOrder)
                    if(pendingInfo.TO_Type.equals("TRFR")){
                        Add_index = WalletAddressIndex(pendingInfo.TO_Sender, addressList, addressSummary)
                        if(Add_index >= 0){
                            pendingList[Add_index].Outgoing = pendingList[Add_index].Outgoing+pendingInfo.TO_Amount+pendingInfo.TO_Fee
                        }
                        Add_index = WalletAddressIndex(pendingInfo.TO_Receiver, addressList, addressSummary)
                        if(Add_index >= 0){
                            pendingList[Add_index].Incoming = pendingList[Add_index].Incoming+pendingInfo.TO_Amount
                        }
                    }
                }
            }
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

        fun WalletAddressIndex(address:String, addressList:ArrayList<WalletObject>, addressSummary: ArrayList<SumaryData>):Int{
            if(!mpParser.IsValidAddress(address) && AddressSummaryIndex(address, addressSummary) < 0 ){
                return -1
            }

            for((index,wallet) in addressList.withIndex()){
                if(wallet.Hash.equals(address) || wallet.Custom.equals(address)){
                    return index
                }
            }
            return -1
        }

        fun stringToOrderData(input:String):PendingInfo {
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

        fun stringToNodeInfo(input: String, address: String, port: Int):NodeInfo{
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
    }
}