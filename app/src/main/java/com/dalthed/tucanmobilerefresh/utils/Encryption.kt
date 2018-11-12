package com.dalthed.tucanmobilerefresh.utils

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.dalthed.tucanmobilerefresh.TuCanMobileRefresh
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.security.auth.x500.X500Principal

object Encryption {
    private val KEY_ALIAS = "TuCaNMobileRefreshKey"
    val ks = KeyStore.getInstance("AndroidKeyStore")
    init {
        ks.load(null)
    }

    fun generateKey( context: Context) {
        val kpg:KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA,"AndroidKeyStore")
        val kp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .run {
                    setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    build()
                }
            kpg.initialize(parameterSpec)
            kpg.generateKeyPair()

        } else {
            val end = Calendar.getInstance()
            end.add(Calendar.YEAR,10)
            val generatorSpec = KeyPairGeneratorSpec.Builder(context).setAlias(KEY_ALIAS)
                .setSubject(X500Principal("CN=$KEY_ALIAS"))
                .setSerialNumber(BigInteger.valueOf(1))
                .setStartDate(Calendar.getInstance().time)
                .setEndDate(end.time)
                .build()
            kpg.initialize(generatorSpec)
            kpg.generateKeyPair()
        }

    }

    fun keyIsGenerated():Boolean {
        return ks.containsAlias(KEY_ALIAS)
    }

    private fun getPrivateKey(context: Context):PrivateKey{
        if(!keyIsGenerated()){
            generateKey(context)
        }
        return ks.getKey(KEY_ALIAS,null) as PrivateKey
    }

    private fun getPublicKey(context: Context):PublicKey{
        if(!keyIsGenerated()){
            generateKey(context)
        }
        return ks.getCertificate(KEY_ALIAS).publicKey as PublicKey
    }

    fun encrypt(password:String,context: Context):ByteArray {
        val publicKey = getPublicKey(context)
        val input = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        input.init(Cipher.ENCRYPT_MODE, publicKey)
        val outputStream = ByteArrayOutputStream()
        val cipherOutputStream = CipherOutputStream(outputStream,input)
        cipherOutputStream.write(password.toByteArray(Charset.defaultCharset()))
        cipherOutputStream.close()
        return outputStream.toByteArray()
    }

    fun decrypt(cipherBytes:ByteArray,context: Context):String {
        val privateKey = getPrivateKey(context)
        val output = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        output.init(Cipher.DECRYPT_MODE,privateKey)
        val cipherInputStream = CipherInputStream(ByteArrayInputStream(cipherBytes),output)

        val inputString = cipherInputStream.bufferedReader().use { it.readText() }

        return inputString

    }

    fun deleteKey(){
        ks.deleteEntry(KEY_ALIAS)
    }
}