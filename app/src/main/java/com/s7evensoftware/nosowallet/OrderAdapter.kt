package com.s7evensoftware.nosowallet

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.s7evensoftware.nosowallet.databinding.OrderRecordRowBinding
import io.realm.RealmResults

class OrderAdapter(callback:OrderAdapterListener): RecyclerView.Adapter<OrderAdapter.Order>() {

    private var OrderList:RealmResults<OrderObject>? = null
    private var callback:OrderAdapterListener? = null

    init {
        this.callback = callback
    }

    fun setOrderList(value: RealmResults<OrderObject>?) {
        OrderList = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Order {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.wallet_address_row, parent, false)
        return Order(view)
    }

    override fun onBindViewHolder(holder: Order, position: Int) {
        OrderList?.let { list ->
            list[position]?.let {
                holder.setOrder(it)
            }
        }
    }

    override fun getItemCount(): Int {
        OrderList?.let {
            return it.size
        }
        return 0
    }

    fun addNewOrder(newOrder: OrderObject) {
        OrderList?.add(newOrder)
        notifyItemInserted(OrderList!!.indexOf(newOrder))
    }

    fun deleteOrderAt(position: Int){
        OrderList?.removeAt(position)
        OrderList?.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getOrder(position: Int):OrderObject? {
        return OrderList?.get(position)
    }

    inner class Order(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnCreateContextMenuListener {

        var rowbinding = OrderRecordRowBinding.bind(itemView)

        fun setOrder(order: OrderObject){
            rowbinding = OrderRecordRowBinding.bind(itemView)
            itemView.setOnCreateContextMenuListener(this)

            rowbinding.historyOrderId.text = order.OrderID
            rowbinding.historyOrderDestination.text = order.Destination
            rowbinding.historyOrderAmount.text = mpCoin.Long2Currency(order.Amount)

            rowbinding.historyOrderCopy.tag = order.OrderID
            rowbinding.historyOrderCopy.tag = order.OrderID

            rowbinding.historyOrderCopy.setOnClickListener(this)
            rowbinding.historyOrderQr.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            when(v?.id){
                R.id.wallet_address_row_copy -> {
                    callback?.onOrderCopied(v.tag as String)
                }
                R.id.wallet_address_row_qr -> {
                    callback?.onOrderQRGenerationCall(v.tag as String)
                }
            }
        }

        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            OrderList?.let {
                callback?.setOrderTarget(adapterPosition)
            }

            menu?.add(0, v!!.id, 0, R.string.menu_action_delete)
            menu?.add(0, v!!.id, 1, R.string.menu_action_customize)
            menu?.add(0, v!!.id, 2, R.string.menu_action_history)
            //menu?.add(0, v!!.id, 2, "")
        }
    }

    interface OrderAdapterListener {
        fun onOrderCopied(address:String)
        fun onOrderQRGenerationCall(address: String)
        fun setOrderTarget(position: Int)
    }
}