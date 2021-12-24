package com.s7evensoftware.nosowallet

import android.util.Log
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmQuery
import io.realm.RealmResults

object DBManager {
    private val realmName = "NosoDB"
    private val config = RealmConfiguration.Builder()
        .allowQueriesOnUiThread(true)
        .allowWritesOnUiThread(true)
        .name(realmName).build()

    init {}

    fun insertDefaultNodes(){
        val realmDB = Realm.getInstance(config)

        if(realmDB.where(ServerObject::class.java).count().toInt() == 0){
            val node1 = ServerObject()
            val node2 = ServerObject()
            val node3 = ServerObject()
            val node4 = ServerObject()
            val node5 = ServerObject()
            val node6 = ServerObject()

            node1.Address = "192.210.226.118"
            node2.Address = "45.146.252.103"
            node3.Address = "194.156.88.117"
            node4.Address = "107.172.5.8"
            node5.Address = "185.239.239.184"
            node6.Address = "109.230.238.240"

            realmDB.executeTransaction {
                it.insert(node1)
                it.insert(node2)
                it.insert(node3)
                it.insert(node4)
                it.insert(node5)
                it.insert(node6)
            }
            Log.e("DBManager","Creating Seed Nodes - OK")
        }else{
            Log.e("DBManager","Seed Nodes Loaded - OK")
        }
        realmDB.close()
    }

    fun insertNewServer(IP:String, Port:Int){
        val realmDB = Realm.getInstance(config)
        val newServer = ServerObject()
        newServer.Address = IP
        newServer.Port = Port
        newServer.isDefault = realmDB.where(ServerObject::class.java).findAll().count() == 0

        realmDB.executeTransaction {
            it.insert(newServer)
        }
    }

    fun getServers(): RealmResults<ServerObject>? {
        val realmDB = Realm.getInstance(config)
        val res = realmDB.where(ServerObject::class.java).findAll()
        return res
    }

    fun deleteServer(IP:String){
        val realmDB = Realm.getInstance(config)
        val found = realmDB.where(ServerObject::class.java)
            .equalTo("Address",IP)
            .findFirst()

        realmDB.executeTransaction {
            found?.deleteFromRealm()
        }
    }

    fun setDefaultServer(server: ServerObject) {
        val realmDB = Realm.getInstance(config)
        val preDefault = realmDB.where(ServerObject::class.java)
            .equalTo("isDefault",true)
            .findFirst()

        val newDefault = realmDB.where(ServerObject::class.java)
            .equalTo("Address",server.Address)
            .findFirst()

        realmDB.executeTransaction {
            preDefault?.isDefault = false
            newDefault?.isDefault = true
        }
    }


}