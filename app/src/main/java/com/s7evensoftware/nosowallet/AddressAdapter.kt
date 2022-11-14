package com.s7evensoftware.nosowallet

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.recyclerview.widget.RecyclerView
import com.s7evensoftware.nosowallet.databinding.WalletAddressRowBinding

class AddressAdapter(callback:AddressAdapterListener): RecyclerView.Adapter<AddressAdapter.Address>() {

    private var AddressList:ArrayList<WalletObject>? = null
    private var PendingList:ArrayList<PendingData>? = null
    private var callback:AddressAdapterListener? = null
    var contextTarget:WalletObject? = null

    init {
        this.callback = callback
    }

    fun setAddressList(value: ArrayList<WalletObject>?) {
        AddressList = value
        notifyDataSetChanged()
    }

    fun setPendingList(value: ArrayList<PendingData>?) {
        PendingList = value
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Address {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.wallet_address_row, parent, false)
        return Address(view)
    }

    override fun onBindViewHolder(holder: Address, position: Int) {
        holder.setAddress(AddressList!![position], PendingList!![position])
    }

    override fun getItemCount(): Int {
        if(AddressList != null){
            return AddressList!!.size
        }
        return 0
    }

    fun addNewWallet(newWallet: WalletObject) {
        AddressList?.add(newWallet)
        PendingList?.add(PendingData())
        notifyItemInserted(AddressList!!.indexOf(newWallet))
    }

    fun deleteWalletAt(position: Int){
        AddressList?.removeAt(position)
        PendingList?.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getWallet(position: Int):WalletObject? {
        return AddressList?.get(position)
    }

    fun updateWallet(wallet: WalletObject) {
        AddressList?.let {
            val index = it.indexOf(wallet)
            notifyItemChanged(index)
        }
    }

    inner class Address(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnCreateContextMenuListener {

        var rowbinding = WalletAddressRowBinding.bind(itemView)

        fun setAddress(wallet: WalletObject, pendingData: PendingData){
            rowbinding = WalletAddressRowBinding.bind(itemView)
            itemView.setOnCreateContextMenuListener(this)

            if(wallet.isLocked){
                rowbinding.isLockedIcon.visibility = View.VISIBLE
            }else{
                rowbinding.isLockedIcon.visibility = View.GONE
            }

            if(wallet.Custom?.isBlank() == true || wallet.Custom?.length?:0 < 5){
                rowbinding.walletAddressRowAddress.text = wallet.Hash
            }else{
                rowbinding.walletAddressRowAddress.text = wallet.Custom
            }
            rowbinding.walletAddressRowIncoming.text = mpCoin.Long2Currency(pendingData.Incoming)
            rowbinding.walletAddressRowOutgoing.text = mpCoin.Long2Currency(pendingData.Outgoing)
            rowbinding.walletAddressRowBalance.text = mpCoin.Long2Currency(wallet.Balance)

            // To Copy the Address into the clipboard
            rowbinding.walletAddressRowCopy.tag = wallet.Hash
            rowbinding.walletAddressRowQr.tag = wallet.Hash
            rowbinding.walletAddressRowContainer.tag = wallet.Hash
            rowbinding.walletAddressRowCopy.setOnClickListener(this)
            rowbinding.walletAddressRowQr.setOnClickListener(this)
            rowbinding.walletAddressRowContainer.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            when(v?.id){
                R.id.wallet_address_row_copy -> {
                    callback?.onAddressCopied(v.tag as String)
                }
                R.id.wallet_address_row_qr -> {
                    callback?.onQRGenerationCall(v.tag as String)
                }
                R.id.wallet_address_row_container -> {
                    callback?.onSourceForSendFunds(v.tag as String)
                }
            }
        }

        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            AddressList?.let {
                callback?.setMenuTarget(adapterPosition)
            }

            contextTarget = AddressList?.find { w -> w.Hash == v?.tag }

            menu?.add(0, v!!.id, 0, R.string.menu_action_delete)
            menu?.add(0, v!!.id, 1, R.string.menu_action_customize)
            menu?.add(0, v!!.id, 2, R.string.menu_action_history)
            menu?.add(0, v!!.id, 3, if( contextTarget != null && contextTarget!!.isLocked){ R.string.menu_action_unlock }else{ R.string.menu_action_lock })
            //menu?.add(0, v!!.id, 2, "")
        }
    }

    interface AddressAdapterListener {
        fun setMenuTarget(position:Int)
        fun onAddressCopied(address:String)
        fun onSourceForSendFunds(address: String)
        fun onQRGenerationCall(address: String)
    }
}