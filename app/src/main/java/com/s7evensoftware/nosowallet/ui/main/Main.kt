package com.s7evensoftware.nosowallet.ui.main

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.s7evensoftware.nosowallet.*
import com.s7evensoftware.nosowallet.R
import com.s7evensoftware.nosowallet.nosocore.mpCripto
import com.s7evensoftware.nosowallet.nosocore.mpFunctions
import com.s7evensoftware.nosowallet.nosocore.mpParser
import com.s7evensoftware.nosowallet.viewmodels.MainViewModel
import com.s7evensoftware.nosowallet.ui.addresslist.AddressList
import com.s7evensoftware.nosowallet.ui.dialog.NOSO_POP_JOB_ID
import com.s7evensoftware.nosowallet.ui.footer.Footer
import com.s7evensoftware.nosowallet.ui.footer.SendState
import com.s7evensoftware.nosowallet.ui.header.Header
import com.s7evensoftware.nosowallet.ui.menu.Menu
import com.s7evensoftware.nosowallet.ui.orderlist.OrderHistory
import com.s7evensoftware.nosowallet.ui.theme.NosoWalletTheme
import com.s7evensoftware.nosowallet.util.Log
import com.s7evensoftware.nosowallet.util.toNoso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class ListState { Address, History, GVT }
enum class NosoAction {
    SetFundsSource,
    SetFundsReference,
    SetFundsDestination,
    SetFundsAmount,
    SetFundsUseAll,
    SendFunds,
    SendConfirm,
    CancelSend,
    SendOrder,

    CopyAddress,
    CreateAddress,
    ExportWallet,
    ImportDialog,
    ClearAmount,

    HiddenDialog,
    SettingsDialog,
    LockDialog,
    UnlockDialog,
    UnlockTempDialog,
    DeleteNode,
    SwitchPoP,
    AddNodeDialog,
    AddNode,
    AddQRWallet,
    AddFileWallets,
    ShowHistory,
    DeleteAddress,
    SetCurrentWallet,
    SetConfirmPassword,
    SetPassword,
    LockWallet,
    UnlockWallet,
    DestinationQR,
    TempUnlockWallet,
    ProcessingOrder,
    DeleteDialog,
    QRDialog
}

@Composable
fun Main(
    viewModel: MainViewModel = viewModel(),
    context: Context = LocalContext.current,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    //lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    backPressedDispatcher: OnBackPressedDispatcher,
    onAction: (NosoAction, Any) -> Unit
) {
    val exportWalletTask = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){
        val result = mpParser.ExportWallet(it.resultCode, it.data, viewModel.addressList)
        if(result != -1 ) Toast.makeText(context, result, Toast.LENGTH_SHORT).show() // -1 = export cancelled
    }

    val pasteQRToDestination = rememberLauncherForActivityResult(ScanContract()){ result ->
        result.contents?.let { content ->
            viewModel.fundsDestination = content
        }
    }

    LaunchedEffect(Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when(viewModel.listState){
                    ListState.History -> { viewModel.listState = ListState.Address }
                    ListState.Address -> {
                        when(viewModel.sendFundsState){
                            SendState.Confirm -> { viewModel.sendFundsState = SendState.Fill }
                            SendState.Fill -> { viewModel.sendFundsState = SendState.Closed }
                            SendState.Closed -> {
                                isEnabled = false
                                backPressedDispatcher.onBackPressed()
                            }
                        }
                    }
                    else -> {
                        isEnabled = false
                        backPressedDispatcher.onBackPressed()
                    }
                }
            }
        }
        backPressedDispatcher.addCallback(callback)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(10.dp)
    ) {
        Header(
            onAction = {  action, value ->
                performAction(action = action, value = value, context = context, onAction = onAction, coroutineScope = coroutineScope, viewModel = viewModel)
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        Menu(
            balance = viewModel.grandBalance.toNoso(),
            onAction = { action, value ->
                performAction(action = action, value = if(action == NosoAction.ExportWallet) exportWalletTask else value, context = context, onAction = onAction, coroutineScope = coroutineScope, viewModel = viewModel)
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        when(viewModel.listState){
            ListState.Address -> {
                AddressList(
                    modifier = Modifier.weight(1f),
                    list = viewModel.addressList,
                    onAction = { action, value ->
                        performAction(action = action, value = value, context = context, onAction = onAction, coroutineScope = coroutineScope, viewModel = viewModel)
                    }
                )
            }
            ListState.History -> {
                OrderHistory(
                    modifier = Modifier.weight(1f),
                    list = viewModel.orderList,
                    onAction = { action, value ->
                        performAction(action = action, value = value, context = context, onAction = onAction, coroutineScope = coroutineScope, viewModel = viewModel)
                    }
                )
            }
            else -> {}
        }
        Spacer(modifier = Modifier.height(5.dp))
        Footer(
            blockNumber = viewModel.lastBlock.value,
            syncState = viewModel.syncStatus.value,
            date = mpFunctions.getDateFromUNIX(viewModel.currentTime.value),
            time = mpFunctions.getTimeFromUNIX(viewModel.currentTime.value),
            sendState = viewModel.sendFundsState,
            availableFunds = mpFunctions.getMaximumToSend(viewModel.grandBalance).toNoso(),
            fundsSource = viewModel.sourceWallet,
            fundsDestination = viewModel.fundsDestination,
            isValidDestination = viewModel.isValidDestination,
            fundsAmount = viewModel.fundsAmount,
            fundsReference = viewModel.fundsReference,
            useAllAddress = viewModel.useAllAddress,
            onAction = { action, value ->
                performAction(action = action, value = if(action == NosoAction.DestinationQR) pasteQRToDestination else value, context = context, onAction = onAction, coroutineScope = coroutineScope, viewModel = viewModel)
            }
        )
    }
}

fun performAction(action: NosoAction, value: Any, context: Context, onAction: (NosoAction, Any) -> Unit, coroutineScope:CoroutineScope, viewModel: MainViewModel){
    when(action){
        //Dialogs
        NosoAction.SettingsDialog,
        NosoAction.AddNodeDialog,
        NosoAction.ImportDialog,
        NosoAction.UnlockDialog,
        NosoAction.LockDialog,
        NosoAction.DeleteDialog,
        NosoAction.QRDialog -> { onAction(action, value) }
        NosoAction.ExportWallet -> {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pkw"
                putExtra(Intent.EXTRA_TITLE, "androidWallet.pkw")
            }
            (value as ManagedActivityResultLauncher<Intent, ActivityResult>).launch(intent)
        }
        NosoAction.CreateAddress -> {
            viewModel.createNewAddress()
            Toast.makeText(context, R.string.general_create_success, Toast.LENGTH_SHORT).show()
        }
        NosoAction.SetCurrentWallet -> { viewModel.sourceWallet = value as WalletObject }
        NosoAction.SetFundsSource -> {
            if(viewModel.sendFundsState != SendState.Confirm){
                viewModel.sourceWallet = value as WalletObject
            }
        }
        NosoAction.SetFundsAmount -> { parseNosoInput(viewModel = viewModel, value = value as String) }
        NosoAction.ClearAmount -> { viewModel.fundsAmount = "0.00000000"; viewModel.replaceFirstZero = true }
        NosoAction.SetFundsDestination -> {
            if(viewModel.sendFundsState != SendState.Confirm){
                viewModel.fundsDestination = value as String
                viewModel.validateDestination(viewModel.fundsDestination)
            }
        }
        NosoAction.DestinationQR -> {
            (value as ManagedActivityResultLauncher<ScanOptions, ScanIntentResult>).launch(ScanOptions())
        }
        NosoAction.ShowHistory -> {
            viewModel.listState = ListState.History
        }
        NosoAction.SetFundsReference -> { viewModel.fundsReference = value as String }
        NosoAction.SetFundsUseAll -> { viewModel.useAllAddress = value as Boolean }
        NosoAction.SendFunds -> { viewModel.sendFundsState = SendState.Fill }
        NosoAction.SendConfirm -> { viewModel.sendFundsState = SendState.Confirm }
        NosoAction.CancelSend -> { viewModel.sendFundsState = SendState.Closed }
        NosoAction.SendOrder -> {
            viewModel.addressForFunds.clear()
            viewModel.addressForFundsLocked.clear()
            val neededAddresses = mutableListOf<WalletObject>()
            val lockedAddresses = mutableListOf<WalletObject>()
            if(viewModel.useAllAddress){
                val rawAmount = viewModel.fundsAmount.toNoso()
                val withFees = rawAmount+ mpCripto.GetFee(rawAmount)
                var pending = withFees
                for(w in viewModel.addressList){
                    if((w.Balance - w.Outgoing) <= 0L) continue
                    pending -= (w.Balance-w.Outgoing)
                    neededAddresses.add(w)
                    if(w.isLocked) lockedAddresses.add(w)
                    if(pending < 0) break
                }
                viewModel.addressForFunds.addAll(neededAddresses)
                viewModel.addressForFundsLocked.addAll(lockedAddresses)
            }else{
                viewModel.addressForFunds.add(viewModel.sourceWallet)
                if(viewModel.sourceWallet.isLocked) viewModel.addressForFundsLocked.add(viewModel.sourceWallet)
            }

            if(viewModel.addressForFundsLocked.isNotEmpty()){
                onAction(NosoAction.UnlockTempDialog, true)
            }else{
                onAction(NosoAction.ProcessingOrder, true)
                viewModel.unlockIndex = 0
                viewModel.addressForFundsLocked.clear()
                coroutineScope.launch {
                    val result = viewModel.processOrder(
                        viewModel.sourceWallet,
                        viewModel.addressForFunds
                    )
                    viewModel.clearSendFunds()
                    onAction(NosoAction.HiddenDialog, false)
                    Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                }
            }
        }
        else -> { }
    }
}
fun parseNosoInput(viewModel: MainViewModel, value:String) {
    val dotChar = if("0,0".toDoubleOrNull() != null) "," else "."
    val savedBeforePaste = viewModel.fundsAmount
    val s = value

    var nC = ""
    var nCi = -1
    var count: Int

    if(savedBeforePaste.length > s.length) {
        count = savedBeforePaste.length - s.length
        val dotPos = s.indexOf(dotChar)
        if(dotPos != -1){
            var finalI = s.substring(0, dotPos)
            if(finalI == ""){ finalI = "0$finalI";viewModel.replaceFirstZero = true }
            var finalE = s.substring(dotPos+1)
            while(finalE.length < 8) finalE = "${finalE}0"
            viewModel.fundsAmount = "$finalI$dotChar$finalE"
        }
        return
    }else{
        count = s.length - savedBeforePaste.length
        var r = count
        for((i,o) in savedBeforePaste.withIndex()){
            if(o != s[i]){
                nC += "${s[i]}"
                if(r == count) nCi = i
                r--
                if(r == 0) break
            }
        }

        if(nC == ""){
            nCi = s.length-(s.length-savedBeforePaste.length)
            nC = s.substring(nCi)
        }
    }

    if(s.isEmpty()){
        viewModel.fundsAmount = ("0${dotChar}00000000")
    }else if(s.indexOf(dotChar) != -1){ // Parse the input only if has decimals, other way can be passed as it is
        nC.let { newChar ->  // get the new chars typed
            if(newChar.length > 1){
                // if is more than one char then is a paste
                if(s == "0${dotChar}00000000") viewModel.replaceFirstZero = true
                if(s.matches("\\d+$dotChar\\d+".toRegex())){ // Check if is a valid input
                    if(s.indexOf(dotChar) != -1){
                        // Fill 0's at the end if is not long enough
                        var afterDot = s.substring(s.indexOf(dotChar))
                        while(afterDot.length != 9) afterDot += "0"
                        viewModel.fundsAmount = s.substring(0, s.indexOf(dotChar)) + afterDot
                    }
                }else{ // If is not valid, restore the previous value
                    viewModel.fundsAmount = (savedBeforePaste)
                }
            }else{
                // Case of 1 new char only
                if(newChar == dotChar){
                    // If the input is a decimal char
                    if(s.substring(s.indexOf(dotChar)).startsWith("$dotChar$dotChar")){
                        // There is a double dot ..
                        viewModel.fundsAmount = s.replace("$dotChar$dotChar",dotChar)
                    }else if(s.indexOf(dotChar) != nCi && s.indexOf(dotChar) < nCi){
                        // There is a dot before new dot
                        val finalI = "0"+s
                            .substring(0, nCi)
                            .replace(dotChar,"")
                        var finalE = s.substring(nCi+count)
                        while(finalE.length < 8) finalE = "${finalE}0"
                        viewModel.fundsAmount = "$finalI$dotChar$finalE"
                    }else{
                        // Case of integer with dot at the end
                        if((s.length.minus(1)) < (nCi+2)){
                            viewModel.fundsAmount = "${s}00000000"
                        }else{
                            // Case of . before .
                            if(s.substring(nCi+1,nCi+2) != dotChar && s.substring(s.indexOf(dotChar)).length < 9){
                                var afterDot = s.substring(s.indexOf(dotChar))
                                while(afterDot.length != 9) afterDot += "0"
                                var final = s.substring(0, nCi) + afterDot
                                if(nCi == 0){
                                    final = "0$final"
                                    viewModel.replaceFirstZero = true
                                }
                                viewModel.fundsAmount = final
                            }else{
                                if(s.substring(nCi+count).indexOf(dotChar) != -1){
                                    var final = s.substring(0, nCi+1) +
                                            s.substring(nCi+1,nCi+1+9).replace(dotChar,"")
                                    if(nCi == 0){
                                        final = "0$final"; viewModel.replaceFirstZero = true
                                    }
                                    viewModel.fundsAmount = final
                                }
                            }
                        }
                    }
                }else if(newChar.matches("\\d".toRegex())){
                    val dotPos = s.indexOf(dotChar)
                    if(dotPos < nCi){
                        if(nCi-dotPos > 8){
                            viewModel.fundsAmount = s.substring(0, nCi)
                        }else{
                            if(s.substring(dotPos).length == 10){
                                val final = s.substring(0, nCi+count) + s.substring(nCi+count+1)
                                viewModel.fundsAmount = final
                            }
                        }
                    }else{
                        val posbeforedot = s.substring(0, s.indexOf(dotChar))
                        val afterdot = s.substring(s.indexOf(dotChar))
                        if((nCi == 0 || posbeforedot == "00") && posbeforedot.length == 2 && viewModel.replaceFirstZero){
                            val final = newChar+afterdot
                            viewModel.replaceFirstZero = false
                            viewModel.fundsAmount = (final)
                        }else{
                            if(nC.toLongOrNull() != null){
                                viewModel.fundsAmount = s
                            }
                        }
                    }
                }else{
                    val final = s.substring(0, nCi) + s.substring(nCi+count)
                    viewModel.fundsAmount = (final)
                }
            }
        }
    }
}

//@Preview
//@Composable
//fun previewMain(){
//    NosoWalletTheme {
//        val viewModel:MainViewModel = viewModel()
//        viewModel.addressList = arrayListOf(
//            WalletObject().apply { Hash = "Nthisisafakewalletobjectjustfortest"},
//            WalletObject().apply { Hash = "Nthisisafakewalletobjectjustfortest"},
//        )
//        Main(viewModel = viewModel){ action, value ->
//
//        }
//    }
//}
//
