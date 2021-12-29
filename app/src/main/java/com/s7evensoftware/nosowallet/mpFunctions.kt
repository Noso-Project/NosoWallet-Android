package com.s7evensoftware.nosowallet

import android.util.Log
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList

class mpFunctions {
    companion object {

        fun UpdateWalletFromSummary(addressList: ArrayList<WalletObject>){
            for(wallet in addressList){
                wallet.Balance = GetAddressBalanceFromSummary(wallet.Hash?:"nohash")
            }
        }

        fun GetAddressBalanceFromSummary(address:String):Long {
            return DBManager.getAddressBalance(address)
        }

        fun Concensus(NODEarray:ArrayList<NodeInfo>, viewModel:MainViewModel):NodeInfo? {
            var result:NodeInfo? = null
            var selectedNode: NodeInfo?
            var ArrT:ArrayList<ConcensusData>
            var CTime = 0L
            var CBlock = 0L
            var CBranch = ""
            var CPending = 0L

            // Get the consensus unix time
            ArrT = ArrayList()
            for(node in NODEarray){
                addValue(node.UTCTime.toString(), ArrT)
                CTime = getHighest(ArrT).toLong()
            }

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
                !viewModel.WalletSynced.value!! &&
                CBlock == viewModel.LastBlock.value &&
                CBranch.equals(viewModel.LastSummary.value)
            ){
                viewModel.WalletSynced.postValue(true)
            }

            //Select a random server for the upcoming requests
            selectedNode = getRandomServer(NODEarray, CBlock, CBranch, CPending)
            viewModel.LastNodeSelected = selectedNode

            if(CBlock > viewModel.LastBlock.value?:0){
                result = selectedNode
                viewModel.LastBlock.postValue(CBlock)
                viewModel.LastSummary.postValue(CBranch)
                viewModel.LastPendingCount.postValue(0)
            }

            if(CPending > viewModel.LastPendingCount.value?:0){
                val Pending_String = mpNetwork.getPendings(selectedNode!!.Address,selectedNode.Port, viewModel)
                ProcessPendings(
                    Pending_String,
                    viewModel.AdddressList.value!!,
                    viewModel.PendingList.value!!
                )
                viewModel.LastPendingCount.postValue(CPending)
            }

            if(viewModel.RealTimeValue.value!! > (CTime*1000-500) || viewModel.RealTimeValue.value!! < (CTime*1000+500)){
                Log.e("mpFuncion","Clocked sync not needed")
            }else{
                viewModel.RealTimeValue.postValue(CTime*1000)
                Log.e("mpFunction","Clocked Synchronized")
            }

            if((CBlock > viewModel.LastBlock.value?:0) || CPending > viewModel.LastPendingCount.value?:0){
                viewModel.UpdateBalanceTrigger.postValue(viewModel.UpdateBalanceTrigger.value!!+1)
            }
            return result
        }

        private fun getRandomServer(NODEarray: ArrayList<NodeInfo>, block:Long, brach:String, pendings:Long): NodeInfo {
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

        fun SendFundsFromAddress(
            origin:String,
            destination:String,
            amount:Long,
            fee:Long,
            reference:String,
            ordertime:Long,
            line:Int,
            lastBlock:Long,
            addressList: ArrayList<WalletObject>
        ): OrderData {
            var AvailableAmount:Long; var AmountTrfr:Long;var FeeTrfr:Long
            var OrderInfo = OrderData()

            AvailableAmount = addressList[WalletAddressIndex(origin, addressList)].Balance-getAddressPendingPays(origin)
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
            OrderInfo.TimeStamp = ordertime
            OrderInfo.Reference = reference
            OrderInfo.TrxLine = line
            OrderInfo.Sender = addressList[WalletAddressIndex(origin, addressList)].PublicKey
            OrderInfo.Address = addressList[WalletAddressIndex(origin, addressList)].Hash
            OrderInfo.Receiver = destination
            OrderInfo.AmountFee = FeeTrfr
            OrderInfo.AmountTrf = AmountTrfr
            OrderInfo.Signature = mpCripto.getStringSigned(
                ordertime.toString()+
                          origin+
                          destination+
                          AmountTrfr.toString()+
                          FeeTrfr.toString()+
                          line.toString(),
                addressList[WalletAddressIndex(origin, addressList)].PrivateKey!!
            )
            OrderInfo.TrfrID = getTransferHash(
                ordertime.toString()+
                        origin+
                        destination+
                        amount.toString()+
                        lastBlock.toString()
            )
            return OrderInfo
        }

        fun getStringFromOrder(order:OrderData):String {
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
            return "NSL"+ordertype+" "+Protocol.toString()+" "+ProgramVersion+" "+(System.currentTimeMillis()/1000)+" "
        }

        fun getOrderHash(textLine:String):String {
            var Result = mpCripto.HashSha256String(textLine)
            return "OR"+mpCripto.BMHexto58(Result, BigInteger("36"))
        }

        fun getTransferHash(textLine:String):String {
            var Resultado = ""
            var Sumatoria:String;var Clave:String

            Resultado = mpCripto.HashSha256String(textLine)
            Resultado = mpCripto.BMHexto58(Resultado, BigInteger("58"))
            Sumatoria = mpCripto.BMB58resumen(Resultado).toString()
            Clave = mpCripto.BMDecto58(Sumatoria)
            return "tR"+Resultado+Clave
        }

        fun getAddressPendingPays(address:String):Long {
            return 0L
        }

        fun ProcessPendings(
            input:String,
            addressList: ArrayList<WalletObject>,
            pendingList: ArrayList<PendingData>
        ){
            var ThisOrder:String
            var Add_index:Int
            val tokens = StringTokenizer(input)

            //Clear pendings before recalculate
            for(pending in pendingList){
                pending.Incoming = 0L
                pending.Outgoing = 0L
            }

            while(tokens.hasMoreTokens()){
                ThisOrder = tokens.nextToken()
                if(ThisOrder != ""){
                    val pendingInfo = stringToOrderData(ThisOrder)
                    if(pendingInfo.TO_Type.equals("TRFR")){
                        Add_index = WalletAddressIndex(pendingInfo.TO_Sender, addressList)
                        if(Add_index >= 0){
                            pendingList[Add_index].Outgoing = pendingList[Add_index].Outgoing+pendingInfo.TO_Amount+pendingInfo.TO_Fee
                        }
                        Add_index = WalletAddressIndex(pendingInfo.TO_Receiver, addressList)
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

        fun WalletAddressIndex(address:String, addressList:ArrayList<WalletObject>):Int{
            if(!mpParser.IsValidAddress(address) && DBManager.getSummarySize() < 0 ){
                return -1
            }

            for((index,wallet) in addressList.withIndex()){
                if(wallet.Hash.equals(address) || wallet.Custom.equals(address)){
                    return index
                }
            }
            return -1
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

        fun getDateFromUNIX(time:Long):String{
            try {
                val formatter = SimpleDateFormat("dd/MM/yyyy")
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                return formatter.format(time)
            }catch (e:Exception){
                Log.e("mpFunctions","Error parsing date")
            }
            return "00/00/0000"
        }

        fun getTimeFromUNIX(time:Long):String{
            try {
                val formatter = SimpleDateFormat("HH:mm:ss a")
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                return formatter.format(time)
            }catch (e:Exception){
                Log.e("mpFunctions","Error parsing date")
            }
            return "00:00:00"
        }


    }
}