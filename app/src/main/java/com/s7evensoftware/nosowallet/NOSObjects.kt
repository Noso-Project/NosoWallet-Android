package com.s7evensoftware.nosowallet

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable
import java.math.BigInteger

class NOSObjects {

}

class ConcensusResult {
    var LastBlock:Long = 0
    var LasPendingCount:Long = 0
    var LastBranch:String = ""
    var Address:String = ""
    var Port:Int = 8080
}


class ConcensusData {
    var Value:String = ""
    var Count:Int = 0
}

class NodeInfo {
    var Address:String = ""
    var Port:Int = 8080
    var Connections:Long = 0L
    var Lastblock:Long = 0L
    var Pendings:Long = 0L
    var Delta:Long = 0L
    var Branch:String = ""
    var Version:String = ""
    var UTCTime:Long = 0
}

open class SumaryData:RealmObject() {
    @PrimaryKey var Hash = ""
    var Custom = ""
    var Balance:Long = -1
    var Score:Long = -1
    var LastOP:Long = -1
}

class PendingData {
    var Incoming:Long = 0
    var Outgoing:Long = 0
}

open class ServerObject:RealmObject() {
    @PrimaryKey var Address:String = "localhost"
    var Port:Int = 8080
    var isDefault:Boolean = false
}

class Options : Serializable {
    var language:Int = 0
    var Port:Int = 8080
    var GetNodes:Boolean = false
    var PoolInfo:String? = null
    var Wallet:String = "wallet.pkw"
    var AutoServer:Boolean = false
    var AutoConnect:Boolean = true
    var Auto_Updater:Boolean = false
    var JustUpdated:Boolean = false
    var VersionPage:String = "https://nosocoin.com"
    var UsePool = false
}

class PendingInfo {
    var TO_Type:String = ""
    var TO_Sender:String = ""
    var TO_Receiver:String = ""
    var TO_Amount:Long = 0
    var TO_Fee:Long = 0
}

class OrderData : Serializable {
    var Block:Int = -1
    var OrderID:String? = null
    var OrderLines:Int = -1
    var OrderType: String? = ""
    var TimeStamp:Long = -1L
    var Reference:String? = null
    var TrxLine:Int = -1
    var Sender:String? = null // La clave publica de quien envia
    var Address:String? = null
    var Receiver:String? = null
    var AmountFee:Long = -1
    var AmountTrf:Long = -1
    var Signature:String? = null
    var TrfrID:String? = null
}

class WalletObject : Serializable {
    var Hash:String? = null
    var Custom:String? = null
    var PublicKey:String? = null
    var PrivateKey:String? = null
    var Balance:Long = 0
    var Pending:Long = 0
    var Score:Long = 0
    var LastOP:Long = 0
}

class KeyPair : Serializable {
    var PublicKey:String? = null
    var PrivateKey:String? = null
}

enum class KeyType {
    SECP256K1,
    SECP384R1,
    SECP521R1,
    SECT283K1
}

class DivResult {
    var Cociente:String? = null
    var Residuo:String? = null
}


