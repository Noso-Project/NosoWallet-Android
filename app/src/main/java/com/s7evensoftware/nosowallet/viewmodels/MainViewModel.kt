package com.s7evensoftware.nosowallet.viewmodels


import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.s7evensoftware.nosowallet.*
import com.s7evensoftware.nosowallet.model.DBManager
import com.s7evensoftware.nosowallet.model.DEFAULT_SYNC_DELAY
import com.s7evensoftware.nosowallet.model.MISSING_FUNDS
import com.s7evensoftware.nosowallet.model.NOSPath
import com.s7evensoftware.nosowallet.nosocore.*
import com.s7evensoftware.nosowallet.popservice.PoPService
import com.s7evensoftware.nosowallet.ui.footer.SendState
import com.s7evensoftware.nosowallet.ui.footer.SyncState
import com.s7evensoftware.nosowallet.ui.main.ListState
import com.s7evensoftware.nosowallet.util.Log
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(private val app:Application): AndroidViewModel(app) {

    private val config = RealmConfiguration.Builder(setOf(ServerObject::class, OrderObject::class, SumaryData::class)).build()
    private val realmDB = Realm.open(config)

    //mainList
    var listState by mutableStateOf(ListState.Address)

    //newVars
    var grandBalance by mutableStateOf(0L)
    var sendFundsState by mutableStateOf(SendState.Closed)
    var isPoPEnabled by mutableStateOf(false)
    var lockPassword by mutableStateOf("")
    var lockConfirmPassword by mutableStateOf("")

    //Funds Variables
    var sourceWallet by mutableStateOf(WalletObject())
    var fundsDestination by mutableStateOf("")
    var isValidDestination by mutableStateOf(false)
    var fundsAmount by mutableStateOf("0.00000000")
    var fundsReference by mutableStateOf("")
    var useAllAddress by mutableStateOf(false)

    //Funds Amount Parsing
    var replaceFirstZero  = true

    val addressForFunds = mutableListOf<WalletObject>()
    val addressForFundsLocked = mutableListOf<WalletObject>()
    var unlockIndex by mutableStateOf(0)

    private val _addressList = mutableStateListOf<WalletObject>()
    var addressList:List<WalletObject> = _addressList

    private val _serverList = mutableStateListOf<ServerObject>()
    var serverList:List<ServerObject> = _serverList

    private val _orderList = mutableStateListOf<OrderObject>()
    var orderList:List<OrderObject> = _orderList

    private val _ghostList = mutableListOf<WalletObject>()
    private val _mnList = mutableListOf<ServerObject>()

    private val lastNodeSelected = mutableStateOf<NodeInfo?>(null)

    var isSyncEnabled = true
    private val syncDelay = mutableStateOf(DEFAULT_SYNC_DELAY)
    val syncStatus = mutableStateOf(SyncState.Syncing)

    val currentTime = mutableStateOf(System.currentTimeMillis())
    val lastBlock = mutableStateOf(0L)
    private val lastSummary = mutableStateOf("")
    private val lastPendingCount = mutableStateOf(0L)

    init {
        restoreBlockBranchInfo()
        viewModelScope.launch { performInit() }
        viewModelScope.launch { timeTask() }
        viewModelScope.launch { isPopRunning() }
        viewModelScope.launch { createSeedNodes();syncMNodes();summarySync() }
    }

    private fun isPopRunning(){
        val activityManager = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)

        val serviceClassName = PoPService::class.java.name
        var isServiceRunning = false

        for (service in runningServices) {
            if (serviceClassName == service.service.className) {
                isServiceRunning = true
                break
            }
        }

        isPoPEnabled = isServiceRunning
    }

    private suspend fun createSeedNodes(){
        DBManager.insertDefaultNodes(realmDB)
    }

    fun createNewAddress(): WalletObject {
        val newAddress = mpCripto.CreateNewAddress()
        _addressList.add(newAddress)
        mpDisk.SaveWallet(addressList)
        return newAddress
    }

    private suspend fun performInit(){
        if(!directoryExist(NOSPath)){
            mpDisk.CreateOptionsFile()
        }

        mpDisk.VerifyFiles(addressList, realmDB).let { pack ->
            _addressList.clear()
            _addressList.addAll(pack.normalList)

            _ghostList.clear()
            _ghostList.addAll(pack.ghostList)
        }

        _orderList.addAll(DBManager.getOrders(realmDB))
    }

    private fun calculateBalance(clearPendings:Boolean = false){
        var total = 0L
        val newList = mutableListOf<WalletObject>()
        for(w in addressList){
            val burnBalance = DBManager.getAddressBalance(w.Hash?:"", realmDB)
            w.copy(balance = burnBalance).let {
                if(clearPendings){ it.Incoming = 0;it.Outgoing = 0 }
                it.Balance -= it.Outgoing
                total += it.Balance
                newList.add(it)
                if(w.Hash == sourceWallet.Hash) sourceWallet = it
            }
        }

        grandBalance = total
        _addressList.clear()
        _addressList.addAll(newList)
    }

    private fun directoryExist(pathName: String): Boolean {
        val file = File(app.getExternalFilesDir(null)!!.path+ File.separator+pathName)
        return file.exists()
    }

    private fun restoreBlockBranchInfo(){
        val sharedPref = app.getSharedPreferences("NOSOPREF",Context.MODE_PRIVATE) ?: return
        lastBlock.value = sharedPref.getLong(app.getString(R.string.sharedpref_netstate_lastblock), 0)
        lastSummary.value = sharedPref.getString(app.getString(R.string.sharedpref_netstate_lastbranch), "0")?:"0"
        Log.e("Main","Restoring Block(${lastBlock.value}) and Branch(${lastSummary.value}) Info")
    }

    private fun saveBlockBranchInfo(lastBlock:Long, lastBrach:String){
        Log.e("Main","Saving Block($lastBlock) and Branch($lastBrach) Info")
        val sharedPref = app.getSharedPreferences("NOSOPREF",Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putLong(app.getString(R.string.sharedpref_netstate_lastblock), lastBlock)
            putString(app.getString(R.string.sharedpref_netstate_lastbranch), lastBrach)
            apply()
        }
    }

    private suspend fun timeTask() {
        while(true){
            currentTime.value += 1000
            delay(1000)
        }
    }

    private suspend fun syncMNodes(){
        withContext(Dispatchers.IO){
            var list = listOf<ServerObject>()
            while(list.isEmpty()){
                val randomServer = mpFunctions.getRandomServer(DBManager.getServers(realmDB))
                list = mpNetwork.getMasterNodeList(
                    targetAddress = randomServer.Address,
                    targetPort = randomServer.Port,
                    mnList = _mnList,
                    syncDelay = syncDelay,
                    syncStatus = syncStatus
                )
            }
            DBManager.updateNodes(list, realmDB)
            _serverList.clear()
            _serverList.addAll(list)
        }
    }

    private suspend fun summarySync(){
        while(isSyncEnabled){
            val nodeArray = ArrayList<NodeInfo>()

            val currentList = serverList.toMutableList()
            currentList.forEach { node ->
                withContext(Dispatchers.IO){
                    mpNetwork.getNodeStatus(
                        targetAddress = node.Address,
                        targetPort = node.Port,
                        syncDelay = syncDelay,
                        syncStatus = syncStatus
                    ).let { nodestat ->
                        if(nodestat.Address != ""){
                            nodeArray.add(nodestat)
                        }
                    }
                }
            }

            if(nodeArray.size > 0){
                // If concensus is true then returns a candidate NodeInfo other way returns null
                val syncNode = mpFunctions.Concensus(
                    nodeArray = nodeArray,
                    addressList = _addressList,
                    lastBlock = lastBlock,
                    lastSummary = lastSummary,
                    lastPendingCount = lastPendingCount,
                    lastNodeSelected = lastNodeSelected,
                    currentTime = currentTime,
                    syncDelay = syncDelay,
                    syncStatus = syncStatus,
                    realmDB
                )

                if(syncNode != null){ // null == false || not null == nodeInfo
                    Log.e("Sync","Consensus failed, syncing")
                    if(
                        mpNetwork.getSummary(
                            context = app,
                            address = syncNode.Address,
                            port = syncNode.Port,
                            syncStatus = syncStatus
                        )
                    ){
                        mpDisk.LoadSummary(realmDB)
                        lastBlock.value = syncNode.LastBlock
                        lastSummary.value = syncNode.LastBranch
                        syncStatus.value = SyncState.Synced

                        calculateBalance(clearPendings = true)
                        saveBlockBranchInfo(syncNode.LastBlock, syncNode.LastBranch)
                    }
                }else{
                    syncStatus.value = SyncState.Synced
                    calculateBalance()
                }
            }else{
                syncStatus.value = SyncState.FatalError
                Log.e("Sync","Consensus failed, unable to reach any node")
            }

            delay(syncDelay.value)
        }
    }

    fun validateDestination(destination:String) {
        if(mpParser.IsValidAddress(destination)){
            isValidDestination = true
        }else{
            isValidDestination = DBManager.isAliasUsed(destination, realmDB)
        }
    }

    suspend fun processOrder(source:WalletObject, sourceList: MutableList<WalletObject>):String{
        var failCount = 0
        val outgoing = source.Hash?:""
        val incoming = fundsDestination
        val balance = fundsAmount.replace(".", "").replace(",","").toLong()
        val ref = fundsReference.replace(" ","_")

        while(failCount < 5){
            val res = mpCripto.SendTo(
                outgoing,
                incoming,
                balance,
                ref,
                lastBlock.value,
                useAllAddress,
                sourceList,
                lastNodeSelected.value,
                syncStatus,
                realmDB
            )

            if(res != ""){
                if(res == MISSING_FUNDS){
                    return "Not enough funds to complete order"
                }else{
                    Log.e("Main","Order success, OrderID: $res")
                    for(w in sourceList){
                        Log.e("MVM", "${w.Hash} -> -${w.Outgoing}")
                    }
                    val newOrder = OrderObject()
                    newOrder.OrderID = res
                    newOrder.Destination = incoming
                    newOrder.Amount = balance
                    _orderList.add(newOrder)
                    DBManager.insertOrder(newOrder, realmDB)

                    return "Order sent to mainnet"
                }
            }else{
                Toast.makeText(app, "Error sending order to mainnet, retrying....", Toast.LENGTH_SHORT).show()
                failCount++
                delay(DEFAULT_SYNC_DELAY)
            }
        }

        return "Failed to send order to mainnet, retry later"
    }

    fun lockWallet(password:String){
        Log.e("MVM", "Locking Address ${sourceWallet.Hash}")
        val hashedPass = mpCripto.HashSha256String(password)
        val encryptedKey = "*"+ mpCripto.XorEncode(hashedPass, sourceWallet.PrivateKey?:"")
        sourceWallet.PrivateKey = encryptedKey
        sourceWallet.isLocked = true

        val updatedList = addressList.toMutableList()
        updatedList.indexOfFirst { it.Hash == sourceWallet.Hash }.let {
            if(it != -1){
                updatedList.removeAt(it)
                updatedList.add(it, sourceWallet)

                mpDisk.SaveWallet(updatedList)
                _addressList.clear()
                _addressList.addAll(updatedList)
            }
        }
        lockPassword = ""
        lockConfirmPassword = ""
        Log.e("MVM", "Lock Address ${sourceWallet.Hash} - Ok")
    }

    fun unlockWallet(password:String, wallet:WalletObject = sourceWallet, temp:Boolean = false):Boolean{
        try {
            val hashedPass = mpCripto.HashSha256String(password)
            val decryptedKey = mpCripto.XorDecode(hashedPass, (wallet.PrivateKey?:"*0").substring(1))
            val testSignature = mpCripto.getStringSigned("VERIFICATION",decryptedKey)
            val verification = mpCripto.VerifySignedString("VERIFICATION", testSignature, wallet.PublicKey?:"")

            if(verification){
                wallet.PrivateKey = decryptedKey
                wallet.isLocked = false

                if(!temp){
                    val updatedList = addressList.toMutableList()
                    updatedList.indexOfFirst { it.Hash == wallet.Hash }.let {
                        if(it != -1){
                            updatedList.removeAt(it)
                            updatedList.add(it, wallet)

                            mpDisk.SaveWallet(updatedList)
                            _addressList.clear()
                            _addressList.addAll(updatedList)
                        }
                    }
                }

                lockPassword = ""
                lockConfirmPassword = ""
                return true
            }else{
                Log.e("MVM", "Unlock Verification Failed")
                return false
            }
        }catch (e:Exception){
            Log.e("MVM", "Unlock Verification Fatal Error: ${e.message}")
            return false
        }
    }

    fun addQRWallet(content: String):Boolean{
        mpParser.ImportQRWallet(content, addressList)?.let {
            _addressList.add(it)
            return true
        }
        return false
    }

    fun addFileWallets(context: Context, data: Intent):Int{
        mpParser.ImportWallet(context, data, addressList)?.let {
            val oldSize = addressList.size
            _addressList.clear()
            _addressList.addAll(it)
            viewModelScope.launch { calculateBalance() }
            return it.size-oldSize
        }
        return -1
    }

    fun deleteCurrentAddress(){
        _ghostList.add(sourceWallet)
        mpDisk.SaveErased(_ghostList)
        _addressList.remove(sourceWallet)
        mpDisk.SaveWallet(_addressList)
    }

    fun addServer(node:ServerObject){
        _serverList.add(node)
        viewModelScope.launch {
            DBManager.insertNewServer(
                node.Address,
                node.Port,
                realmDB
            )
        }
    }

    fun removeServer(node:ServerObject){
        _serverList.remove(node)
        viewModelScope.launch {
            DBManager.deleteServer(node.Address, realmDB)
        }
    }

    fun clearSendFunds() {
        isValidDestination = false
        addressForFunds.clear()
        addressForFundsLocked.clear()
        fundsDestination = ""
        fundsAmount = "0.00000000"
        fundsReference = ""
        sendFundsState = SendState.Closed
    }
}