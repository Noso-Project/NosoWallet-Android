package com.nosoproject.nosowallet.ui.header

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosoproject.nosowallet.ui.main.NosoAction
import com.nosoproject.nosowallet.R

@Composable
fun Header(
    onAction: (NosoAction, Any) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(10.dp))
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.noso_coin),
            contentDescription = null,
            modifier = Modifier
                .height(45.dp)
                .width(45.dp)
        )
        Column(modifier = Modifier
            .width(IntrinsicSize.Max)
            .padding(start = 10.dp)) {
            Text(
                text = "NOSO",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(y = 4.dp)
            )
            Text(
                text = "m o b i l e",
                fontSize = 12.sp,
                textAlign = TextAlign.Justify,
                modifier = Modifier.offset(y = (-4).dp)
                //modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { onAction(NosoAction.SettingsDialog, true) }) {
            Image(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = null,
                modifier = Modifier
                    .height(48.dp)
                    .width(48.dp)
            )
        }
    }
}

//@Composable
//@Preview
//fun previewHeader(){
//    NosoWalletTheme {
//        Header(){
//
//        }
//    }
//}