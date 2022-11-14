package com.s7evensoftware.nosowallet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.s7evensoftware.nosowallet.databinding.ConnectionServerRowBinding
import io.realm.RealmResults

class ServerAdapter(callback:OnServerSelected): RecyclerView.Adapter<ServerAdapter.Server>(){

    private var ServerList: RealmResults<ServerObject>? = null
    private var ServerLocalCopy: ArrayList<ServerObject>? = null
    private var callback:OnServerSelected? = null

    init {
        this.callback = callback
    }

    fun setServers(servers: RealmResults<ServerObject>?) {
        ServerList = servers
        ServerLocalCopy = ArrayList()
        ServerList?.forEach {
            val s = ServerObject()
            s.Address = it.Address
            s.Port = it.Port
            ServerLocalCopy?.add(s)
        }
    }

    fun setServers(servers: List<ServerObject>) {
        ServerLocalCopy = ArrayList(servers)
    }

    fun deleteServer(server:ServerObject){
        indexOf(server).let {
            ServerLocalCopy?.removeAt(it)
            DBManager.deleteServer(server.Address)
            notifyItemRemoved(it)
        }
    }

    fun getServers(): ArrayList<ServerObject>? {
        return ServerLocalCopy
    }

    fun indexOf(server:ServerObject):Int{
        return ServerList?.indexOf(server)?:0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Server {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.connection_server_row, parent, false)
        return Server(view)
    }

    override fun onBindViewHolder(holder: Server, position: Int) {
        holder.setServer(ServerList!!.get(position)!!)
    }

    override fun getItemCount(): Int {
        if(ServerList == null){
            return 0
        }
        return ServerList!!.size
    }

    inner class Server(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener {

        fun setServer(server:ServerObject){
            var rowBinding = ConnectionServerRowBinding.bind(itemView)

            rowBinding.connectionRowContainer.tag = server
            rowBinding.connectionRowContainer.setOnClickListener(this)
            rowBinding.connectionRowAddress.text = server.Address
            rowBinding.connectionRowPort.text = server.Port.toString()
            if(server.isDefault){
                rowBinding.connectionRowDefault.visibility = View.VISIBLE
            }else{
                rowBinding.connectionRowDefault.visibility = View.GONE
            }
        }

        override fun onClick(v: View) {
            callback?.onSelectServer(v)
        }
    }

    interface OnServerSelected {
        fun onSelectServer(v: View)
    }
}