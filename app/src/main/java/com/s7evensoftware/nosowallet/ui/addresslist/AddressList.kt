package com.s7evensoftware.nosowallet.ui.addresslist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.s7evensoftware.nosowallet.model.WalletObject
import com.s7evensoftware.nosowallet.ui.main.NosoAction
import com.s7evensoftware.nosowallet.ui.wallet.WalletRow

@Composable
fun AddressList(modifier: Modifier, list:List<WalletObject>, onAction: (NosoAction, Any) -> Unit) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(10.dp))
            .padding(10.dp)
    ){
        items(list){wallet ->
            WalletRow(wallet = wallet, onAction = onAction)
            Spacer(modifier = Modifier.height(2.dp))
            Divider(color = Color.Black)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

//@Preview
//@Composable
//fun listPreview(){
//    NosoWalletTheme {
//        val list = listOf(
//            WalletObject().apply { Hash = "Nthisisafakewalletobjectjustfortest"},
//            WalletObject().apply { Hash = "Nthisisafakewalletobjectjustfortest"},
//        )
//        AddressList(modifier = Modifier, list = list){
//
//        }
//    }
//}
