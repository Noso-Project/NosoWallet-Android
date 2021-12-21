package com.s7evensoftware.nosowallet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.s7evensoftware.nosowallet.databinding.ActivityWelcomeBinding
import com.s7evensoftware.nosowallet.databinding.DialogSetupBinding
import io.ktor.utils.io.core.*
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.*
import java.net.InetAddress
import java.net.Socket
import kotlin.coroutines.CoroutineContext

class Welcome : AppCompatActivity(), View.OnClickListener, ServerAdapter.onServerSelected, CoroutineScope {

    lateinit var binding:ActivityWelcomeBinding
    lateinit var viewModel:MainViewModel
    private var job:Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        Realm.init(this)
        prepareViews()

        RequestPermissions()
        EjecutarInicio()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(viewModel.isSettingsOpen){
            viewModel.SettingsDialog?.dismiss()
            viewModel.isSettingsOpen = true
        }
    }

    private fun prepareViews() {
        if(viewModel.isSettingsOpen){
            viewModel.SettingsDialog?.show()
        }
        binding.welcomeSettings.setOnClickListener(this)
        binding.welcomeTestConn?.setOnClickListener(this)


    }

    private fun prepareSettingsView(view:View):View{
        val dialogBinding = DialogSetupBinding.bind(view)
        dialogBinding.settingsAddServer.setOnClickListener(this)
        dialogBinding.settingsAddServerDone.setOnClickListener(this)
        dialogBinding.settingsDeleteServer.setOnClickListener(this)
        dialogBinding.settingsServerList.visibility = View.VISIBLE
        dialogBinding.settingsAddServerContainer.visibility = View.GONE

        val serverAdapter = ServerAdapter(this)
        serverAdapter.setServers(DBManager.getServers())
        dialogBinding.settingsServerList.layoutManager = LinearLayoutManager(this)
        dialogBinding.settingsServerList.adapter = serverAdapter
        serverAdapter.notifyDataSetChanged()

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

    suspend fun client(){

        //192.168.226.227
        val Server_IP = "192.210.226.118"
        val Server_Port = 8080

        val serverAddr = InetAddress.getByName(Server_IP)
        val socket = Socket(serverAddr, Server_Port)

        try {
            val mbo = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
            val inputStream = socket.getInputStream()

            mpdisk.CreateSummFile(this)

            mbo.println("GETZIPSUMARY")
            val sFile = File(getExternalFilesDir(null)!!.path
                    +File.separator
                    +NOSPath
                    +File.separator
                    +SummaryFileName)

            val fos = FileOutputStream(sFile, false)
            var read:Int
            val bytes = ByteArray(8192)

            read = inputStream.read(bytes)


            while (read != -1) {
                Log.e("Welcome-Read", String(bytes))
                fos.write(bytes,0, read)
                read = inputStream.read(bytes)
            }

            socket.close()
            Log.e("Welcome","Fin de la ejecucion")

        }catch (e:Exception){
            Log.e("Welcome-Err",e.message!!)
        }
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.welcome_test_conn -> {
                Log.e("Welcome","Prueba de boton")
                launch {
                    client()
                }
            }
            R.id.welcome_settings -> {
                if(viewModel.SettingsDialog == null){
                    Log.e("Welcome","Was null")
                    viewModel.SettingsDialog = AlertDialog.Builder(this)
                        .setView(prepareSettingsView(layoutInflater.inflate(R.layout.dialog_setup, null)))
                        .setCancelable(true)
                        .setOnCancelListener {
                            viewModel.isSettingsOpen = false
                            Log.e("Welcome","Dialog closed")
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

    private fun RequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    1
                )
            }
        }
    }

    fun EjecutarInicio() {
        if(!directoryexist("NOSODATA")){
            mpdisk.CrearArchivoOpciones(this)
        }else{
            mpdisk.CargarOpciones(this)
        }
        mpdisk.VerificarArchivos(this)
    }

    private fun directoryexist(pathName: String): Boolean {
        val file = File(applicationContext.getExternalFilesDir(null)!!.path+File.separator+"NOSODATA")
        return file.exists()
    }

    override fun selectServer(v: View) {
        TODO("Not yet implemented")
    }


}