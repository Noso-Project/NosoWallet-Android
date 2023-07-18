package com.nosoproject.nosowallet.ui.menu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nosoproject.nosowallet.R
import com.nosoproject.nosowallet.ui.main.NosoAction

@Composable
fun Menu(balance:String, onAction: (NosoAction, Any) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        MenuOption(
            title = stringResource(id = R.string.wallet_actions_create),
            icon = R.drawable.ic_baseline_create_new_24,
            onAction = { onAction(NosoAction.CreateAddress, "") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        MenuOption(
            title = stringResource(id = R.string.wallet_actions_import),
            icon = R.drawable.ic_import_icon,
            onAction = { onAction(NosoAction.ImportDialog, true) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        MenuOption(
            title = stringResource(id = R.string.wallet_actions_export),
            icon = R.drawable.ic_export_icon,
            onAction = { onAction(NosoAction.ExportWallet, true) }
        )
        Spacer(modifier = Modifier.weight(1f))
        BalanceView(balance = balance)
    }
}

//@Preview
//@Composable
//fun menuPreview(){
//    NosoWalletTheme {
//        Menu(balance = "2.33815000"){
//
//        }
//    }
//}

