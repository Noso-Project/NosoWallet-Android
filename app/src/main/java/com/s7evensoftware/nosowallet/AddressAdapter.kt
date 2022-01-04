package com.s7evensoftware.nosowallet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.s7evensoftware.nosowallet.databinding.WalletAddressRowBinding

class AddressAdapter(callback:OnCopyDone): RecyclerView.Adapter<AddressAdapter.Address>() {

    private var AddressList:ArrayList<WalletObject>? = null
    private var PendingList:ArrayList<PendingData>? = null
    private var callback:OnCopyDone? = null

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

    inner class Address(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener {
        fun setAddress(wallet: WalletObject, pendingData: PendingData){
            var rowbinding = WalletAddressRowBinding.bind(itemView)
            rowbinding.walletAddressRowAddress.text = wallet.Hash
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
    }

    interface OnCopyDone {
        fun onAddressCopied(address:String)
        fun onSourceForSendFunds(address: String)
        fun onQRGenerationCall(address: String)
    }
}