package com.nosoproject.nosowallet.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nosoproject.nosowallet.R

@Composable
fun BalanceView(balance:String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = stringResource(id = R.string.wallet_grand_balance),
            fontSize = 15.sp,
            color = Color.White
        )
        Text(
            text = balance,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}