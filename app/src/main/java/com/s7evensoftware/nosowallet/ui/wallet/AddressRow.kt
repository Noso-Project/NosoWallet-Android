package com.s7evensoftware.nosowallet.ui.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AddressRow(
    address:String, isSelected:Boolean, onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(start = 3.dp, end = 1.dp, top = 5.dp, bottom = 5.dp)
            .clickable { onAction() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = address, color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.weight(1f))
        if (isSelected) Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
    }
}