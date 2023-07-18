package com.nosoproject.nosowallet.ui.dialog

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosoproject.nosowallet.model.WalletObject
import com.nosoproject.nosowallet.ui.customcomposable.Button
import com.nosoproject.nosowallet.ui.customcomposable.OutlinedTextField
import com.nosoproject.nosowallet.ui.main.NosoAction
import com.nosoproject.nosowallet.ui.theme.walletColor
import com.nosoproject.nosowallet.ui.wallet.AddressRow

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PoPDialog(
    context: Context = LocalContext.current,
    addressList:List<WalletObject>,
    popAddress: String,
    popPassword: String,
    readOnly: Boolean = false,
    onAction: (NosoAction, Any) -> Unit
) {
    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(10.dp))
            .background(color = Color.White)
            .padding(20.dp)
            .verticalScroll(state = rememberScrollState())
    ) {
        Text(
            text = "PoP Settings",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color.Black
        )
        if(!readOnly){
            Text(
                text = "Select an address (tap on it). It will be used for the proof of participation.",
                fontSize = 12.sp,
                color = Color.Black,
                lineHeight = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        if(!readOnly){
            LazyColumn(
                modifier = Modifier
                    .background(color = walletColor, shape = RoundedCornerShape(5.dp))
                    .padding(5.dp)
                    .height(150.dp)
            ){
                items(addressList){ wallet ->
                    AddressRow(address = wallet.Hash?:"", isSelected = popAddress == (wallet.Hash?: "")){
                        onAction(NosoAction.SetPopAddress, wallet.Hash?:"")
                    }
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
        }
        if(readOnly){
            OutlinedTextField(
                readOnly = true,
                value = popAddress, onValueChange = { },
                label = { Text(text = "Address:") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done)
            )
        }
        OutlinedTextField(
            readOnly = readOnly,
            value = popPassword, onValueChange = { onAction(NosoAction.SetPopPassword, it) },
            label = { Text(text = "Password:") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            isError = popPassword.length <= 8
        )
        Spacer(modifier = Modifier.height(5.dp))
        Row(modifier = Modifier.align(End)) {
            Button(
                onClick = {
                    onAction(NosoAction.SettingsDialog, true)
                }
            ) {
                Text(text = "Close", color = Color.Black)
            }
            if(!readOnly){
                Spacer(modifier = Modifier.width(5.dp))
                Button(
                    onClick = {
                        onAction(NosoAction.SwitchPoP, true)
                        onAction(NosoAction.HiddenDialog, false)
                    },
                    enabled = popAddress.isNotEmpty() && popPassword.isNotEmpty() && popPassword.length > 8
                ) {
                    Text(text = "Enable", color = Color.Black)
                }
            }
        }
    }
}