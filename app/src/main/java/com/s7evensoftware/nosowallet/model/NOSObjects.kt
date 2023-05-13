package com.s7evensoftware.nosowallet.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.io.Serializable

class ConcensusResult {
    var LastBlock:Long = 0
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

class PoolData {
    var Address:String = ""
    var Port:Int = 8080
    var MinerID:String = ""
    var NosoAddress:String = ""
    var Connected:Boolean = false
    var Invalid: Boolean = false
    var CurrentBlock:Long = 0
    var TargetHash:String = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
    var TargetDiff:String = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
    var PoolBalance:Long = 0L
    var PoolTilPayment:Int = 30
    var PoolPayStr:String = ""
}

class PoolPayData{
    var Block = 0L
    var Amount = 0L
    var OrderID = ""
}

class SumaryData: RealmObject {
    @PrimaryKey
    var Hash = ""
    var Custom = ""
    var Balance: Long = -1
    var Score: Long = -1
    var LastOP: Long = -1
}

class ServerObject:RealmObject {
    @PrimaryKey var Address:String = "localhost"
    var Port:Int = 8080
    var NosoAddress:String = ""
    var Count:Int = 0
}

class OrderObject: RealmObject {
    @PrimaryKey var OrderID: String = "-1"
    var Destination:String? = null
    var Amount:Long = 0L
    var Timestamp:Long = 0L
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
    var Sender:String? = null // Public Key of who is sending the noso
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
    var isLocked:Boolean = false

    var Incoming:Long = 0L
    var Outgoing:Long = 0L

    fun copy(
        hash:String? = Hash,
        custom:String? = Custom,
        publicKey:String? = PublicKey,
        privateKey:String? = PrivateKey,
        balance:Long = Balance,
        pending:Long = Pending,
        score:Long = Score,
        lastOP:Long = LastOP,
        locked:Boolean = isLocked,
        incoming:Long = Incoming,
        outgoing:Long = Outgoing
    ): WalletObject {
        return WalletObject().apply {
            Hash = hash
            Custom = custom
            PublicKey = publicKey
            PrivateKey = privateKey
            Balance = balance
            Pending = pending
            Score = score
            LastOP = lastOP
            isLocked = locked
            Incoming = incoming
            Outgoing = outgoing
        }
    }
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


