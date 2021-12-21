package com.s7evensoftware.nosowallet

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.s7evensoftware.nosowallet.databinding.ConnectionServerRowBinding
import io.realm.RealmResults

class ServerAdapter(callback:onServerSelected): RecyclerView.Adapter<ServerAdapter.Server>(){

    private var ServerList: RealmResults<ServerObject>? = null
    private var callback:onServerSelected? = null

    init {
        this.callback = callback
    }

    fun setServers(servers: RealmResults<ServerObject>?) {
        Log.e("Adapter","Agregando lista de: "+servers!!.size+" servidores")
        ServerList = servers
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

        private var rowBinding: ConnectionServerRowBinding = ConnectionServerRowBinding.bind(itemView)

        fun setServer(server:ServerObject){
            rowBinding.connectionRowAddress.text = server.address
            rowBinding.connectionRowPort.text = server.port.toString()
            Log.e("Adapter","Server: "+server.address+" - Default: "+server.default)
            if(server.default){
                rowBinding.connectionRowDefault.visibility = View.VISIBLE
            }else{
                rowBinding.connectionRowDefault.visibility = View.GONE
            }
        }

        override fun onClick(v: View) {
            callback?.selectServer(v)
        }
    }

    public interface onServerSelected {
        fun selectServer(v:View)
    }
}