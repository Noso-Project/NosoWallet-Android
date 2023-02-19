package com.s7evensoftware.nosowallet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.s7evensoftware.nosowallet.nosocore.mpDisk
import com.s7evensoftware.nosowallet.ui.dialog.DialogHandler
import com.s7evensoftware.nosowallet.ui.main.Main
import com.s7evensoftware.nosowallet.ui.main.NosoAction
import com.s7evensoftware.nosowallet.ui.theme.NosoWalletTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // set context for read/write tasks
        requestPermissions()
        mpDisk.setContext(this)

        setContent {
            NosoWalletTheme {
                val coroutineScope = rememberCoroutineScope()
                val showDialog = remember { mutableStateOf(false) }
                val dialogType = remember { mutableStateOf(NosoAction.HiddenDialog) }


                DialogHandler(
                    show = showDialog.value && dialogType.value != NosoAction.HiddenDialog,
                    type = dialogType.value,
                    coroutineScope = coroutineScope
                ){ targetType, value ->
                    if(dialogType.value != NosoAction.HiddenDialog){
                        if(dialogType.value != targetType && value){
                            dialogType.value = targetType
                            showDialog.value = false
                        }else{
                            dialogType.value = NosoAction.HiddenDialog
                            showDialog.value = false
                        }
                    }else{
                        dialogType.value = targetType
                        showDialog.value = value
                    }
                }

                DialogHandler(
                    show = dialogType.value != NosoAction.HiddenDialog && !showDialog.value,
                    type = dialogType.value,
                    coroutineScope = coroutineScope
                ){ targetType, value ->
                    if(dialogType.value != NosoAction.HiddenDialog){
                        if(dialogType.value != targetType && value){
                            dialogType.value = targetType
                            showDialog.value = true
                        }else{
                            dialogType.value = NosoAction.HiddenDialog
                            showDialog.value = true
                        }
                    }else{
                        showDialog.value = value
                        dialogType.value = targetType
                    }
                }

                Main(
                    backPressedDispatcher = onBackPressedDispatcher
                ){ action, value ->
                    dialogType.value = action
                    showDialog.value = value as Boolean
                }
            }
        }
    }

    private fun requestPermissions() {
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