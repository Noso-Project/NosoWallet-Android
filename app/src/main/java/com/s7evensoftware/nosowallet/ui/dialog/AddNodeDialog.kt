package com.s7evensoftware.nosowallet.ui.dialog

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s7evensoftware.nosowallet.model.ServerObject
import com.s7evensoftware.nosowallet.ui.customcomposable.Button
import com.s7evensoftware.nosowallet.ui.customcomposable.OutlinedTextField
import com.s7evensoftware.nosowallet.ui.main.NosoAction

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddNodeDialog(
    context: Context = LocalContext.current,
    onAction: (NosoAction, Any) -> Unit
) {
    var newNodeAddress by remember { mutableStateOf("") }
    var newNodePort by remember { mutableStateOf("8080") }

    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(10.dp))
            .background(color = Color.White)
            .padding(20.dp)
            .verticalScroll(state = rememberScrollState())
    ) {
        Text(
            text = "Add temporal node",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color.Black
        )
        Text(
            text = "Nodes are sync from the mainnet and those added manually may get removed after the next consensus",
            fontSize = 12.sp,
            color = Color.Black,
            lineHeight = 12.sp
        )
        Spacer(modifier = Modifier.height(5.dp))
        Column() {
            OutlinedTextField(
                value = newNodeAddress, onValueChange = {
                    if(it.matches("(\\d+\\.?)*".toRegex())) newNodeAddress = it
                },
                label = { Text(text = "Address:") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                value = newNodePort, onValueChange = {
                    if(it.toIntOrNull() != null) newNodePort = it
                },
                label = { Text(text = "Port:") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
            )
        }
        Row(modifier = Modifier.align(alignment = Alignment.End)) {
            Button(onClick = { onAction(NosoAction.SettingsDialog, true) }) {
                Text(text = "Cancel", color = Color.Black)
            }
            Spacer(modifier = Modifier.width(5.dp))
            Button(
                onClick = { onAction(NosoAction.AddNode, ServerObject().apply { Address = newNodeAddress;Port = newNodePort.toInt() }) },
                enabled = newNodeAddress.isNotEmpty() && newNodePort.isNotEmpty()
            ) {
                Text(text = "Add", color = Color.Black)
            }
        }
    }
}