package com.s7evensoftware.nosowallet.ui.footer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.s7evensoftware.nosowallet.R
import com.s7evensoftware.nosowallet.ui.theme.NosoWalletTheme
import com.s7evensoftware.nosowallet.ui.theme.success

enum class SyncState { Syncing, Synced, NetworkError, FatalError, Retrying }

@Composable
fun BlockState(number:Long, state:SyncState) {
    Row(
        modifier = Modifier
            .background(
                color = when(state) {
                    SyncState.Synced -> success
                    SyncState.NetworkError -> Color.Red
                    SyncState.FatalError -> Color.Gray
                    SyncState.Retrying -> Color.Cyan
                    SyncState.Syncing -> Color.Yellow
                },
                shape = RoundedCornerShape(5.dp)
            )
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(id = R.drawable.ic_block), contentDescription = null)
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = number.toString(), color = Color.White)
    }
}

@Composable
@Preview
fun blockPreview(){
    NosoWalletTheme() {
        BlockState(number = 12345, state = SyncState.Synced)
    }
}