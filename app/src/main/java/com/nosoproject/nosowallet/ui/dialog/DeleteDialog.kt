package com.nosoproject.nosowallet.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosoproject.nosowallet.model.WalletObject
import com.nosoproject.nosowallet.ui.customcomposable.Button
import com.nosoproject.nosowallet.ui.main.NosoAction

@Composable
fun DeleteDialog(
    targetWallet: WalletObject,
    onAction: (NosoAction, Any) -> Unit
) {
    Column(
    modifier = Modifier
        .clip(shape = RoundedCornerShape(10.dp))
        .background(color = Color.White)
        .padding(20.dp)
        .verticalScroll(state = rememberScrollState())
    )
    {
        Text(
            text = "Delete address",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color.Black
        )
        Text(
            text = "This will permanently delete the address and keys, you can't undo this action.",
            fontSize = 12.sp,
            color = Color.Black,
            lineHeight = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        TextField(value = targetWallet.Hash?:"", onValueChange = {}, readOnly = true, singleLine = true)
        Spacer(modifier = Modifier.width(10.dp))
        Row(modifier = Modifier.align(End)) {
            Button(onClick = { onAction(NosoAction.HiddenDialog, true) }) {
                Text(text = "Cancel", color = Color.Black)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(onClick = { onAction(NosoAction.DeleteAddress, targetWallet) }) {
                Text(text = "Delete", color = Color.Black)
            }
        }
    }
}