package com.s7evensoftware.nosowallet.ui.dialog

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.s7evensoftware.nosowallet.WalletObject
import com.s7evensoftware.nosowallet.model.QR_BITMAP_SIZE
import com.s7evensoftware.nosowallet.ui.main.NosoAction

@Composable
fun QRDialog(
    wallet:WalletObject,
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
            text = "QR Code",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color.Black
        )
        Text(
            text = wallet.Hash?:"Select a valid address",
            fontSize = 12.sp,
            color = Color.Black,
            lineHeight = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        val bits = QRCodeWriter().encode(wallet.Hash?:"", BarcodeFormat.QR_CODE, QR_BITMAP_SIZE, QR_BITMAP_SIZE)
        val QRbitmap = Bitmap.createBitmap(QR_BITMAP_SIZE, QR_BITMAP_SIZE, Bitmap.Config.RGB_565).also {
            for ( x in 0 until QR_BITMAP_SIZE) {
                for ( y in 0 until QR_BITMAP_SIZE){
                    it.setPixel(x,y, if(bits[x,y]) Color.Black.toArgb() else Color.White.toArgb())
                }
            }
        }
        Image(
            bitmap = QRbitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.align(CenterHorizontally)
        )
    }
}