package com.nosoproject.nosowallet.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosoproject.nosowallet.ui.customcomposable.Button
import com.nosoproject.nosowallet.ui.customcomposable.OutlinedTextField
import com.nosoproject.nosowallet.ui.main.NosoAction


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LockDialog(
    password:String,
    confirmPassword:String,
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
            text = "Lock Address",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color.Black
        )
        Text(
            text = "This will encrypt the private key with a password, use it carefully.",
            fontSize = 12.sp,
            color = Color.Black,
            lineHeight = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { onAction(NosoAction.SetPassword, it) },
            label = { Text(text = "Password") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if(password.length >= 8) Color.Green else Color.Red
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { onAction(NosoAction.SetConfirmPassword, it) },
            label = { Text(text = "Confirm Password") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if(password == confirmPassword && password.isNotEmpty()) Color.Green else Color.Red
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Row(modifier = Modifier.align(Alignment.End)) {
            Button(onClick = { onAction(NosoAction.HiddenDialog, false) }) {
                Text(text = "Cancel", color = Color.Black)
            }
            Spacer(modifier = Modifier.width(5.dp))
            Button(
                onClick = {
                    onAction(NosoAction.LockWallet, password)
                },
                enabled = password.isNotEmpty() && password == confirmPassword
            ) {
                Text(text = "Lock", color = Color.Black)
            }
        }
    }
}