package com.nosoproject.nosowallet.ui.nodes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nosoproject.nosowallet.R

@Composable
fun NodeRow(address:String, port:Int, isSelected:Boolean, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 5.dp, vertical = 2.dp)
            .clickable { onAction() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_baseline_computer_24),
            contentDescription = null,
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = "$address:$port", color = Color.White)
        Spacer(modifier = Modifier.weight(1f))
        if (isSelected) Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
    }
}