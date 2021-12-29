package com.s7evensoftware.nosowallet

import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator
import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECKeyGenerationParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.DSADigestSigner
import org.bouncycastle.crypto.signers.ECDSASigner
import java.math.BigInteger
import java.security.SecureRandom

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

        fun GetSigner(): DSADigestSigner {
            return DSADigestSigner(ECDSASigner(),SHA1Digest())
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

            Publickey = (askp.public as ECPublicKeyParameters).q.getEncoded(false)
            Privatekey = (askp.private as ECPrivateKeyParameters).d.toByteArray()

            val keyPair = KeyPair()
            keyPair.PublicKey = String(org.bouncycastle.util.encoders.Base64.encode(Publickey))
            keyPair.PrivateKey = String(org.bouncycastle.util.encoders.Base64.encode(Privatekey))
            return keyPair
        }

        fun SignMessage(
            message: ByteArray,
            PrivateKey: ByteArray,
            AKeyType: KeyType): ByteArray
        {
            var LSigner:DSADigestSigner
            var LRecreatedPrivKey:ECPrivateKeyParameters
            var LCurve:X9ECParameters
            var domain:ECDomainParameters

            LCurve = GetCurve(AKeyType)
            domain = GetDomain(LCurve)
            LRecreatedPrivKey = ECPrivateKeyParameters(BigInteger(1, PrivateKey), domain)

            LSigner = GetSigner()
            LSigner.init(true, LRecreatedPrivKey)
            LSigner.update(message, 0, message.size)
            val signature = LSigner.generateSignature()

            return signature
        }

        fun VerifySignature(
            signature:ByteArray,
            message:ByteArray,
            publicKey: ByteArray,
            keyType: KeyType
        ):Boolean {
            var LSigner:DSADigestSigner
            var LRecreatedPublicKey: ECPublicKeyParameters
            var LCurve: X9ECParameters
            var Domain: ECDomainParameters

            LCurve = GetCurve(keyType)
            Domain = GetDomain(LCurve)
            LRecreatedPublicKey = ECPublicKeyParameters(LCurve.curve.decodePoint(publicKey), Domain)
            LSigner = GetSigner()

            LSigner.init(false, LRecreatedPublicKey)
            LSigner.update(message, 0, message.size)

            return LSigner.verifySignature(signature)
        }
    }
}