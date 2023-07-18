package com.nosoproject.nosowallet.ui.dialog

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nosoproject.nosowallet.R
import com.nosoproject.nosowallet.model.ServerObject
import com.nosoproject.nosowallet.ui.main.NosoAction
import com.nosoproject.nosowallet.viewmodels.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun DialogHandler(
    show:Boolean,
    context: Context = LocalContext.current,
    type: NosoAction,
    viewModel: MainViewModel = viewModel(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    toggleDialog: (NosoAction, Boolean) -> Unit
) {
    if(show){
        Dialog(
            onDismissRequest = {
                toggleDialog(NosoAction.HiddenDialog, false)
                //viewModel.clearDialog()
            },
            content = {
                when(type){
                    NosoAction.SettingsDialog -> {
                        SettingsDialog(
                            serverList = viewModel.serverList,
                            popServiceEnabled = viewModel.isPoPEnabled,
                            isPoolReady = viewModel.poolList.isNotEmpty()
                        ){ action, value ->
                            performAction(action = action, value = value, context = context, viewModel = viewModel, coroutineScope = coroutineScope, toggleDialog = toggleDialog)
                        }
                    }
                    NosoAction.PopSetupDialog,
                    NosoAction.PopSettingsDialog -> {
                        PoPDialog(
                            addressList = viewModel.addressList,
                            popAddress = viewModel.popAddress,
                            popPassword = viewModel.popPassword,
                            readOnly = type == NosoAction.PopSettingsDialog
                        ){ action, value ->
                            performAction(action = action, value = value, context = context, viewModel = viewModel, coroutineScope = coroutineScope, toggleDialog = toggleDialog)
                        }
                    }
                    NosoAction.LockDialog -> {
                        LockDialog(
                            password = viewModel.lockPassword,
                            confirmPassword = viewModel.lockConfirmPassword
                        ){ action, value ->
                            performAction(action = action, value = value, context = context, viewModel = viewModel, coroutineScope = coroutineScope, toggleDialog = toggleDialog)
                        }
                    }
                    NosoAction.UnlockDialog,
                    NosoAction.UnlockTempDialog -> {
                        UnlockDialog(
                            orderCase = type == NosoAction.UnlockTempDialog,
                            unlockList = if(type == NosoAction.UnlockTempDialog) viewModel.addressForFundsLocked else listOf(viewModel.sourceWallet),
                            unlockIndex = viewModel.unlockIndex,
                            password = viewModel.lockPassword
                        ){ action, value ->
                            performAction(action = action, value = value, context = context, viewModel = viewModel, coroutineScope = coroutineScope, toggleDialog = toggleDialog)
                        }
                    }
                    NosoAction.ProcessingOrder -> {
                        SendingOrderDialog { action, value ->
                            performAction(action = action, value = value, context = context, viewModel = viewModel, coroutineScope = coroutineScope, toggleDialog = toggleDialog)
                        }
                    }
                    NosoAction.AddNodeDialog -> {
                        AddNodeDialog { action, value ->
                            performAction(action = action, value = value, context = context, viewModel = viewModel, coroutineScope = coroutineScope, toggleDialog = toggleDialog)
                        }
                    }
                    NosoAction.ImportDialog -> {
                        ImportDialog { action, value ->
                            performAction(action = action, value = value, context = context, viewModel = viewModel, coroutineScope = coroutineScope, toggleDialog = toggleDialog)
                        }
                    }
                    NosoAction.QRDialog -> {
                        QRDialog(wallet = viewModel.sourceWallet){ action, value ->
                            performAction(action = action, value = value, context = context, viewModel = viewModel, coroutineScope = coroutineScope, toggleDialog = toggleDialog)
                        }
                    }
                    NosoAction.DeleteDialog -> {
                        DeleteDialog(targetWallet = viewModel.sourceWallet) { action, value ->
                            performAction(action = action, value = value, context = context, viewModel = viewModel, coroutineScope = coroutineScope, toggleDialog = toggleDialog)
                        }
                    }
                    else -> { }
                }
            }
        )
    }
}

fun performAction(action: NosoAction, value: Any, context: Context, viewModel: MainViewModel, coroutineScope: CoroutineScope, toggleDialog: (NosoAction, Boolean) -> Unit){
    when(action) {
        NosoAction.HiddenDialog,
        NosoAction.SettingsDialog,
        NosoAction.AddNodeDialog,
        NosoAction.UnlockDialog,
        NosoAction.UnlockTempDialog,
        NosoAction.LockDialog,
        NosoAction.DeleteDialog,
        NosoAction.QRDialog,
        NosoAction.PopSetupDialog,
        NosoAction.PopSettingsDialog -> { toggleDialog(action, value as Boolean) }

        NosoAction.AddNode -> {
            viewModel.addServer(value as ServerObject)
            toggleDialog(NosoAction.SettingsDialog, true)
        }
        NosoAction.AddQRWallet -> {
            if(
                viewModel.addQRWallet(value as String)
            ){
                Toast.makeText(context, "Imported 1 new wallet", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(context, "Invalid wallet or already exists", Toast.LENGTH_SHORT).show()
            }
        }
        NosoAction.AddFileWallets -> {
            val result = viewModel.addFileWallets(context, value as Intent)
            if(result < 0){
                Toast.makeText(context, R.string.general_import_error_1, Toast.LENGTH_SHORT).show()
            }else{
                toggleDialog(NosoAction.HiddenDialog, false)
                var showMessage = context.getString(R.string.general_import_start) + " "+result+" "
                if(result > 1){
                    showMessage += context.getString(R.string.general_import_end_multi)
                }else if(result == 1){
                    showMessage += context.getString(R.string.general_import_end_single)
                }else{
                    showMessage = context.getString(R.string.general_import_nonew_wallet)
                }
                Toast.makeText(context, showMessage, Toast.LENGTH_SHORT).show()
            }
        }
        NosoAction.SetPassword -> { viewModel.lockPassword = value as String }
        NosoAction.SetConfirmPassword -> { viewModel.lockConfirmPassword = value as String }
        NosoAction.LockWallet -> { viewModel.lockWallet(value as String); toggleDialog(NosoAction.HiddenDialog, false) }
        NosoAction.UnlockWallet -> {
            if(viewModel.unlockWallet(value as String)){
                toggleDialog(NosoAction.HiddenDialog, false)
            }else{
                Toast.makeText(context, R.string.dialog_unlock_failed, Toast.LENGTH_LONG).show()
            }
        }
        NosoAction.TempUnlockWallet -> {
            if(
                viewModel.unlockWallet(
                    password = value as String,
                    wallet = viewModel.addressForFundsLocked[viewModel.unlockIndex],
                    temp = true
                )
            ){
                if(viewModel.unlockIndex == viewModel.addressForFundsLocked.indices.last){
                    toggleDialog(NosoAction.ProcessingOrder, true)
                    viewModel.unlockIndex = 0
                    viewModel.addressForFundsLocked.clear()
                    coroutineScope.launch {
                        val result = viewModel.processOrder(
                            viewModel.sourceWallet,
                            viewModel.addressForFunds
                        )
                        viewModel.clearSendFunds()
                        toggleDialog(NosoAction.HiddenDialog, false)
                        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                    }
                }else{
                    viewModel.unlockIndex += 1
                }
            }else{
                Toast.makeText(context, R.string.dialog_unlock_failed, Toast.LENGTH_LONG).show()
            }
        }
        NosoAction.DeleteAddress -> {
            if(viewModel.addressList.size > 1){
                viewModel.deleteCurrentAddress()
                toggleDialog(NosoAction.HiddenDialog, false)
            }else{
                toggleDialog(NosoAction.HiddenDialog, false)
                Toast.makeText(context, "You can't delete the only address in your wallet", Toast.LENGTH_SHORT).show()
            }
        }
        NosoAction.DeleteNode -> { viewModel.removeServer(value as ServerObject) }
        NosoAction.SetPopAddress -> { viewModel.popAddress = value as String }
        NosoAction.SetPopPassword -> { viewModel.popPassword = value as String }
        else -> {}
    }
}