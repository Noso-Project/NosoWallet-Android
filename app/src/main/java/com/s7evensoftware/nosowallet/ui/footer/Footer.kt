package com.s7evensoftware.nosowallet.ui.footer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s7evensoftware.nosowallet.R
import com.s7evensoftware.nosowallet.model.WalletObject
import com.s7evensoftware.nosowallet.nosocore.mpFunctions
import com.s7evensoftware.nosowallet.ui.customcomposable.Button
import com.s7evensoftware.nosowallet.ui.main.NosoAction
import com.s7evensoftware.nosowallet.util.toNoso

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Footer(
    blockNumber:Long,
    syncState: SyncState,
    date:String,
    time:String,

    sendState:SendState,
    availableFunds:String,
    fundsSource: WalletObject,
    fundsDestination: String,
    isValidDestination:Boolean,
    fundsAmount:String,
    fundsReference:String,
    useAllAddress:Boolean,

    onAction: (NosoAction, Any) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(10.dp))
            .padding(5.dp)
    ) {
        AnimatedVisibility(visible = sendState == SendState.Closed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BlockState(number = blockNumber, state = syncState)
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(start = 5.dp)
                ) {
                    Text(text = date, fontSize = 12.sp)
                    Text(text = time, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onAction(NosoAction.SendFunds, "") },
                    modifier = Modifier.height(25.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.general_send_funds_title),
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }
            }
        }

        AnimatedVisibility(visible = sendState != SendState.Closed) {
            SendFunds(
                availableFunds = availableFunds,
                selectedFunds = mpFunctions.getMaximumToSend(fundsSource.Balance-fundsSource.Outgoing).toNoso(),
                sourceAddress = fundsSource.Hash?:"",
                fundsDestination = fundsDestination,
                isValidDestination = isValidDestination,
                fundsAmount = fundsAmount,
                fundsReference = fundsReference,
                useAllAddress = useAllAddress,
                sendState = sendState,
                onAction = onAction
            )
        }
    }
}

//@Composable
//@Preview
//fun footerPreview(){
//    NosoWalletTheme {
//        Footer(
//            blockNumber = 12345L,
//            syncState = SyncState.Synced,
//            date = "06/02/2023",
//            time = "21:39:07",
//            sendState = SendState.Fill
//        ){
//
//        }
//    }
//}