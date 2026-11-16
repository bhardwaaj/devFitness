package com.example.devfitness.data

import android.util.Log
import com.example.devfitness.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class UserRole {
    Admin,
    Member;

    companion object {
        fun from(value: String?): UserRole = if (value == "admin") Admin else Member
    }
}

sealed interface AuthResult {
    data class Success(
        val name: String,
        val email: String,
        val role: UserRole,
        val token: String,
    ) : AuthResult

    data class Failure(val reason: AuthFailure) : AuthResult
}

enum class AuthFailure {
    InvalidCredentials,
    EmailTaken,
    Unavailable,
    NotConfigured,
    Inactive,
}

/**
 * Authentication client for the DEV Fitness backend.
 * Call from a background thread. Atlas credentials never enter the Android APK.
 */
class AuthStore {
    fun login(email: String, password: String): AuthResult {
        return request(
            path = "/auth/login",
            body = JSONObject()
                .put("email", email.trim().lowercase())
                .put("password", password),
        )
    }

    fun register(name: String, email: String, password: String): AuthResult {
        return request(
            path = "/auth/register",
            body = JSONObject()
                .put("name", name.trim())
                .put("email", email.trim().lowercase())
                .put("password", password),
        )
    }

    private fun request(path: String, body: JSONObject): AuthResult {
        val baseUrl = BuildConfig.API_BASE_URL.trim().trimEnd('/')
        if (baseUrl.isBlank()) return AuthResult.Failure(AuthFailure.NotConfigured)

        var connection: HttpURLConnection? = null
        return try {
            connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12_000
                readTimeout = 12_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val payload = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (payload.isBlank()) JSONObject() else JSONObject(payload)

            if (status in 200..299) {
                val user = json.getJSONObject("user")
                AuthResult.Success(
                    name = user.optString("name"),
                    email = user.optString("email"),
                    role = UserRole.from(user.optString("role")),
                    token = json.getString("token"),
                )
            } else {
                when (json.optString("error")) {
                    "INVALID_CREDENTIALS" -> AuthResult.Failure(AuthFailure.InvalidCredentials)
                    "EMAIL_TAKEN", "DUPLICATE_RESOURCE" -> AuthResult.Failure(AuthFailure.EmailTaken)
                    "ACCOUNT_INACTIVE" -> AuthResult.Failure(AuthFailure.Inactive)
                    else -> AuthResult.Failure(AuthFailure.Unavailable)
                }
            }
        } catch (error: Throwable) {
            // LinkageError is deliberately caught so an optional platform class can never kill the UI.
            Log.e(TAG, "Authentication API request failed", error)
            AuthResult.Failure(AuthFailure.Unavailable)
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val TAG = "AuthStore"
    }
}
