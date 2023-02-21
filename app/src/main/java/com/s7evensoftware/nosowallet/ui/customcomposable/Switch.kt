package com.s7evensoftware.nosowallet.ui.customcomposable

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.s7evensoftware.nosowallet.ui.theme.success
import com.s7evensoftware.nosowallet.ui.theme.walletColor
import com.s7evensoftware.nosowallet.ui.theme.walletError

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SwitchColors = SwitchDefaults.colors(
        disabledCheckedTrackColor = walletColor.copy(0.8f),
        disabledUncheckedThumbColor = Color.DarkGray,
        uncheckedTrackColor = walletError,
        checkedTrackColor = success,
        uncheckedThumbColor = Color.White,
        checkedThumbColor = Color.White
    )
) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = thumbContent,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = colors
    )
}

//@Preview
//@Composable
//fun previewSwitch(){
//    NosoWalletTheme {
//        Switch(
//            enabled = true,
//            checked = false,
//            onCheckedChange = { }
//        )
//    }
//}