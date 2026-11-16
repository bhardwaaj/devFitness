package com.example.devfitness.data.api

import android.os.Handler
import android.os.Looper
import com.example.devfitness.data.model.ChatMessage
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class RealtimeChatClient(
    baseUrl: String,
    token: String,
    private val conversationId: String,
    private val onMessage: (ChatMessage) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val socket: Socket = IO.socket(
        baseUrl.trimEnd('/'),
        IO.Options.builder().setAuth(mapOf("token" to token)).build(),
    )

    fun connect() {
        socket.on(Socket.EVENT_CONNECT) {
            socket.emit("chat:join", JSONObject().put("conversationId", conversationId))
        }
        socket.on("chat:message") { arguments ->
            val payload = arguments.firstOrNull() as? JSONObject ?: return@on
            val message = runCatching { ModelJson.chatMessage(payload) }.getOrNull() ?: return@on
            if (message.conversationId == conversationId) {
                mainHandler.post { onMessage(message) }
            }
        }
        socket.on(Socket.EVENT_CONNECT_ERROR) {
            mainHandler.post { onError("Live chat disconnected. Messages can still be sent.") }
        }
        socket.connect()
    }

    fun disconnect() {
        socket.off()
        socket.disconnect()
    }
}

class RealtimeUpdatesClient(
    baseUrl: String,
    token: String,
    private val onNotification: (title: String, body: String) -> Unit,
    private val onChat: (sender: String, message: String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val socket = IO.socket(
        baseUrl.trimEnd('/'),
        IO.Options.builder().setAuth(mapOf("token" to token)).build(),
    )

    fun connect() {
        socket.on("notification") { arguments ->
            val value = arguments.firstOrNull() as? JSONObject ?: return@on
            mainHandler.post {
                onNotification(value.optString("title", "DEV Fitness"), value.optString("body"))
            }
        }
        socket.on("chat:notification") { arguments ->
            val value = arguments.firstOrNull() as? JSONObject ?: return@on
            mainHandler.post {
                onChat(value.optString("senderName", "New message"), value.optString("text"))
            }
        }
        socket.connect()
    }

    fun disconnect() {
        socket.off()
        socket.disconnect()
    }
}
