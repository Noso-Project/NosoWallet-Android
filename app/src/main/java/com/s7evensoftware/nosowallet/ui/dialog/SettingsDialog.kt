package com.s7evensoftware.nosowallet.ui.dialog

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s7evensoftware.nosowallet.ServerObject
import com.s7evensoftware.nosowallet.ui.main.NosoAction
import com.s7evensoftware.nosowallet.ui.nodes.NodeRow
import com.s7evensoftware.nosowallet.ui.theme.NosoWalletTheme
import com.s7evensoftware.nosowallet.ui.theme.walletColor
import com.s7evensoftware.nosowallet.util.Log

const val NOSO_POP_JOB_ID = 7020090

@Composable
fun SettingsDialog(
    serverList:List<ServerObject>,
    popServiceEnabled:Boolean,
    context: Context = LocalContext.current,
    onAction: (NosoAction, Any) -> Unit
) {
    var selectedServer by remember { mutableStateOf(ServerObject()) }

    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(10.dp))
            .background(color = Color.White)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp)
            .verticalScroll(state = rememberScrollState())
    ) {
        Text(
            text = "Settings",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(5.dp))
        Row (
            verticalAlignment = Alignment.CenterVertically,
            //modifier = Modifier.padding(horizontal = 10.dp)
        ){
            Text(
                text = "Enable PoP Service",
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                enabled = true,
                checked = popServiceEnabled,
                onCheckedChange = { onAction(NosoAction.SwitchPoP, !popServiceEnabled) }
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        LazyColumn(
            modifier = Modifier
                .background(color = walletColor, shape = RoundedCornerShape(10.dp))
                .padding(10.dp)
                .heightIn(min = 200.dp, max = 200.dp)
        ){
            if(serverList.isEmpty()){
                item(){
                    Text(
                        text = "Node list is empty",
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(CenterHorizontally)
                            .fillMaxWidth()
                    )
                }
            }
            items(serverList){ server ->
                NodeRow(address = server.Address, port = server.Port, isSelected = selectedServer.Address == server.Address){
                    selectedServer = server
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.End, 
            modifier = Modifier
                .align(alignment = Alignment.End)
                .padding(top = 5.dp)
                .padding(bottom = 20.dp)
        ){
            Text(text = "Node list options", color = Color.LightGray)
            Spacer(modifier = Modifier.width(5.dp))
            IconButton(
                onClick = { onAction(NosoAction.AddNodeDialog, true) },
                modifier = Modifier
                    .background(color = Color.Gray, shape = RoundedCornerShape(5.dp))
                    .width(24.dp)
                    .height(24.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(5.dp))
            IconButton(
                enabled = selectedServer.Address != "localhost",
                onClick = { onAction(NosoAction.DeleteNode, selectedServer) },
                modifier = Modifier
                    .background(color = Color.Gray, shape = RoundedCornerShape(5.dp))
                    .width(24.dp)
                    .height(24.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
@Preview
fun previewSettings(){
    NosoWalletTheme {
        SettingsDialog(serverList = listOf(), popServiceEnabled = true){ a,b ->

        }
    }
}