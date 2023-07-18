package com.nosoproject.nosowallet.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosoproject.nosowallet.ui.theme.NosoWalletTheme

@Composable
fun MenuOption(title:String, icon:Int, onAction:() -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 12.sp, color = Color.White)
        IconButton(
            onClick = onAction,
            modifier = Modifier
                .background(color = Color.White, shape = RoundedCornerShape(5.dp))
                .padding(0.dp)
                .height(30.dp)
                .width(30.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = Color.Black
            )
        }
    }
}

@Preview
@Composable
fun previewMenu(){
    NosoWalletTheme {
        MenuOption("New", com.nosoproject.nosowallet.R.drawable.ic_export_icon){

        }
    }
}
