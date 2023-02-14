package com.s7evensoftware.nosowallet.model

import com.s7evensoftware.nosowallet.OrderObject
import com.s7evensoftware.nosowallet.ServerObject
import com.s7evensoftware.nosowallet.SumaryData
import com.s7evensoftware.nosowallet.util.Log
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults

class DBManager {
    companion object {

        suspend fun updateNodes(dynamicList:List<ServerObject>, realmDB:Realm){
            realmDB.write {
                delete(ServerObject::class)
                for (node in dynamicList) {
                    copyToRealm(node)
                }
            }
        }

        suspend fun clearNonWorkingNodes(realmDB: Realm){
            val deprecatedNodesList = listOf(
                "107.172.193.176", "107.172.5.8",
                "107.175.194.151", "192.3.73.184",
                "185.239.239.184", "45.146.252.103",
                "194.156.88.117", "172.245.52.208",
                "192.210.226.118", "23.94.21.83"
            )

            realmDB.write {
                deprecatedNodesList.forEach { target ->
                    query<ServerObject>("Address = $0", target).first().find()?.let {
                        delete(it)
                    }
                }
            }
        }

        suspend fun insertDefaultNodes(realmDB: Realm){
            // Erase expired or no-longer working nodes
            clearNonWorkingNodes(realmDB)
            if(realmDB.query<ServerObject>().find().count() < 7){
                val node1 = ServerObject().apply { Address = "109.230.238.240" }
                val node2 = ServerObject().apply { Address = "198.144.190.194" }
                val node3 = ServerObject().apply { Address = "149.57.226.244" }
                val node4 = ServerObject().apply { Address = "107.172.193.176" }
                val node5 = ServerObject().apply { Address = "66.151.117.247" }
                val node6 = ServerObject().apply { Address = "149.57.229.81" }
                val node7 = ServerObject().apply { Address = "107.175.24.151" }
                val node8 = ServerObject().apply { Address = "159.196.1.198" }
                val node9 = ServerObject().apply { Address = "101.100.138.125" }

                realmDB.write {
                    copyToRealm(node1)
                    copyToRealm(node2)
                    copyToRealm(node3)
                    copyToRealm(node4)
                    copyToRealm(node5)
                    copyToRealm(node6)
                    copyToRealm(node7)
                }
                Log.e("DBManager","Creating Seed Nodes - OK")
            }else{
                Log.e("DBManager","Seed Nodes Loaded - OK")
            }
        }

        suspend fun insertOrder(newOrder: OrderObject, realmDB: Realm) {
            realmDB.write {
                copyToRealm(newOrder)
            }
        }

        fun getOrders(realmDB: Realm): ArrayList<OrderObject> {
            val list = realmDB.query<OrderObject>().find()
            val result = ArrayList<OrderObject>()
            list.forEach { order ->
                val o = OrderObject().apply {
                    OrderID = order.OrderID
                    Destination = order.Destination
                    Amount = order.Amount
                }
                result.add(o)
            }
            return result
        }


        fun getSummarySize(realmDB: Realm):Int {
            return realmDB.query<SumaryData>().find().count()
        }

        fun getAddressBalance(address:String, realmDB: Realm):Long {
            realmDB.query<SumaryData>("Hash = $0", address).first().find()?.let {
                val result = it.Balance
                return result
            }
            return 0L
        }

        fun getCustom(address: String, realmDB: Realm):String {
            realmDB.query<SumaryData>("Hash = $0", address).first().find()?.let {
                val result = it.Custom
                return result
            }
            return ""
        }

        fun getWallet(address:String, realmDB: Realm): SumaryData? {
            realmDB.query<SumaryData>("Hash = $0", address).first().find()?.let {
                return it
            }
            return null
        }

        fun isAliasUsed(custom_name:String, realmDB: Realm):Boolean {
            return realmDB.query<SumaryData>("Custom = $0", custom_name).first().find() != null
        }

        suspend fun addSummaryFromList(addressSummary: ArrayList<SumaryData>, realmDB: Realm) {
            realmDB.write {
                for(summary in addressSummary){
                    copyToRealm(summary)
                }
            }
        }

        suspend fun clearSummary(realmDB: Realm){
            realmDB.write {
                delete(SumaryData::class)
            }
        }

        suspend fun insertNewServer(IP:String, Port:Int, realmDB: Realm){
            val newServer = ServerObject()
            newServer.Address = IP
            newServer.Port = Port

            realmDB.write {
                copyToRealm(newServer)
            }
        }

        fun getServers(realmDB: Realm): RealmResults<ServerObject> {
            return realmDB.query<ServerObject>().find()
        }

        suspend fun deleteServer(IP:String, realmDB: Realm){
            realmDB.write {
                realmDB.query<ServerObject>("Address = $0",IP).first().find()?.let {
                    findLatest(it)?.let { node ->
                        delete(node)
                    }
                }
            }
        }
    }
}