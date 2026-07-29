package com.bookmarkapp.queuemark.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Local-only "guest" session: lets the user enter the app with no Firebase
// account at all (anonymous sign-in is a network call, so it can't gate an
// offline-first app). The flag survives restarts; a Firebase account is
// created lazily in the background once the network allows.
interface GuestSessionStore {
    var isGuest: Boolean
}

@Singleton
class PrefsGuestSessionStore @Inject constructor(
    @ApplicationContext context: Context
) : GuestSessionStore {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    override var isGuest: Boolean
        get() = prefs.getBoolean(KEY_GUEST, false)
        set(value) = prefs.edit { putBoolean(KEY_GUEST, value) }

    private companion object {
        const val KEY_GUEST = "is_guest"
    }
}
