package com.s7evensoftware.nosowallet

import android.util.Base64
import android.util.Log
import org.bouncycastle.asn1.ocsp.Signature
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.crypto.*
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECKeyGenerationParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.DSAKCalculator
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.jcajce.provider.digest.SHA512
import org.bouncycastle.jcajce.util.MessageDigestUtils
import org.bouncycastle.util.encoders.Base64Encoder
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey


class mpSignerUtils {
    companion object {

    }
}

class SignerUtils {
    companion object {
        val SigningAlgorithm = "SHA-1withECDSA"
        var FSecureRandom: SecureRandom? = null
        var securerandom:SecureRandom? = null
            get() = GetSecureRandom()

        fun GetCurveFromKeyType( AKeyType: KeyType): X9ECParameters{
            return CustomNamedCurves.getByName(AKeyType.name)
        }

        fun GetSecureRandom(): SecureRandom {
            if(FSecureRandom != null){
                return FSecureRandom!!
            }else{
                FSecureRandom = SecureRandom()
                return FSecureRandom!!
            }

        }

        fun GetSigner(): java.security.Signature? {
            val sign_o = java.security.Signature.getInstance(SigningAlgorithm)
            return sign_o
        }

        fun GetCurve(keyType: KeyType): X9ECParameters {
            return GetCurveFromKeyType((keyType))
        }

        fun GetDomain(curve: X9ECParameters): ECDomainParameters {
            return ECDomainParameters(curve.curve, curve.g, curve.n, curve.h, curve.seed)
        }


        fun GenerateECKeyPair(AKeyType: KeyType): KeyPair {
            var LCurve:X9ECParameters
            var domain:ECDomainParameters
            var KeyPairGeneratorInstance:AsymmetricCipherKeyPairGenerator
            var askp: AsymmetricCipherKeyPair
            var Publickey: ByteArray
            var Privatekey: ByteArray

            LCurve = GetCurve(AKeyType)
            domain = GetDomain(LCurve)
            KeyPairGeneratorInstance = ECKeyPairGenerator()
            KeyPairGeneratorInstance.init(ECKeyGenerationParameters(domain, securerandom))
            askp = KeyPairGeneratorInstance.generateKeyPair()
            Publickey = (askp.public as ECPublicKeyParameters).q.getEncoded(true)
            Privatekey = (askp.private as ECPrivateKeyParameters).d.toByteArray()

            val keyPair = KeyPair()
            keyPair.PublicKey = String(java.util.Base64.getEncoder().encode(Publickey))
            keyPair.PrivateKey = String(java.util.Base64.getEncoder().encode(Privatekey))
            return keyPair
        }

        /*
        fun SignMessage(
            message: ByteArray,
            PrivateKey: ByteArray,
            AKeyType: KeyType): ByteArray
        {
            var LSigner:Signer
            var LRecreatedPrivKey:ECPrivateKeyParameters
            var LCurve:X9ECParameters
            var domain:ECDomainParameters

            LCurve = GetCurve(AKeyType)
            domain = GetDomain(LCurve)
            LRecreatedPrivKey = ECPrivateKeyParameters(BigInteger(1, PrivateKey), domain)
            //LSigner = GetSigner()


        }


        fun VerifySignature(
            signature: ByteArray,
            message: ByteArray,
            PublicKey: ByteArray,
            AKeyType: KeyType):Boolean
        {

        }

        fun ByteToString(Value: ByteArray): String {

        }

        fun StrToByte(Value:String): ByteArray {

        }

         */

    }



}