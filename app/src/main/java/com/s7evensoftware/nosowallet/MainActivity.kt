package com.s7evensoftware.nosowallet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.s7evensoftware.nosowallet.databinding.ActivityMainBinding
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object {
        lateinit var UserOptions: Options
        var Listadirecciones = ArrayList<WalletObject>()
        var output = ""
    }

    lateinit var binding:ActivityMainBinding
    var importWalletTask = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        mpParser.ImportWallet(this, it.resultCode, it.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        RequestPermissions()
        EjecutarInicio()
        binding.mainConsoleOutput.text = output

        binding.mainImportar.setOnClickListener {
            mpParser.showExplorer(importWalletTask)
        }

        //val test2 = mpCripto.GetAddressFromPublicKey("BCl2HkZzwXqcbOwjysbbO74J8y06oF38EDR7rF51IcNbvqDoQqe8ZbQ5sN1p0Z0ICQ83k74kIowWRwLUFFXWlBw=")
    }

    private fun RequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
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

}