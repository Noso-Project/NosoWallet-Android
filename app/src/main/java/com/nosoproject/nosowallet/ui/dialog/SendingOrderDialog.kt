package com.nosoproject.nosowallet.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosoproject.nosowallet.ui.main.NosoAction
import com.nosoproject.nosowallet.ui.theme.walletColor

@Composable
fun SendingOrderDialog(
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
            text = "Processing new Order...",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(10.dp))
        CircularProgressIndicator(color = walletColor, modifier = Modifier.align(CenterHorizontally))
    }
}