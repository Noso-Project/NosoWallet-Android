package com.nosoproject.nosowallet.ui.dialog

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.Bottom
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.nosoproject.nosowallet.R
import com.nosoproject.nosowallet.ui.main.NosoAction

@Composable
fun ImportDialog(
    onAction: (NosoAction, Any) -> Unit
) {
    val importWalletQrTask = rememberLauncherForActivityResult(ScanContract()){ result ->
        result.contents?.let { content ->
            onAction(NosoAction.AddQRWallet, content)
        }
    }

    val importWalletFileTask = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK){
            it.data?.let { intentData -> onAction(NosoAction.AddFileWallets, intentData) }
        }
    }

    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(10.dp))
            .background(color = Color.White)
            .padding(20.dp)
            .verticalScroll(state = rememberScrollState())
    ) {
        Text(
            text = "Select an Import method",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.align(CenterHorizontally),
            verticalAlignment = Bottom
        ) {
            Column(
                horizontalAlignment = CenterHorizontally,
                modifier = Modifier.clickable { importWalletQrTask.launch(ScanOptions()) }
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_qricon_24), contentDescription = null)
                Text(text = "QR Code")
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                horizontalAlignment = CenterHorizontally,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        type = "*/*"
                    }
                    importWalletFileTask.launch(intent)
                }
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_fileicon_24), contentDescription = null)
                Text(text = "File .Pkw")
            }
        }
    }
}