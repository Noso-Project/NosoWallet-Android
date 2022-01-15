package com.s7evensoftware.nosowallet

import io.realm.FieldAttribute
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.annotations.PrimaryKey

object DBManager {
    private val realmName = "NosoDB"
    private val config = RealmConfiguration.Builder()
        .schemaVersion(1)
        .migration { realm, oldVersion, newVersion ->
            val schema = realm.schema
            if(oldVersion == 0L){
                val newModel = schema.create("OrderObject")
                newModel.addField("OrderID", String::class.java, FieldAttribute.PRIMARY_KEY)
                newModel.addField("Destination", String::class.java)
                newModel.addField("Amount", Long::class.java)
            }
            Log.e("DBManager","Updating DB from version: $oldVersion")
        }
        .allowQueriesOnUiThread(true)
        .allowWritesOnUiThread(true)
        .name(realmName).build()

    init {}

    fun insertDefaultNodes(){
        val realmDB = Realm.getInstance(config)

        if(realmDB.where(ServerObject::class.java).count().toInt() != 7){
            val node1 = ServerObject()
            val node2 = ServerObject()
            val node3 = ServerObject()
            val node4 = ServerObject()
            val node5 = ServerObject()
            val node6 = ServerObject()
            val node7 = ServerObject()

            node1.Address = "192.210.226.118"
            node2.Address = "45.146.252.103"
            node3.Address = "194.156.88.117"
            node4.Address = "107.172.5.8"
            node5.Address = "185.239.239.184"
            node6.Address = "109.230.238.240"
            node7.Address = "23.94.21.83"

            realmDB.executeTransaction {
                // Clear List
                realmDB.delete(ServerObject::class.java)
                it.insert(node1)
                it.insert(node2)
                it.insert(node3)
                it.insert(node4)
                it.insert(node5)
                it.insert(node6)
                it.insert(node7)
            }
            Log.e("DBManager","Creating Seed Nodes - OK")
        }else{
            Log.e("DBManager","Seed Nodes Loaded - OK")
        }
        realmDB.close()
    }

    fun insertOrder(newOrder:OrderObject) {
        val realmDB = Realm.getInstance(config)
        realmDB.executeTransaction {
            it.insert(newOrder)
        }
        realmDB.close()
    }

    fun getOrders(): ArrayList<OrderObject> {
        val realmDB = Realm.getInstance(config)
        val list = realmDB.where(OrderObject::class.java).findAll()

        val result = ArrayList<OrderObject>()
        list.forEach { order ->
            val o = OrderObject()
            o.OrderID = order.OrderID
            o.Destination = order.Destination
            o.Amount = order.Amount
            result.add(o)
        }
        realmDB.close()
        return result
    }


    fun getSummarySize():Int {
        val realmDB = Realm.getInstance(config)
        val size = realmDB.where(SumaryData::class.java).count().toInt()
        realmDB.close()
        return size
    }

    fun getAddressBalance(address:String):Long {
        val realmDB = Realm.getInstance(config)
        realmDB.where(SumaryData::class.java).equalTo("Hash",address).findFirst()?.let {
            val result = it.Balance
            realmDB.close()
            return result
        }
        return 0L
    }

    fun getCustom(address: String):String {
        val realmDB = Realm.getInstance(config)
        realmDB.where(SumaryData::class.java).equalTo("Hash",address).findFirst()?.let {
            val result = it.Custom
            realmDB.close()
            return result
        }
        return ""
    }

    fun getWallet(address:String):SumaryData? {
        val realmDB = Realm.getInstance(config)
        realmDB.where(SumaryData::class.java).equalTo("Hash",address).findFirst()?.let {
            return it
        }
        return null
    }

    fun isAliasUsed(custom_name:String):Boolean {
        val realmDB = Realm.getInstance(config)
        realmDB.where(SumaryData::class.java).findAll()?.let {
            for(sd in it){
                if(sd.Custom == custom_name){
                    return true
                }
            }
        }
        return false
    }

    fun addSummaryFromList(addressSummary: ArrayList<SumaryData>) {
        val realmDB = Realm.getInstance(config)
        realmDB.executeTransaction {
            for(summary in addressSummary){
                it.insert(summary)
            }
        }
        realmDB.close()
    }

    fun clearSummary(){
        val realmDB = Realm.getInstance(config)
        realmDB.executeTransaction {
            it.delete(SumaryData::class.java)
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
        realmDB.close()
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
        realmDB.close()
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
        realmDB.close()
    }


}