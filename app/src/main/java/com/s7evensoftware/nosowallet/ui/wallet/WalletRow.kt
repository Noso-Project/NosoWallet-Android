package com.s7evensoftware.nosowallet.ui.wallet

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s7evensoftware.nosowallet.R
import com.s7evensoftware.nosowallet.model.WalletObject
import com.s7evensoftware.nosowallet.ui.main.NosoAction
import com.s7evensoftware.nosowallet.ui.theme.NosoWalletTheme
import com.s7evensoftware.nosowallet.ui.theme.walletPressed
import com.s7evensoftware.nosowallet.util.toNoso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun WalletRow(
    context: Context = LocalContext.current,
    clipManager: ClipboardManager = LocalClipboardManager.current,
    wallet: WalletObject,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    onAction: (NosoAction, Any) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val colorFade = remember { Animatable(Color.White) }

    Box() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = colorFade.value, shape = RoundedCornerShape(10.dp))
                .padding(2.dp)
                .pointerInput(menuExpanded, wallet) {
                    detectTapGestures(
                        onLongPress = {
                            coroutineScope.launch {
                                menuExpanded = true
                            }
                        },
                        onPress = {
                            coroutineScope.launch {
                                colorFade.animateTo(
                                    targetValue = walletPressed,
                                    animationSpec = tween(150)
                                )
                                //delay(80)
                                colorFade.animateTo(
                                    targetValue = Color.White,
                                    animationSpec = tween(150)
                                )
                            }
                        },
                        onTap = {
                            coroutineScope.launch {
                                onAction(NosoAction.SetFundsSource, wallet)
                            }
                        }
                    )
                },
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(0.8f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.wallet_address_label),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    if(wallet.isLocked) Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.width(10.dp).height(10.dp)
                    )
                }
                Text(text = if((wallet.Custom?:"").isEmpty()) wallet.Hash?:"Invalid Address" else wallet.Custom?:"", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                    WalletBalance(label = stringResource(id = R.string.wallet_incoming_balance), value = wallet.Incoming.toNoso(), if(wallet.Incoming > 0) Color.Green else Color.Black)
                    Spacer(modifier = Modifier.width(10.dp))
                    WalletBalance(label = stringResource(id = R.string.wallet_outgoing_balance), value = wallet.Outgoing.toNoso(), if(wallet.Outgoing > 0) Color.Red else Color.Black)
                    Spacer(modifier = Modifier.width(10.dp))
                    WalletBalance(label = stringResource(id = R.string.wallet_address_balance), value = wallet.Balance.toNoso())
                }
            }
            Row(modifier = Modifier.padding(5.dp).weight(0.2f)) {
                IconButton(
                    onClick = {
                        clipManager.setText(AnnotatedString(wallet.Hash?:"InvalidHash"))
                        Toast.makeText(context, R.string.general_copytoclip, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .width(26.dp)
                        .height(26.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_content_copy_24),
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = {
                        onAction(NosoAction.SetCurrentWallet, wallet)
                        onAction(NosoAction.QRDialog, true)
                    },
                    modifier = Modifier
                        .width(26.dp)
                        .height(26.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_qricon_24),
                        contentDescription = null
                    )
                }
            }
        }
        Box(modifier = Modifier.offset(y = (20).dp, x = 20.dp)) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(text = { Text(text = "Copy") }, onClick = { menuExpanded = false;onAction(NosoAction.CopyAddress, wallet) })
                DropdownMenuItem(text = { Text(text = "Delete") }, onClick = {
                    menuExpanded = false
                    onAction(NosoAction.SetCurrentWallet, wallet)
                    onAction(NosoAction.DeleteDialog, true)
                })
                DropdownMenuItem(text = { Text(text = "History") }, onClick = { menuExpanded = false;onAction(NosoAction.ShowHistory, true) })
                if(wallet.isLocked){
                    DropdownMenuItem(
                        text = { Text(text = "Unlock") },
                        onClick = {
                            menuExpanded = false
                            onAction(NosoAction.SetCurrentWallet, wallet)
                            onAction(NosoAction.UnlockDialog, true)
                        }
                    )
                }else{
                    DropdownMenuItem(
                        text = { Text(text = "Lock") },
                        onClick = {
                            menuExpanded = false
                            onAction(NosoAction.SetCurrentWallet, wallet)
                            onAction(NosoAction.LockDialog, true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WalletBalance(label:String, value:String, color: Color = Color.Black) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, fontSize = 12.sp)
        Text(text = value, fontSize = 12.sp, color = color)
    }
}

@Preview
@Composable
fun previewAddress(){
    NosoWalletTheme {
        WalletRow(
            wallet = WalletObject().apply {
                Hash = "Nasdf77123eihj34sidufhasdf8239rdg"
            }
        ){ _,_ -> }
    }
}