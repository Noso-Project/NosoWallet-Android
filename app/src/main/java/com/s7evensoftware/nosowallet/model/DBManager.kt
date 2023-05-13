package com.s7evensoftware.nosowallet.model

import com.s7evensoftware.nosowallet.util.Log
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults

class DBManager {
    companion object {

        val seedsList = listOf(
            "47.87.181.190", "47.87.178.205", "66.151.117.247", "47.87.180.219",
            "47.87.137.96", "192.3.85.196", "192.3.254.186", "198.46.218.125",
            "20.199.50.27", "63.227.69.162", "81.22.38.101"
        )

        suspend fun updateNodes(dynamicList:List<ServerObject>, realmDB:Realm){
            realmDB.write {
                delete(ServerObject::class)
                for (node in dynamicList) {
                    copyToRealm(node)
                }
            }
        }

        private suspend fun clearNonWorkingNodes(realmDB: Realm){
            val deprecatedNodesList = listOf(
                "107.172.193.176", "107.172.5.8",
                "107.175.194.151", "192.3.73.184",
                "185.239.239.184", "45.146.252.103",
                "194.156.88.117", "172.245.52.208",
                "192.210.226.118", "23.94.21.83",
                "109.230.238.240", "198.144.190.194",
                "149.57.226.244", "149.57.229.81",
                "159.196.1.198", "101.100.138.125"
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
                val newNodeList = seedsList.map { nodeAddress ->
                    ServerObject().apply {
                        Address = nodeAddress
                    }
                }

                realmDB.write {
                    newNodeList.forEach {
                        copyToRealm(it)
                    }
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
                    Timestamp = order.Timestamp
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

        fun getSummaryByAlias(custom_name:String, realmDB: Realm): SumaryData? {
            return realmDB.query<SumaryData>("Custom = $0", custom_name).first().find()
        }

        fun isAliasUsed(custom_name:String, realmDB: Realm): Boolean {
            return getSummaryByAlias(custom_name = custom_name, realmDB = realmDB) != null
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