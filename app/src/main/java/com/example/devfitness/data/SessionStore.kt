package com.example.devfitness.data

import android.content.Context

class SessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "devfitness_session",
        Context.MODE_PRIVATE,
    )

    fun load(): AuthResult.Success? {
        val token = preferences.getString(KEY_TOKEN, null).orEmpty()
        if (token.isBlank()) return null
        return AuthResult.Success(
            name = preferences.getString(KEY_NAME, "").orEmpty(),
            email = preferences.getString(KEY_EMAIL, "").orEmpty(),
            role = UserRole.from(preferences.getString(KEY_ROLE, "member")),
            token = token,
        )
    }

    fun save(session: AuthResult.Success) {
        preferences.edit()
            .putString(KEY_NAME, session.name)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_ROLE, session.role.name.lowercase())
            .putString(KEY_TOKEN, session.token)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_ROLE = "role"
        const val KEY_TOKEN = "token"
    }
}
