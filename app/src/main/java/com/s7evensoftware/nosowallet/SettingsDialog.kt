package com.s7evensoftware.nosowallet

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.s7evensoftware.nosowallet.databinding.DialogSetupBinding

class SettingsDialog(viewModel:MainViewModel): DialogFragment(), View.OnClickListener {

    private val viewModel = viewModel
    var settingsDialog:View? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return AlertDialog.Builder(requireContext())
            .setView(prepareSettingsView(layoutInflater.inflate(R.layout.dialog_setup, null)))
            .setCancelable(true)
            .create()
    }

    private fun prepareSettingsView(view:View):View{
        val dialogBinding = DialogSetupBinding.bind(view)
        dialogBinding.settingsAddServer.setOnClickListener(this)
        dialogBinding.settingsAddServerDone.setOnClickListener(this)
        dialogBinding.settingsDeleteServer.setOnClickListener(this)
        dialogBinding.settingsServerList.visibility = View.VISIBLE
        dialogBinding.settingsAddServerContainer.visibility = View.GONE

        dialogBinding.settingsAddServerAddressInput.addTextChangedListener {
            viewModel.SettingsAddress.value = it.toString()
        }

        dialogBinding.settingsAddServerPortInput.addTextChangedListener {
            viewModel.SettingsPort.value = it.toString()
        }

        if(viewModel.SettingsAddress.value != null){
            dialogBinding.settingsAddServerAddressInput.post {
                dialogBinding.settingsAddServerAddressInput.setText(viewModel.SettingsAddress.value)
            }
        }

        if(viewModel.SettingsPort.value != null){
            dialogBinding.settingsAddServerPortInput.post {
                dialogBinding.settingsAddServerPortInput.setText(viewModel.SettingsPort.value)
            }
        }

        return view
    }

    companion object {
        const val TAG = "SettingsDialog"
        fun newInstance(viewModel: MainViewModel) = SettingsDialog(viewModel)
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.settings_add_server -> {
                val dialogBinding = DialogSetupBinding.bind(settingsDialog!!)
                dialogBinding.settingsServerList.visibility = View.GONE
                dialogBinding.settingsAddServerContainer.visibility = View.VISIBLE
            }

            R.id.settings_add_server_done -> {
                val dialogBinding = DialogSetupBinding.bind(settingsDialog!!)
                dialogBinding.settingsServerList.visibility = View.VISIBLE
                dialogBinding.settingsAddServerContainer.visibility = View.GONE
            }
        }
    }

}