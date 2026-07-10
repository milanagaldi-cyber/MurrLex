package com.lexaprograms.polishcards

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class MurrLexServerSession(
    val serverUrl: String,
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAtMillis: Long,
    val username: String,
    val email: String
) {
    val isConfigured: Boolean get() = serverUrl.isNotBlank()
    val isAuthenticated: Boolean get() = isConfigured && accessToken.isNotBlank() && refreshToken.isNotBlank()
}

data class MurrLexServerResult(
    val session: MurrLexServerSession? = null,
    val error: String = ""
)

object MurrLexServerClient {
    suspend fun login(serverUrl: String, login: String, password: String): MurrLexServerResult =
        authenticate(serverUrl, "/api/auth/login", JSONObject().put("login", login).put("password", password))

    suspend fun register(serverUrl: String, username: String, email: String, password: String): MurrLexServerResult =
        authenticate(
            serverUrl,
            "/api/auth/register",
            JSONObject().put("username", username).put("email", email).put("password", password)
        )

    suspend fun refresh(session: MurrLexServerSession): MurrLexServerResult = withContext(Dispatchers.IO) {
        if (!session.isConfigured || session.refreshToken.isBlank()) return@withContext MurrLexServerResult(error = "Server session is missing")
        val response = postJson(session.serverUrl, "/api/auth/refresh", JSONObject().put("refreshToken", session.refreshToken), "")
        parseSession(session.serverUrl, response, session.refreshToken, session.username, session.email)
    }

    suspend fun logout(session: MurrLexServerSession) = withContext(Dispatchers.IO) {
        if (session.isConfigured && session.refreshToken.isNotBlank()) {
            postJson(session.serverUrl, "/api/auth/logout", JSONObject().put("refreshToken", session.refreshToken), "")
        }
    }

    private suspend fun authenticate(serverUrl: String, path: String, body: JSONObject): MurrLexServerResult = withContext(Dispatchers.IO) {
        if (serverUrl.trim().isBlank()) return@withContext MurrLexServerResult(error = "Enter the MurrLex server URL first")
        val response = postJson(serverUrl, path, body, "")
        parseSession(serverUrl, response, "", "", "")
    }

    private fun parseSession(
        serverUrl: String,
        response: Pair<Int, String>,
        previousRefreshToken: String,
        previousUsername: String,
        previousEmail: String
    ): MurrLexServerResult {
        val (status, body) = response
        val json = runCatching { JSONObject(body) }.getOrNull()
        if (status !in 200..299 || json == null) {
            return MurrLexServerResult(error = json?.optString("error").orEmpty().ifBlank { "MurrLex server request failed" })
        }
        val accessToken = json.optString("accessToken").trim()
        val refreshToken = json.optString("refreshToken").trim().ifBlank { previousRefreshToken }
        if (accessToken.isBlank() || refreshToken.isBlank()) return MurrLexServerResult(error = "MurrLex server returned an invalid session")
        val user = json.optJSONObject("user")
        return MurrLexServerResult(
            session = MurrLexServerSession(
                serverUrl = serverUrl.trim().trimEnd('/'),
                accessToken = accessToken,
                refreshToken = refreshToken,
                accessExpiresAtMillis = System.currentTimeMillis() + json.optLong("expiresInSeconds", 900L) * 1000L,
                username = user?.optString("username")?.trim().orEmpty().ifBlank { previousUsername },
                email = user?.optString("email")?.trim().orEmpty().ifBlank { previousEmail }
            )
        )
    }

    private fun postJson(serverUrl: String, path: String, body: JSONObject, bearerToken: String): Pair<Int, String> {
        val connection = (URL(serverUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "MurrLex/${BuildConfig.VERSION_NAME}")
            if (bearerToken.isNotBlank()) setRequestProperty("Authorization", "Bearer $bearerToken")
        }
        return try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            status to text
        } catch (_: Exception) {
            0 to ""
        } finally {
            connection.disconnect()
        }
    }
}
