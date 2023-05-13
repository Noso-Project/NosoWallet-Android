package com.s7evensoftware.nosowallet.ui.footer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s7evensoftware.nosowallet.R
import com.s7evensoftware.nosowallet.ui.customcomposable.Button
import com.s7evensoftware.nosowallet.ui.customcomposable.OutlinedTextField
import com.s7evensoftware.nosowallet.ui.main.NosoAction
import com.s7evensoftware.nosowallet.ui.theme.confirm
import com.s7evensoftware.nosowallet.ui.theme.success
import com.s7evensoftware.nosowallet.ui.theme.walletColor
import com.s7evensoftware.nosowallet.util.toNoso

enum class SendState { Closed, Fill, Confirm }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SendFunds(
    availableFunds:String,
    selectedFunds:String,
    sourceAddress:String,
    fundsDestination:String,
    isValidDestination:Boolean,
    fundsAmount:String,
    fundsReference:String,
    useAllAddress:Boolean = false,
    sendState:SendState,
    softwareKeyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current,
    clipManager: ClipboardManager = LocalClipboardManager.current,
    onAction: (NosoAction, Any) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.general_send_funds_title)
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            value = sourceAddress,
            onValueChange = { },
            label = { Text(text = stringResource(id = if(sourceAddress.isEmpty()) R.string.general_send_funds_to_placeholder else R.string.general_send_funds_from)) }
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = fundsDestination,
            onValueChange = { onAction(NosoAction.SetFundsDestination, it) },
            singleLine = true,
            label = { Text(text = stringResource(id = R.string.general_send_funds_to)) },
            leadingIcon = {
                Row(modifier = Modifier.padding(horizontal = 5.dp)) {
                    IconButton(
                        onClick = { onAction(NosoAction.SetFundsDestination, clipManager.getText()?.text?:"") },
                        modifier = Modifier
                            .width(26.dp)
                            .height(26.dp),
                        enabled = sendState != SendState.Confirm
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_content_paste_24),
                            contentDescription = null,
                            tint = walletColor
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    IconButton(
                        onClick = { onAction(NosoAction.DestinationQR, "") },
                        modifier = Modifier
                            .width(26.dp)
                            .height(26.dp),
                        enabled = sendState != SendState.Confirm
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_qricon_24),
                            contentDescription = null,
                            tint = walletColor
                        )
                    }
                }
            },
            trailingIcon = {
                Icon(
                    imageVector = if(isValidDestination) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if(isValidDestination) success else Color.Red
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, autoCorrect = false, capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = fundsAmount,
            singleLine = true,
            onValueChange = { onAction(NosoAction.SetFundsAmount, it) },
            label = { Text(text = stringResource(id = R.string.general_send_funds_amount)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            textStyle = TextStyle(
                textAlign = TextAlign.End,
                fontSize = 18.sp,
                color = if(
                    (useAllAddress && availableFunds.toFloat() >= fundsAmount.toFloat()) || (!useAllAddress && selectedFunds.toFloat() >= fundsAmount.toFloat())
                ) Color.Black else Color.Red),
            leadingIcon = {
                if(fundsAmount.isNotEmpty() && fundsAmount.toFloat() > 0){
                    IconButton(
                        onClick = { onAction(NosoAction.ClearAmount, "0.00000000") },
                        modifier = Modifier
                            .background(color = walletColor, shape = CircleShape)
                            .width(28.dp)
                            .height(28.dp)
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .width(24.dp)
                                .height(24.dp)
                        )
                    }
                }
            }
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = fundsReference,
            label = { Text(text = stringResource(id = R.string.general_send_funds_ref)) },
            onValueChange = { onAction(NosoAction.SetFundsReference, it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, autoCorrect = true, capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions {
                softwareKeyboardController?.hide()
            }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = useAllAddress,
                onCheckedChange = { onAction(NosoAction.SetFundsUseAll, it) },
                enabled = sendState != SendState.Confirm && availableFunds.toNoso() != 0L && fundsAmount.toNoso() > selectedFunds.toNoso()
            )
            Text(text = "Send from all")
            Spacer(modifier = Modifier.weight(1f))

            Row() {
                Button(
                    onClick = {
                        if(sendState != SendState.Confirm){
                            onAction(NosoAction.CancelSend, "")
                        }else{
                            onAction(NosoAction.SendFunds, "")
                        }
                    }
                ) {
                    Text(text = if(sendState != SendState.Confirm) "Close" else "Cancel", color = Color.Black)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column() {
                    AnimatedVisibility(visible = sendState == SendState.Fill) {
                        Button(
                            onClick = { onAction(NosoAction.SendConfirm, "") },
                            enabled = sourceAddress.isNotEmpty() &&
                                    isValidDestination &&
                                    fundsAmount.toFloat() > 0.0 &&
                                    (
                                            (useAllAddress && availableFunds.toFloat() >= fundsAmount.toFloat()) ||
                                                    (!useAllAddress && selectedFunds.toFloat() >= fundsAmount.toFloat())
                                            )
                        ) {
                            Text(text = "  Send  ", color = Color.Black)
                        }
                    }
                    AnimatedVisibility(visible = sendState == SendState.Confirm) {
                        Button(
                            onClick = { onAction(NosoAction.SendOrder, "") },
                            shape = RoundedCornerShape(5.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = confirm)
                        ) {
                            Text(text = "Confirm", color = Color.White)
                        }
                    }
                }
            }
        }
    }

}

//@OptIn(ExperimentalComposeUiApi::class)
//@Preview
//@Composable
//fun previewSend(){
//    NosoWalletTheme {
//        Column(modifier = Modifier
//            .fillMaxWidth()
//            .background(color = Color.White)) {
//            SendFunds(
//                availableFunds = "100",
//                selectedFunds = "100",
//                sourceAddress = "",
//                fundsReference = "",
//                fundsAmount = "",
//                fundsDestination = "",
//                sendState = SendState.Confirm,
//                useAllAddress = false
//            ){ action, value ->
//
//            }
//        }
//    }
//}
//
