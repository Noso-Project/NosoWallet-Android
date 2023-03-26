package com.s7evensoftware.nosowallet.ui.customcomposable

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.Black, errorBorderColor = Color.Red),
    softwareKeyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current,
) {
    androidx.compose.material3.OutlinedTextField(
        value=value,
        onValueChange=onValueChange,
        modifier=modifier,
        enabled=enabled,
        readOnly=readOnly,
        textStyle=textStyle,
        label=label,
        placeholder=placeholder,
        leadingIcon=leadingIcon,
        trailingIcon=trailingIcon,
        isError=isError,
        visualTransformation=visualTransformation,
        keyboardOptions=keyboardOptions,
        keyboardActions = KeyboardActions(onDone = {
            softwareKeyboardController?.hide()
        }),
        singleLine=singleLine,
        maxLines=maxLines,
        interactionSource=interactionSource,
        colors=colors
    )
}