package com.s7evensoftware.nosowallet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.s7evensoftware.nosowallet.databinding.WalletAddressRowBinding

class AddressAdapter(callback:OnCopyDone): RecyclerView.Adapter<AddressAdapter.Address>() {

    private var AddressList:ArrayList<WalletObject>? = null
    private var callback:OnCopyDone? = null

    init {
        this.callback = callback
    }

    fun setAddressList(value: ArrayList<WalletObject>?) {
        AddressList = value
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Address {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.wallet_address_row, parent, false)
        return Address(view)
    }

    override fun onBindViewHolder(holder: Address, position: Int) {
        holder.setAddress(AddressList!!.get(position))
    }

    override fun getItemCount(): Int {
        if(AddressList != null){
            return AddressList!!.size
        }
        return 0
    }

    fun addNewWallet(newWallet: WalletObject) {
        AddressList?.add(newWallet)
        notifyItemInserted(AddressList!!.indexOf(newWallet))
    }

    inner class Address(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener {
        fun setAddress(wallet: WalletObject){
            var rowbinding = WalletAddressRowBinding.bind(itemView)
            rowbinding.walletAddressRowAddress.text = wallet.Hash
            rowbinding.walletAddressRowBalance.text = mpCoin.Long2Currency(wallet.Balance)

            // To Copy the Address into the clipboard
            rowbinding.walletAddressRowCopy.tag = wallet.Hash
            rowbinding.walletAddressRowCopy.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            when(v?.id){
                R.id.wallet_address_row_copy -> {
                    callback?.onAddressCopied(v.tag as String)
                }
            }
        }
    }

    interface OnCopyDone {
        fun onAddressCopied(address:String)
    }
}