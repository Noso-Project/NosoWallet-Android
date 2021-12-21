package com.s7evensoftware.nosowallet

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable
import java.math.BigInteger

class NOSObjects {

}

open class ServerObject:RealmObject() {
    @PrimaryKey var address:String = "localhost"
    var port:Int = 8080
    var default:Boolean = false
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

class OrderData : Serializable {
    var Block:Int = -1
    var OrderID:String? = null
    var OrderLines:Int = -1
    var OrderType: String? = ""
    var TimeStamp:Int = -1
    var Reference:String? = null
    var TrxLine:Int = -1
    var Sender:String? = null // La clave publica de quien envia
    var Address:String? = null
    var Receiver:String? = null
    var AmmountFee:Int = -1
    var AmmountTrf:Int = -1
    var Signature:String? = null
    var TrfrID:String? = null
}

class WalletObject : Serializable {
    var Hash:String? = null
    var Custom:String? = null
    var PublicKey:String? = null
    var PrivateKey:String? = null
    var Balance:Int = 0
    var Pending:Int = 0
    var Score:Int = 0
    var LasOP:Int = 0
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


