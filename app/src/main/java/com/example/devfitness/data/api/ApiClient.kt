package com.example.devfitness.data.api

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class ApiResult<out T> {
    data class Success<T>(val value: T, val statusCode: Int) : ApiResult<T>()

    data class Failure(
        val statusCode: Int? = null,
        val message: String,
        val errorCode: String? = null,
        val cause: Throwable? = null,
    ) : ApiResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(value), statusCode)
        is Failure -> this
    }
}

/**
 * Small synchronous HTTP client. Invoke its methods from a worker thread.
 *
 * [tokenProvider] is evaluated for every request, allowing callers to rotate or clear a token
 * without rebuilding the client. It should return the raw token, without the "Bearer" prefix.
 */
class ApiClient(
    baseUrl: String,
    private val tokenProvider: () -> String?,
    private val connectTimeoutMillis: Int = 15_000,
    private val readTimeoutMillis: Int = 20_000,
) {
    private val rootUrl = baseUrl.trim().trimEnd('/').also {
        require(it.startsWith("http://") || it.startsWith("https://")) {
            "baseUrl must be an absolute HTTP(S) URL"
        }
    }

    fun get(path: String, query: Map<String, Any?> = emptyMap()): ApiResult<JSONObject> =
        request("GET", path, query = query)

    fun post(path: String, body: JSONObject = JSONObject()): ApiResult<JSONObject> =
        request("POST", path, body)

    fun put(path: String, body: JSONObject = JSONObject()): ApiResult<JSONObject> =
        request("PUT", path, body)

    fun patch(path: String, body: JSONObject = JSONObject()): ApiResult<JSONObject> =
        request("PATCH", path, body)

    fun delete(path: String, body: JSONObject? = null): ApiResult<JSONObject> =
        request("DELETE", path, body)

    fun upload(path: String, bytes: ByteArray, contentType: String): ApiResult<JSONObject> {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(buildUrl(path, emptyMap())).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = connectTimeoutMillis
                readTimeout = 60_000
                useCaches = false
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", contentType)
                setFixedLengthStreamingMode(bytes.size)
                tokenProvider()?.trim()?.takeIf(String::isNotEmpty)?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                }
            }
            connection.outputStream.use { it.write(bytes) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val payload = parsePayload(stream?.bufferedReader()?.use { it.readText() }.orEmpty())
            if (status in 200..299) {
                ApiResult.Success(payload, status)
            } else {
                ApiResult.Failure(
                    statusCode = status,
                    message = payload.nullableString("message", "error", "detail")
                        ?: "Upload failed with HTTP $status",
                )
            }
        } catch (error: Throwable) {
            ApiResult.Failure(message = error.message ?: "Upload failed", cause = error)
        } finally {
            connection?.disconnect()
        }
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        query: Map<String, Any?> = emptyMap(),
    ): ApiResult<JSONObject> {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(buildUrl(path, query)).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = connectTimeoutMillis
                readTimeout = readTimeoutMillis
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                tokenProvider()?.trim()?.takeIf(String::isNotEmpty)?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                }
                if (body != null) doOutput = true
            }

            if (body != null) {
                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val payload = parsePayload(raw)

            if (status in 200..299) {
                ApiResult.Success(payload, status)
            } else {
                ApiResult.Failure(
                    statusCode = status,
                    message = payload.nullableString("message", "error", "detail")
                        ?: "Request failed with HTTP $status",
                    errorCode = payload.nullableString("code", "errorCode", "error_code"),
                )
            }
        } catch (error: IOException) {
            ApiResult.Failure(message = error.message ?: "Network request failed", cause = error)
        } catch (error: JSONException) {
            ApiResult.Failure(message = "Server returned invalid JSON", cause = error)
        } catch (error: RuntimeException) {
            ApiResult.Failure(message = error.message ?: "API request failed", cause = error)
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildUrl(path: String, query: Map<String, Any?>): String {
        val normalizedPath = if (path.startsWith('/')) path else "/$path"
        val encodedQuery = query.entries
            .filter { it.value != null }
            .joinToString("&") { (key, value) ->
                "${encode(key)}=${encode(value.toString())}"
            }
        return "$rootUrl$normalizedPath" + if (encodedQuery.isBlank()) "" else "?$encodedQuery"
    }

    private fun parsePayload(raw: String): JSONObject {
        if (raw.isBlank()) return JSONObject()
        return when (raw.firstOrNull { !it.isWhitespace() }) {
            '[' -> JSONObject().put(ARRAY_WRAPPER, JSONArray(raw))
            '{' -> JSONObject(raw)
            else -> JSONObject().put("data", raw)
        }
    }

    companion object {
        internal const val ARRAY_WRAPPER = "_array"

        fun pathSegment(value: String): String = encode(value)

        private fun encode(value: String): String =
            URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }
}
