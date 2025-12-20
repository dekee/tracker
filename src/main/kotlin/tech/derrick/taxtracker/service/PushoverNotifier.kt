package tech.derrick.taxtracker.service

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PushoverNotifier {
    private val log = LoggerFactory.getLogger(PushoverNotifier::class.java)

    private val enabled: Boolean =
        (System.getenv("NOTIFY_PUSHOVER_ENABLED") ?: "false").trim().equals("true", ignoreCase = true)

    private val token: String = (System.getenv("NOTIFY_PUSHOVER_TOKEN") ?: "").trim()
    private val user: String = (System.getenv("NOTIFY_PUSHOVER_USER") ?: "").trim()

    private val client = OkHttpClient()

    fun notifyStatusChange(parcelId: String, ownerName: String, previousStatus: String, newStatus: String) {
        if (!enabled) return

        if (token.isBlank() || user.isBlank()) {
            log.warn("Pushover enabled but NOTIFY_PUSHOVER_TOKEN / NOTIFY_PUSHOVER_USER not set; skipping notification")
            return
        }

        val title = "Tax status changed"
        val message = "$parcelId ($ownerName): '$previousStatus' -> '$newStatus'"

        val body = FormBody.Builder()
            .add("token", token)
            .add("user", user)
            .add("title", title)
            .add("message", message)
            .build()

        val req = Request.Builder()
            .url("https://api.pushover.net/1/messages.json")
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val respBody = resp.body?.string()
                log.warn("Pushover notify failed: status={} body={}", resp.code, respBody)
            } else {
                log.info("✅ Pushover notification sent for parcelId={}", parcelId)
            }
        }
    }
}


