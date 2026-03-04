package tech.edgerun.hwvault.android

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Sends signed approvals to an approval backend.
 */
class StrongBoxApprovalClient(
    private val baseUrl: String,
    private val bearerToken: String,
    private val signer: StrongBoxSigner,
    private val http: OkHttpClient = OkHttpClient(),
) {
    fun submitApproval(payload: ApprovalPayload): Boolean {
        signer.ensureKeyExists()
        val challenge = payload.toCanonicalBytes()
        val sig = signer.signChallenge(challenge)

        val body = JSONObject()
            .put("requestId", payload.requestId)
            .put("status", "approved")
            .put("challenge", payload.toBase64())
            .put("signature", android.util.Base64.encodeToString(sig, android.util.Base64.NO_WRAP))
            .toString()
            .toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/v1/approvals/${payload.requestId}/decision")
            .addHeader("Authorization", "Bearer $bearerToken")
            .post(body)
            .build()

        http.newCall(req).execute().use { resp ->
            return resp.isSuccessful
        }
    }
}
