package tech.edgerun.hwvault.android

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.cert.X509Certificate

/**
 * StrongBox-backed signer for hardware-rooted approvals.
 *
 * Notes:
 * - Requires API 28+ for setIsStrongBoxBacked.
 * - Falls back to normal Android Keystore only if allowFallback == true.
 */
class StrongBoxSigner(
    private val alias: String = "hwvault_strongbox_signing",
    private val allowFallback: Boolean = false,
) {
    fun ensureKeyExists(): KeyMetadata {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getCertificate(alias) as? X509Certificate
        if (existing != null) {
            return KeyMetadata(alias = alias, attestationChain = listOf(existing))
        }

        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                builder.setIsStrongBoxBacked(true)
            } catch (_: Throwable) {
                if (!allowFallback) {
                    throw IllegalStateException("StrongBox requested but unavailable")
                }
            }
        } else if (!allowFallback) {
            throw IllegalStateException("StrongBox requires Android 9+ (API 28)")
        }

        kpg.initialize(builder.build())
        kpg.generateKeyPair()

        val cert = ks.getCertificate(alias) as? X509Certificate
            ?: throw IllegalStateException("Failed to read generated certificate")
        return KeyMetadata(alias = alias, attestationChain = listOf(cert))
    }

    fun signChallenge(challenge: ByteArray): ByteArray {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val entry = ks.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("Signing key not found: $alias")

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(entry.privateKey)
        signature.update(challenge)
        return signature.sign()
    }

    data class KeyMetadata(
        val alias: String,
        val attestationChain: List<X509Certificate>,
    )
}
