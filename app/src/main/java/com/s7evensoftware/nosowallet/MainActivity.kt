package com.s7evensoftware.nosowallet

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.s7evensoftware.nosowallet.databinding.ActivityMainBinding
import com.s7evensoftware.nosowallet.databinding.DialogSetupBinding
import io.realm.Realm
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope, View.OnClickListener, ServerAdapter.OnServerSelected, AddressAdapter.OnCopyDone {
    companion object {
        lateinit var UserOptions: Options
    }

    lateinit var binding:ActivityMainBinding
    lateinit var viewModel:MainViewModel
    var importWalletTask = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        val result = mpParser.ImportWallet(this, it.resultCode, it.data, viewModel.AdddressList.value!!, viewModel.PendingList.value!!)
        if(result < 0){
            when(result){
                -1 -> {Toast.makeText(this, R.string.general_import_error_1, Toast.LENGTH_SHORT).show()}
                -2 -> {Toast.makeText(this, R.string.general_import_error_2, Toast.LENGTH_SHORT).show()}
                -3 -> { /* Should I show something when cancelled? */ }
            }
        }else{
            var showMessage = getString(R.string.general_import_start) + " "+result+" "
            if(result > 1){
                showMessage += getString(R.string.general_import_end_multi)
            }else if(result == 1){
                showMessage += getString(R.string.general_import_end_single)
            }else{
                showMessage = getString(R.string.general_import_nonew_wallet)
            }
            Toast.makeText(this, showMessage, Toast.LENGTH_SHORT).show()
            Log.e("Main","Import wallet triggering")
            addressAdapter?.setAddressList(viewModel.AdddressList.value)
            viewModel.UpdateBalanceTrigger.value = viewModel.UpdateBalanceTrigger.value?.inc()
        }

    }
    var exportWalletTask = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        val result = mpParser.ExportWallet(it.resultCode, it.data, viewModel.AdddressList.value!!)
        if(result != -1 ) Toast.makeText(this, result, Toast.LENGTH_SHORT).show() // -1 = export cancelled
    }
    private var serverAdapter:ServerAdapter? = null
    private var addressAdapter:AddressAdapter? = null

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // set context for read/write tasks
        mpDisk.setContext(this)

        //Start built in DB
        Realm.init(this)
        CreateDefaultSeedNodes()

        //Start view model for whole app
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        //Entry point for all view in main
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        // Prepare external components and permissions

        RequestPermissions()
        PerformInitialization()
        RestoreBlockBranchInfo()
        prepareView()
        setContentView(view)
        RestoreSendFundsView()

        // Display Content
        CalculateGrandBalance()
        SummarySync()
        TimeTask()
    }

    private fun RestoreSendFundsView() {
        viewModel.isSendFundsOpen.observe(this, {
            if(it){
                binding.mainNetstatContainer.visibility = View.GONE
                binding.mainSendFundsContainer.visibility = View.VISIBLE
            }else{
                binding.mainNetstatContainer.visibility = View.VISIBLE
                binding.mainSendFundsContainer.visibility = View.GONE
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancel() // Cancel repetitive sync task
    }

    private fun TimeTask() {
        launch {
            while(true){
                viewModel.RealTimeValue.postValue(viewModel.RealTimeValue.value!! + 1000)
                delay(1000)
            }
        }
    }

    private fun SaveBlockBranchInfo(){
        Log.e("Main","Saving Block and Branch Info")
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putInt(getString(R.string.sharedpref_netstate_lastblock), viewModel.LastBlock.value?.toInt()?:0)
            putString(getString(R.string.sharedpref_netstate_lastbranch), viewModel.LastSummary.value)
            apply()
        }
    }

    private fun RestoreBlockBranchInfo(){
        Log.e("Main","Restoring Block and Branch Info")
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        viewModel.LastBlock.value = sharedPref.getInt(getString(R.string.sharedpref_netstate_lastblock), 0).toLong()
        viewModel.LastSummary.value = sharedPref.getString(getString(R.string.sharedpref_netstate_lastbranch), "0")
    }

    private fun CreateDefaultSeedNodes() {
        DBManager.insertDefaultNodes()
    }

    private fun UpdateWalletFromSummary() {
        viewModel.AdddressList.value?.let {
            mpFunctions.UpdateWalletFromSummary(it)
        }
    }

    private fun SummarySync() {
        launch {
            while(viewModel.SYNC_ENABLED){
                val NODEarray = ArrayList<NodeInfo>()

                DBManager.getServers()?.forEach {
                    NODEarray.add(mpNetwork.getNodeStatus(it.Address, it.Port, viewModel))
                }

                // If concensus is true then returns a candidate NodeInfo otherway returns null
                val syncNode = mpFunctions.Concensus(NODEarray, viewModel)

                if(syncNode != null){ // null == false || not null == nodeInfo
                    Log.e("Sync","Consensus failed, syncing")
                    if(mpNetwork.getSummary(applicationContext, syncNode.Address, syncNode.Port, viewModel)){
                        mpDisk.LoadSummary()
                        viewModel.WalletSynced.postValue(true)
                        SaveBlockBranchInfo()
                    }
                }else{
                    viewModel.WalletSynced.postValue(true)
                    Log.e("Sync","Consensus succeed, no summary sync is needed")
                }
                delay(viewModel.SYNC_DELAY)
            }
        }
    }

    private fun prepareView() {
        //Add buttons touch listeners
        binding.mainSettings.setOnClickListener(this)
        binding.mainWalletExport.setOnClickListener(this)
        binding.mainWalletImport.setOnClickListener(this)
        binding.mainWalletCreate.setOnClickListener(this)
        binding.mainNetstatBlockContainer.setOnClickListener(this)
        binding.mainNetstatOpenSend.setOnClickListener(this)
        binding.mainSendFundsPaste.setOnClickListener(this)
        binding.mainSendFundsClose.setOnClickListener(this)
        binding.mainSendFundsCancel.setOnClickListener(this)
        binding.mainSendFundsSend.setOnClickListener(this)
        binding.mainSendFundsSendConfirm.setOnClickListener(this)

        //Send Funds Input Text Handlers
        // Destination address
        binding.mainSendFundsDestination.addTextChangedListener {
            viewModel.SendFunds_TO = it.toString()
        }

        // Transfer amount
        binding.mainSendFundsAmount.addTextChangedListener(object : TextWatcher
            {
                var ignoreNext = false
                var replaceFirstZero = true
                var savedBeforePaste: CharSequence? = null

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    savedBeforePaste = s // Save the actual value in case is needed for paste case
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if(!ignoreNext){
                        if(s?.isEmpty() == true){
                            ignoreNext = true
                            binding.mainSendFundsAmount.setText("0.00000000")
                        }else if(s?.indexOf(".") != -1){ // Parse the input only if has decimals, other way can be passed as it is
                            s?.substring(start, start+count).let { newChar ->  // get the only new char typed
                                if(newChar?.length?:0 > 1){ // if is more than one char then is a paste
                                    if(s?.matches("[0-9]+\\.[0-9]+".toRegex()) == true){ // Check if is a valid input
                                        if(s.indexOf(".") != -1){
                                            // Fill 0's at the end if is not long enough
                                            var afterDot = s?.substring(s.indexOf("."))
                                            while(afterDot?.length != 9){
                                                afterDot += "0"
                                            }

                                            var final = s?.substring(0, s.indexOf(".")) + afterDot

                                            ignoreNext = true
                                            binding.mainSendFundsAmount.setText(final)
                                            binding.mainSendFundsAmount.setSelection(start+count)
                                        }
                                    }else{ // If is not valid, restore the previous value
                                        ignoreNext = true
                                        binding.mainSendFundsAmount.setText(savedBeforePaste)
                                    }
                                }else{ // Case of 1 new char only
                                    if(newChar == "." || newChar == ","){ // If the input is a ./,
                                        if(s?.indexOf(".") != start && s?.indexOf('.')?:0 < start){
                                            var final = s?.substring(0, start)+s?.substring(start+count)
                                            ignoreNext = true
                                            binding.mainSendFundsAmount.setText(final)
                                            binding.mainSendFundsAmount.setSelection(start)
                                        }else{
                                            // Case of integer wiht dot at the end
                                            if((s?.length?.minus(1))!! < (start+2)){
                                                val final = s.toString()+"00000000"
                                                ignoreNext = true
                                                binding.mainSendFundsAmount.setText(final)
                                                binding.mainSendFundsAmount.setSelection(start+count)
                                            }else{
                                                // Case of . before .
                                                if(s?.substring(start+1,start+2) != "." && s?.substring(s.indexOf("."))?.length?:0 < 9){
                                                    var afterDot = s?.substring(s.indexOf("."))
                                                    while(afterDot?.length != 9){
                                                        afterDot += "0"
                                                    }
                                                    var final = s?.substring(0, start) + afterDot

                                                    if(start == 0){
                                                        final = "0"+final
                                                        replaceFirstZero = true
                                                    }

                                                    ignoreNext = true
                                                    binding.mainSendFundsAmount.setText(final)
                                                    binding.mainSendFundsAmount.setSelection(start+count)
                                                }else{
                                                    if(s?.substring(start+count)?.indexOf(".") != -1){
                                                        var final =
                                                            s?.substring(0, start+count)+
                                                                    s?.substring(start+count,start+count+9)?.replace(".","")

                                                        if(start == 0){
                                                            final = "0"+final
                                                            replaceFirstZero = true
                                                        }

                                                        ignoreNext = true
                                                        binding.mainSendFundsAmount.setText(final)
                                                        binding.mainSendFundsAmount.setSelection(start+count+(if(start == 0) 1 else 0))
                                                    }
                                                }
                                            }
                                        }
                                    }else if(newChar?.matches("[0-9]".toRegex()) == true){
                                        val dotpos = s?.indexOf(".")
                                        if(dotpos?:0 < start){
                                            if(start-(dotpos?:0) > 8){
                                                var final = s?.substring(0, start)
                                                ignoreNext = true
                                                binding.mainSendFundsAmount.setText(final)
                                                binding.mainSendFundsAmount.setSelection(start)
                                            }else{
                                                if(s?.substring(dotpos?:0)?.length == 10){
                                                    var final = s?.substring(0, start+count)+s?.substring(start+count+1)
                                                    ignoreNext = true
                                                    binding.mainSendFundsAmount.setText(final)
                                                    binding.mainSendFundsAmount.setSelection(start+count)
                                                }
                                            }
                                        }else{
                                            val posbeforedot = s?.substring(0, s.indexOf("."))
                                            val afterdot = s?.substring(s.indexOf("."))
                                            if(start == 0 && posbeforedot?.length == 2 && replaceFirstZero){
                                                val final = newChar+afterdot
                                                ignoreNext = true
                                                replaceFirstZero = false
                                                binding.mainSendFundsAmount.setText(final)
                                                binding.mainSendFundsAmount.setSelection(1)
                                            }
                                        }
                                    }else{
                                        var final = s?.substring(0, start)+s?.substring(start+count)
                                        ignoreNext = true
                                        binding.mainSendFundsAmount.setText(final)
                                        binding.mainSendFundsAmount.setSelection(start)
                                    }
                                }
                            }
                        }
                    }else{
                        ignoreNext = false
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    if(s.toString().isNotEmpty()){
                        val isCommaCase = s?.indexOf(",")
                        val isPointCase = s?.indexOf(".")

                        if(isCommaCase != -1){
                            val integer = s?.substring(0,isPointCase?:0)
                            var decimal = s?.substring(isPointCase?:0)?.replace(",","")

                            if(decimal?.length?:0 > 8){
                                decimal = decimal?.substring(0,8)
                            }else{
                                while(decimal?.length?:0 < 8){
                                    decimal += "0"
                                }
                            }
                            viewModel.SendFunds_Amount = (integer+decimal).toLong()
                        }

                        if(isPointCase != -1){
                            val integer = s?.substring(0,isPointCase?:0)
                            var decimal = s?.substring(isPointCase?:0)?.replace(".","")

                            if(decimal?.length?:0 > 8){
                                decimal = decimal?.substring(0,8)
                            }else{
                                while(decimal?.length?:0 < 8){
                                    decimal += "0"
                                }
                            }
                            viewModel.SendFunds_Amount = (integer+decimal).toLong()
                        }

                        if(isPointCase == -1 && isCommaCase == -1){
                            viewModel.SendFunds_Amount = (s.toString()+"00000000").toLong()
                        }
                    }else{
                        viewModel.SendFunds_Amount = 0L
                    }

                    viewModel.AvailableBalance.value?.let {
                        if(
                            viewModel.SendFunds_Amount > 0L &&
                            viewModel.SendFunds_Amount <= mpFunctions.getMaximumToSend(it)
                        ){
                            binding.mainSendFundsAmountCheck.setImageDrawable(getDrawable(R.drawable.ic_baseline_check_circle_24))
                        }else{
                            binding.mainSendFundsAmountCheck.setImageDrawable(getDrawable(R.drawable.ic_baseline_cancel_24))
                        }
                    }
                }
            })


        //Prepare Wallet List container
        addressAdapter = AddressAdapter(this)
        addressAdapter?.setPendingList(viewModel.PendingList.value)
        addressAdapter?.setAddressList(viewModel.AdddressList.value)
        binding.mainAddressList.layoutManager = LinearLayoutManager(this)
        binding.mainAddressList.adapter = addressAdapter

        //Order Success/Fail Observer
        viewModel.TriggerSuccessError.observe(this, {
            if(it > 0){
                if(it.mod(2) == 0){
                    Toast.makeText(applicationContext, R.string.general_sendfunds_success, Toast.LENGTH_SHORT).show()
                    viewModel.TriggerSuccessError.value = 0
                }else{
                    Snackbar.make(binding.mainSendFundsSend, R.string.general_sendfunds_error_conn, Snackbar.LENGTH_SHORT).show()
                    viewModel.TriggerSuccessError.value = 0
                }
            }
        })

        //Sync Observer
        viewModel.WalletSynced.observe(this, {
            if(it && !viewModel.ConnectionError.value!!){
                binding.mainNetstatBlockContainer.background.setTint(getColor(R.color.colorGreenSync))
            }else{
                binding.mainNetstatBlockContainer.background.setTint(getColor(R.color.colorRedUnsync))
            }
        })

        //Block Observer
        viewModel.LastBlock.observe(this, {
            if(it > 0L){
                binding.mainNetstatBlockNumber.text = it.toString()
            }
        })

        //Time Observer
        viewModel.RealTimeValue.observe(this, {
            binding.mainNetstatDate.text = mpFunctions.getDateFromUNIX(it)
            binding.mainNetstatTime.text = mpFunctions.getTimeFromUNIX(it)
        })
    }

    private fun prepareSettingsView(view:View):View{
        val dialogBinding = DialogSetupBinding.bind(view)
        dialogBinding.settingsAddServer.setOnClickListener(this)
        dialogBinding.settingsAddServerDone.setOnClickListener(this)
        dialogBinding.settingsDeleteServer.setOnClickListener(this)
        dialogBinding.settingsAddServerBack.setOnClickListener(this)
        dialogBinding.settingsAppVersion.text = "v "+packageManager.getPackageInfo(packageName, 0).versionName
        dialogBinding.settingsServerList.visibility = View.VISIBLE
        dialogBinding.settingsAddServerContainer.visibility = View.GONE


        serverAdapter = ServerAdapter(this)
        serverAdapter?.setServers(DBManager.getServers())
        dialogBinding.settingsServerList.layoutManager = LinearLayoutManager(this)
        dialogBinding.settingsServerList.adapter = serverAdapter
        serverAdapter?.notifyDataSetChanged()

        dialogBinding.settingsAddServerAddressInput.addTextChangedListener {
            viewModel.SettingsAddress.value = it.toString()
        }

        dialogBinding.settingsAddServerPortInput.addTextChangedListener {
            viewModel.SettingsPort.value = it.toString()
        }

        dialogBinding.settingsAddServerAddressInput.post {
            dialogBinding.settingsAddServerAddressInput.setText(viewModel.SettingsAddress.value)
        }

        dialogBinding.settingsAddServerPortInput.post {
            dialogBinding.settingsAddServerPortInput.setText(viewModel.SettingsPort.value)
        }

        return view
    }

    private fun RequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                    applicationContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_NETWORK_STATE
                )
                != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                    applicationContext,
                    Manifest.permission.INTERNET
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    1
                )
            }
        }
    }

    fun PerformInitialization() {
        if(!directoryexist(NOSPath)){
            mpDisk.CreateOptionsFile()
        }else{
            mpDisk.LoadOptions()
        }
        mpDisk.VerifyFiles(viewModel.AdddressList.value!!, viewModel.PendingList.value!!)
        addressAdapter?.notifyDataSetChanged()
    }

    private fun CalculateGrandBalance(){
        viewModel.UpdateBalanceTrigger.observe(this, {
            UpdateWalletFromSummary()  // Individual Balance Update
            var total:Long = 0
            viewModel.AdddressList.value?.let {
                if(it.size > 0){
                    for(wallet in it){
                        total += wallet.Balance
                    }

                    viewModel.PendingList.value?.let {
                        for(pending in it){
                            total -= pending.Outgoing
                        }
                    }
                }
            }
            viewModel.AvailableBalance.value = total
            binding.mainGrandBalance.text = mpCoin.Long2Currency(total)
            addressAdapter?.notifyDataSetChanged()
        })
    }

    private fun directoryexist(pathName: String): Boolean {
        val file = File(applicationContext.getExternalFilesDir(null)!!.path+File.separator+"NOSODATA")
        return file.exists()
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.main_send_funds_paste -> {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                if(clipboard.hasPrimaryClip()){
                    if(clipboard.primaryClipDescription?.getMimeType(0) == "text/plain"){
                        val clip = clipboard.primaryClip?.getItemAt(0)
                        binding.mainSendFundsDestination.setText(clip?.text.toString())
                    }
                }
            }
            R.id.main_netstat_open_send -> { // Hides the block statu and date/time area and shows the send fund area
                binding.mainNetstatContainer.visibility = View.GONE
                binding.mainSendFundsContainer.visibility = View.VISIBLE
            }
            R.id.main_send_funds_close -> { // Show the block statu and date/time area and hides the send fund area
                binding.mainNetstatContainer.visibility = View.VISIBLE
                binding.mainSendFundsContainer.visibility = View.GONE
            }
            R.id.main_send_funds_send -> { // If valid data then shows the confirmation button
                if(viewModel.SendFunds_FROM.isNotBlank()){
                    if(viewModel.SendFunds_TO.isNotBlank()){
                        if(mpParser.IsValidAddress(viewModel.SendFunds_TO)){
                            if(viewModel.SendFunds_TO != viewModel.SendFunds_FROM){
                                if(viewModel.SendFunds_Amount > 0L){
                                    viewModel.AvailableBalance.value?.let {
                                        if(
                                            viewModel.SendFunds_Amount <= mpFunctions.getMaximumToSend(it)
                                        ){
                                            //Disable modificatiosn until confirmed or cancelled
                                            binding.mainSendFundsDestination.isEnabled = false
                                            binding.mainSendFundsAmount.isEnabled = false
                                            binding.mainSendFundsReference.isEnabled = false

                                            binding.mainSendFundsClose.visibility = View.GONE
                                            binding.mainSendFundsSend.visibility = View.GONE
                                            binding.mainSendFundsCancel.visibility = View.VISIBLE
                                            binding.mainSendFundsSendConfirm.visibility = View.VISIBLE
                                        }else{
                                            Snackbar.make(binding.mainSendFundsSend, R.string.general_sendfunds_error5, Snackbar.LENGTH_SHORT).show()
                                        }
                                    }
                                }else{
                                    Snackbar.make(binding.mainSendFundsSend, R.string.general_sendfunds_error4, Snackbar.LENGTH_SHORT).show()
                                }
                            }else{
                                Snackbar.make(binding.mainSendFundsSend, R.string.general_sendfunds_error3, Snackbar.LENGTH_SHORT).show()
                            }
                        }else{
                            Snackbar.make(binding.mainSendFundsSend, R.string.general_sendfunds_error2, Snackbar.LENGTH_SHORT).show()
                        }
                    }else{
                        Snackbar.make(binding.mainSendFundsSend, R.string.general_sendfunds_error1, Snackbar.LENGTH_SHORT).show()
                    }
                }else{
                    Snackbar.make(binding.mainSendFundsSend, R.string.general_sendfunds_error0, Snackbar.LENGTH_SHORT).show()
                }
            }
            R.id.main_send_funds_send_confirm -> {
                viewModel.WalletSynced.value?.let {
                    if(it){
                        launch{
                            var order_pending = true
                            val outgoing = viewModel.SendFunds_FROM
                            val incoming = viewModel.SendFunds_TO
                            val balance = viewModel.SendFunds_Amount
                            val ref = viewModel.SendFubds_Ref

                            while(order_pending){
                                val res = mpCripto.SendTo(
                                    outgoing,
                                    incoming,
                                    balance,
                                    ref,
                                    viewModel
                                )

                                if(res == "ok"){
                                    val out = mpFunctions.WalletAddressIndex(outgoing, viewModel.AdddressList.value!!)
                                    val inc = mpFunctions.WalletAddressIndex(incoming, viewModel.AdddressList.value!!)
                                    if(out != -1){
                                        viewModel.PendingList.value?.let {
                                            it[out].Outgoing += balance+mpCoin.GetFee(balance)
                                        }
                                    }
                                    if(inc != -1){
                                        viewModel.PendingList.value?.let {
                                            it[out].Incoming += balance
                                        }
                                    }
                                    viewModel.UpdateBalanceTrigger.postValue(viewModel.UpdateBalanceTrigger.value?:0+1)
                                    viewModel.TriggerSuccessError.postValue(viewModel.TriggerSuccessError.value!!+2)
                                    order_pending = false
                                }else{
                                    viewModel.TriggerSuccessError.postValue(viewModel.TriggerSuccessError.value!!+1)
                                    delay(DEFAULT_SYNC_DELAY)
                                }
                            }
                        }
                        binding.mainSendFundsFrom.setText("")
                        binding.mainSendFundsReference.setText("")
                        binding.mainSendFundsAmount.setText("0.00000000")
                        binding.mainSendFundsDestination.setText("")

                        binding.mainSendFundsDestination.isEnabled = true
                        binding.mainSendFundsAmount.isEnabled = true
                        binding.mainSendFundsReference.isEnabled = true

                        binding.mainSendFundsClose.visibility = View.VISIBLE
                        binding.mainSendFundsSend.visibility = View.VISIBLE
                        binding.mainSendFundsCancel.visibility = View.GONE
                        binding.mainSendFundsSendConfirm.visibility = View.GONE

                        viewModel.isSendFundsOpen.value = false
                    }else{
                        Snackbar.make(binding.mainSendFundsSend, R.string.general_sendfunds_error_sync, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            R.id.main_send_funds_cancel -> {
                binding.mainSendFundsDestination.isEnabled = true
                binding.mainSendFundsAmount.isEnabled = true
                binding.mainSendFundsReference.isEnabled = true

                binding.mainSendFundsClose.visibility = View.VISIBLE
                binding.mainSendFundsSend.visibility = View.VISIBLE
                binding.mainSendFundsCancel.visibility = View.GONE
                binding.mainSendFundsSendConfirm.visibility = View.GONE
            }
            R.id.main_wallet_create -> {
                val newAddress = mpCripto.CreateNewAddress()
                addressAdapter?.addNewWallet(newAddress)

                mpDisk.SaveWallet(viewModel.AdddressList.value!!)
                Toast.makeText(this,R.string.general_create_success,Toast.LENGTH_SHORT).show()
            }
            R.id.main_wallet_import -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    setType("*/*")
                }
                importWalletTask.launch(intent)
            }
            R.id.main_wallet_export -> {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    setType("application/pkw")
                    putExtra(Intent.EXTRA_TITLE, "androidWallet.pkw")
                }
                exportWalletTask.launch(intent)
            }
            R.id.main_settings -> {
                if(viewModel.SettingsDialog == null){
                    viewModel.SettingsDialog = AlertDialog.Builder(this)
                        .setView(prepareSettingsView(layoutInflater.inflate(R.layout.dialog_setup, null)))
                        .setCancelable(true)
                        .setOnCancelListener {
                            viewModel.isSettingsOpen = false
                        }
                        .create()
                }
                viewModel.SettingsDialog?.show()
                viewModel.isSettingsOpen = true
            }

            // Settings Dialog Actions //
            R.id.settings_add_server -> {
                val settingsServerList = viewModel.SettingsDialog?.findViewById<RecyclerView>(R.id.settings_server_list)
                val settingsAddServerContainer = viewModel.SettingsDialog?.findViewById<ScrollView>(R.id.settings_add_server_container)
                settingsServerList?.visibility = View.GONE
                settingsAddServerContainer?.visibility = View.VISIBLE
            }
            R.id.settings_delete_server -> {
                if(viewModel.SettingsServerSelected != null){
                    serverAdapter?.indexOf(viewModel.SettingsServerSelected!!)
                        ?.let {
                            DBManager.deleteServer(viewModel.SettingsServerSelected!!.Address)
                            serverAdapter?.notifyItemRemoved(it)
                            viewModel.SettingsServerSelected = null
                        }
                }
            }
            R.id.settings_add_server_back -> {
                val settingsServerList = viewModel.SettingsDialog?.findViewById<RecyclerView>(R.id.settings_server_list)
                val settingsAddServerContainer = viewModel.SettingsDialog?.findViewById<ScrollView>(R.id.settings_add_server_container)
                settingsServerList?.visibility = View.VISIBLE
                settingsAddServerContainer?.visibility = View.GONE
            }
            R.id.settings_add_server_done -> {
                if(
                    viewModel
                        .SettingsAddress
                        .value!!
                        .matches(
                            "([0-9][0-9]?[0-9]?).([0-9][0-9]?[0-9]?).([0-9][0-9]?[0-9]?).([0-9][0-9]?[0-9]?)"
                                .toRegex())
                    &&
                    viewModel
                        .SettingsPort
                        .value!!
                        .matches("[0-9]+".toRegex())
                ){
                    DBManager.insertNewServer(
                        viewModel.SettingsAddress.value?:"localhost",
                        Integer.parseInt(viewModel.SettingsPort.value?:"8080")
                    )
                    val settingsServerList = viewModel.SettingsDialog?.findViewById<RecyclerView>(R.id.settings_server_list)
                    val settingsAddServerContainer = viewModel.SettingsDialog?.findViewById<ScrollView>(R.id.settings_add_server_container)
                    settingsServerList?.visibility = View.VISIBLE
                    settingsAddServerContainer?.visibility = View.GONE

                    settingsAddServerContainer?.findViewById<EditText>(R.id.settings_add_server_address_input)?.let {
                        it.setText("")
                    }

                    settingsAddServerContainer?.findViewById<EditText>(R.id.settings_add_server_port_input)?.let {
                        it.setText("")
                    }
                }else{
                    Snackbar.make(v, R.string.settings_new_server_error, Snackbar.LENGTH_LONG).show()
                }
            }

        }
    }

    override fun onSelectServer(v: View) {
        if(!(v.tag as ServerObject).isDefault){
            viewModel.SettingsServerSelected = v.tag as ServerObject
            DBManager.setDefaultServer(v.tag as ServerObject)
            serverAdapter?.notifyDataSetChanged()
        }
    }

    override fun onAddressCopied(address: String) {
        val clipManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val addressClip = ClipData.newPlainText("Noso Address",address)
        clipManager.setPrimaryClip(addressClip)

        //Show Message "Copied"
        Toast.makeText(this, R.string.general_copytoclip, Toast.LENGTH_SHORT).show()
    }

    override fun onSourceForSendFunds(address: String) {
        viewModel.SendFunds_FROM = address
        binding.mainSendFundsFrom.setText(address)
    }

}