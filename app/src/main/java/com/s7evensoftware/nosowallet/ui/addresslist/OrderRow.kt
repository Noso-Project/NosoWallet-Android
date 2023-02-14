package com.s7evensoftware.nosowallet.ui.addresslist

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s7evensoftware.nosowallet.OrderObject
import com.s7evensoftware.nosowallet.R
import com.s7evensoftware.nosowallet.ui.main.NosoAction
import com.s7evensoftware.nosowallet.ui.theme.NosoWalletTheme
import com.s7evensoftware.nosowallet.util.toNoso

@Composable
fun OrderRow(
    context: Context = LocalContext.current,
    clipManager: ClipboardManager = LocalClipboardManager.current,
    order: OrderObject,
    onAction: (NosoAction, Any) -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.White)
                .padding(2.dp)
                .clickable {  },
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.history_order_label),
                        fontSize = 10.sp
                    )
                }
                Text(text = order.OrderID, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                    WalletBalance(label = stringResource(id = R.string.wallet_outgoing_balance), value = order.Amount.toNoso(), Color.Red)
                }
            }
            Row(modifier = Modifier.padding(5.dp)) {
                IconButton(
                    onClick = {
                        clipManager.setText(AnnotatedString(order.OrderID))
                        Toast.makeText(context, R.string.general_copytoclip, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .width(26.dp)
                        .height(26.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_content_copy_24),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun WalletBalance(label:String, value:String, color: Color = Color.Black) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, fontSize = 12.sp)
        Text(text = value, fontSize = 12.sp, color = color)
    }
}

@Preview
@Composable
fun previewOrderRow(){
    NosoWalletTheme {
        OrderRow(
            order = OrderObject().apply {
                this.OrderID = "asdlfjkasdjfalsdjfañsdjfñasjdfñasjdñf"
                this.Amount = 1000L
                this.Destination = "S7evenSoftware"
            }
        ){ action, value ->
        }
    }
}