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
    val realmDB = Realm.getInstance(config)

    init {}

    fun insertNewServer(IP:String, Port:Int){
        val newServer = ServerObject()
        newServer.address = IP
        newServer.port = Port
        newServer.default = true

        Log.e("DBManager","New server default status: "+newServer.default)
        realmDB.executeTransaction {
            it.insert(newServer)
            Log.e("DBManager","In default status: "+newServer.default)
        }
        Log.e("DBManager","stat: "+realmDB.where(ServerObject::class.java).findAll().first()!!.default)
    }

    fun getServers(): RealmResults<ServerObject>? {
        return realmDB.where(ServerObject::class.java).findAll()
    }

    fun deleteServer(IP:String){
        val found = realmDB.where(ServerObject::class.java)
            .equalTo("address",IP)
            .findFirst()

        realmDB.executeTransactionAsync {
            found?.deleteFromRealm()
        }
    }







}