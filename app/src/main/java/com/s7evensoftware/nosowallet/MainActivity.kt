package com.s7evensoftware.nosowallet

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
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
import java.util.*
import kotlin.collections.ArrayList
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
        }

    }
    var exportWalletTask = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        val result = mpParser.ExportWallet(this, it.resultCode, it.data, viewModel.AdddressList.value!!)
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
    }
    private var serverAdapter:ServerAdapter? = null
    private var addressAdapter:AddressAdapter? = null



    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        UpdateWalletFromSummary()
        CalculateGrandBalance()
        SummarySync()

        // Display Content
        prepareView()
        setContentView(view)
    }

    private fun CreateDefaultSeedNodes() {
        Log.e("Main","Calling SeedNodes creation")
        DBManager.insertDefaultNodes()
    }

    private fun UpdateWalletFromSummary() {
        mpFunctions.UpdateWalletFromSummary(viewModel.AdddressList.value!!, viewModel.AddressSummary.value!!)
    }

    private fun SummarySync() {
        launch {
            while(viewModel.SYNC_ENABLED){
                //val ActualTime:Long = System.currentTimeMillis()/1000
                //Log.e("SyncThread","Current Time: "+ActualTime)

                val NODEarray = ArrayList<NodeInfo>()

                DBManager.getServers()?.forEach {
                    NODEarray.add(mpNetwork.getNodeStatus(it.Address, it.Port))
                }

                Log.e("Main","Nodes verified: "+NODEarray.size)


                // If concensus is true then returns a candidate NodeInfo otherway returns null
                val syncNode = mpFunctions.Concensus(NODEarray, viewModel)

                if(syncNode != null){ // null == false || not null == nodeInfo
                    Log.e("Sync","Concensus failed, syncing")
                    if(mpNetwork.getSummary(applicationContext, syncNode.Address, syncNode.Port)){
                        mpDisk.LoadSummary(applicationContext, viewModel.AddressSummary.value!!)
                        viewModel.REF_Addresses = true
                        val addressSummary = viewModel.AddressSummary.value
                        viewModel.WalletSynced = (addressSummary?.get(0)?.LastOP ?: 0) == viewModel.LastBlock.value
                        viewModel.REF_Status = true
                    }
                }else{
                    Log.e("Sync","Concensus succeed, no sync is needed")
                }
                viewModel.REF_Nodes = true
                viewModel.LastSyncExecution.postValue(System.currentTimeMillis())
                //CalculateGrandBalance()
                delay(viewModel.SYNC_DELAY)
            }
        }
    }

    private fun CancelSync(){
        viewModel.SYNC_ENABLED = false
    }

    private fun prepareView() {
        //Add buttons touch listeners
        binding.mainSettings.setOnClickListener(this)
        binding.mainWalletExport.setOnClickListener(this)
        binding.mainWalletImport.setOnClickListener(this)
        binding.mainWalletCreate.setOnClickListener(this)

        //Prepare Wallet List container
        addressAdapter = AddressAdapter(this)
        addressAdapter?.setAddressList(viewModel.AdddressList.value)
        binding.mainAddressList.layoutManager = LinearLayoutManager(this)
        binding.mainAddressList.adapter = addressAdapter
    }

    private fun prepareSettingsView(view:View):View{
        val dialogBinding = DialogSetupBinding.bind(view)
        dialogBinding.settingsAddServer.setOnClickListener(this)
        dialogBinding.settingsAddServerDone.setOnClickListener(this)
        dialogBinding.settingsDeleteServer.setOnClickListener(this)
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
            mpDisk.CrearArchivoOpciones(this)
        }else{
            mpDisk.CargarOpciones(this)
        }
        mpDisk.VerificarArchivos(this, viewModel.AdddressList.value!!, viewModel.AddressSummary.value!!, viewModel.PendingList.value!!)
        addressAdapter?.notifyDataSetChanged()
    }

    private fun CalculateGrandBalance(){
        var total:Long = 0
        if(viewModel.AdddressList.value != null && viewModel.AdddressList.value!!.size > 0){
            for(wallet in viewModel.AdddressList.value!!){
                total += wallet.Balance
            }
        }
        binding.mainGrandBalance.text = mpCoin.Long2Currency(total)
    }

    private fun directoryexist(pathName: String): Boolean {
        val file = File(applicationContext.getExternalFilesDir(null)!!.path+File.separator+"NOSODATA")
        return file.exists()
    }

    override fun onClick(v: View) {
        when(v.id){
            /*R.id.welcome_test_conn -> {
                launch {
                    mpNetwork.getSummary(applicationContext, viewModel)
                }
            }*/
            R.id.main_wallet_create -> {
                val newAddress = mpCripto.CreateNewAddress()
                addressAdapter?.addNewWallet(newAddress)
                mpDisk.SaveWallet(this, viewModel.AdddressList.value!!)
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
                }else{
                    Snackbar.make(v, R.string.settings_new_server_error, Snackbar.LENGTH_LONG).show()
                }
            }

        }
    }

    override fun onSelectServer(v: View) {
        if(!(v.tag as ServerObject).isDefault){
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

}