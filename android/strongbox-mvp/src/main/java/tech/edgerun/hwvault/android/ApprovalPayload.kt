package tech.edgerun.hwvault.android

import java.util.Base64

/**
 * Canonical challenge payload for second-device approvals.
 */
data class ApprovalPayload(
    val requestId: String,
    val action: String,
    val secretId: String,
    val issuedAtUnix: Long,
    val expiresAtUnix: Long,
) {
    fun toCanonicalBytes(): ByteArray {
        val canonical = listOf(
            requestId,
            action,
            secretId,
            issuedAtUnix.toString(),
            expiresAtUnix.toString(),
        ).joinToString("|")
        return canonical.toByteArray(Charsets.UTF_8)
    }

    fun toBase64(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(toCanonicalBytes())
}
