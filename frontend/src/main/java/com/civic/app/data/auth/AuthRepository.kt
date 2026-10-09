package com.civic.app.data.auth

import com.civic.app.data.cloud.AuthApi
import com.civic.app.data.cloud.CloudConfig
import com.civic.app.data.cloud.CloudException
import com.civic.app.data.cloud.ProfileApi
import com.civic.app.data.cloud.ProfileDto
import com.civic.app.data.cloud.ProfilePatch
import com.civic.app.data.cloud.StorageApi
import com.civic.app.data.cloud.TokenResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * The one place the rest of the app asks "who is signed in?".
 *
 * It works in two modes and the screens do not need to care which:
 * - **no backend configured** (`local.properties` has no `supabase.*`) — the user picks a display name once and
 *   gets a device profile. Everything stays on the phone.
 * - **backend configured** — sign up / sign in with email and password; reports and comments carry the real
 *   account, and [com.civic.app.data.sync.SyncManager] pushes them to Postgres.
 *
 * A device profile is not thrown away when the user signs in: its local reports are re-attributed to the cloud
 * account so nothing written before signing in is lost.
 */
class AuthRepository(
    private val store: SessionStore,
    private val authApi: AuthApi,
    private val profileApi: ProfileApi,
    private val storageApi: StorageApi,
) {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /** Guards token refresh so several parallel requests cannot each burn the refresh token. */
    private val refreshLock = Mutex()

    /** True when accounts and the shared feed are possible at all, i.e. the build has Supabase credentials. */
    val isCloudAvailable: Boolean get() = CloudConfig.isConfigured

    val installId: String get() = store.installId

    val account: Account? get() = (_state.value as? AuthState.Active)?.account

    val currentUserId: String? get() = account?.id

    /** Call once at startup. Restores the stored identity without blocking on the network. */
    fun restore() {
        val stored = store.readAccount()
        _state.value = when {
            stored == null -> AuthState.NeedsOnboarding
            stored.isCloud -> AuthState.Active(stored, store.readSession())
            else -> AuthState.Active(stored)
        }
    }

    // ---------- device profile ----------

    /** Creates (or renames) the on-device identity. Works with no network and no backend. */
    fun useDeviceProfile(displayName: String) {
        val name = displayName.trim().ifEmpty { "Neighbour" }
        val existing = store.readAccount()
        val account = Account(
            id = existing?.takeIf { !it.isCloud }?.id ?: "local:${store.installId}",
            username = name.toUsername(),
            displayName = name,
            avatar = existing?.avatar,
            bio = existing?.bio,
            isCloud = false,
        )
        store.writeAccount(account)
        _state.value = AuthState.Active(account)
    }

    // ---------- cloud accounts ----------

    suspend fun signUp(email: String, password: String, displayName: String): SignUpOutcome {
        requireCloud()
        val response = authApi.signUp(email, password, displayName)
        // With "Confirm email" on, Supabase returns the user but no tokens: there is nothing to sign in with yet.
        if (response.accessToken.isBlank()) {
            return SignUpOutcome.NeedsEmailConfirmation(email.trim())
        }
        adoptSession(response, fallbackDisplayName = displayName, email = email)
        return SignUpOutcome.SignedIn
    }

    suspend fun signIn(email: String, password: String) {
        requireCloud()
        val response = authApi.signIn(email, password)
        adoptSession(response, fallbackDisplayName = email.substringBefore('@'), email = email)
    }

    /**
     * Drops the cloud session. The identity falls back to a device profile keeping the same display name, so the
     * app stays usable and signed-out reports still have an author.
     */
    suspend fun signOut() {
        val previous = store.readAccount()
        (_state.value as? AuthState.Active)?.session?.let { authApi.signOut(it.accessToken) }
        store.clearAll()
        if (previous == null) {
            _state.value = AuthState.NeedsOnboarding
            return
        }
        // The name, photo and bio are kept: signing out should not wipe the identity shown on this phone's own
        // reports. The install id survives clearAll(), so the device profile keeps the id it had before.
        val device = previous.copy(
            id = "local:${store.installId}",
            email = null,
            isCloud = false,
        )
        store.writeAccount(device)
        _state.value = AuthState.Active(device)
    }

    /** Forgets this phone's identity entirely (used by "remove profile" in settings). */
    fun forgetAccount() {
        store.clearAll()
        _state.value = AuthState.NeedsOnboarding
    }

    suspend fun requestPasswordReset(email: String) {
        requireCloud()
        authApi.requestPasswordReset(email)
    }

    // ---------- profile editing ----------

    /**
     * Saves profile edits locally first so the UI updates even offline, then mirrors them to Postgres when
     * signed in. A failed mirror throws after the local save, so the user keeps their change and sees why.
     */
    suspend fun updateProfile(displayName: String? = null, bio: String? = null, avatarFile: File? = null) {
        val current = account ?: return
        var avatarValue = current.avatar
        if (avatarFile != null) avatarValue = avatarFile.absolutePath

        val updated = current.copy(
            displayName = displayName?.trim()?.ifEmpty { current.displayName } ?: current.displayName,
            bio = bio?.trim() ?: current.bio,
            avatar = avatarValue,
        )
        store.writeAccount(updated)
        _state.value = AuthState.Active(updated, (_state.value as? AuthState.Active)?.session)

        if (!updated.isCloud) return
        val remoteAvatar = avatarFile?.let { storageApi.uploadPhoto(updated.id, it) }
        val saved = profileApi.update(
            updated.id,
            ProfilePatch(displayName = updated.displayName, bio = updated.bio, avatarUrl = remoteAvatar),
        )
        if (saved != null) {
            val merged = updated.copy(
                username = saved.username,
                displayName = saved.displayName,
                avatar = saved.avatarUrl ?: updated.avatar,
                bio = saved.bio,
            )
            store.writeAccount(merged)
            _state.value = AuthState.Active(merged, (_state.value as? AuthState.Active)?.session)
        }
    }

    // ---------- tokens ----------

    /**
     * The access token for an outgoing request, refreshed first if it is about to expire.
     * Returns null when there is no cloud session, which makes the caller fall back to the anon key.
     */
    suspend fun accessToken(): String? {
        val active = _state.value as? AuthState.Active ?: return null
        val session = active.session ?: return null
        if (!session.isExpiring()) return session.accessToken
        return refreshLock.withLock {
            // Another coroutine may have refreshed while this one waited.
            val latest = (_state.value as? AuthState.Active)?.session ?: return@withLock null
            if (!latest.isExpiring()) return@withLock latest.accessToken
            try {
                val refreshed = authApi.refresh(latest.refreshToken)
                val new = refreshed.toSession(latest.userId) ?: return@withLock null
                store.writeSession(new)
                _state.value = AuthState.Active(active.account, new)
                new.accessToken
            } catch (e: CloudException) {
                // The refresh token is dead (revoked, or the project was reset): fall back to a device profile.
                if (e.status in 400..499) {
                    store.clearSession()
                    _state.value = AuthState.Active(active.account.copy(isCloud = false))
                    store.writeAccount(active.account.copy(isCloud = false))
                }
                null
            }
        }
    }

    // ---------- internals ----------

    private suspend fun adoptSession(response: TokenResponse, fallbackDisplayName: String, email: String) {
        val userId = response.user?.id ?: throw CloudException(0, "The server did not return an account id.")
        val session = response.toSession(userId)
            ?: throw CloudException(0, "The server did not return a session.")

        val remote = runCatching { profileApi.fetch(userId) }.getOrNull()
            ?: runCatching {
                // The trigger normally creates this row; this covers a project where schema.sql ran late.
                profileApi.createIfMissing(
                    ProfileDto(
                        id = userId,
                        username = fallbackDisplayName.toUsername(),
                        displayName = fallbackDisplayName.trim().ifEmpty { "Neighbour" },
                    ),
                )
            }.getOrNull()

        val account = Account(
            id = userId,
            username = remote?.username ?: fallbackDisplayName.toUsername(),
            displayName = remote?.displayName ?: fallbackDisplayName.trim().ifEmpty { "Neighbour" },
            avatar = remote?.avatarUrl,
            bio = remote?.bio,
            email = email.trim(),
            isCloud = true,
        )
        store.writeAccount(account)
        store.writeSession(session)
        _state.value = AuthState.Active(account, session)
    }

    private fun requireCloud() {
        if (!isCloudAvailable) {
            throw CloudException(0, "This build has no server configured. See docs/BACKEND_SETUP.md.")
        }
    }
}

private fun TokenResponse.toSession(userId: String): CloudSession? {
    if (accessToken.isBlank() || refreshToken.isBlank()) return null
    val lifetime = if (expiresIn > 0) expiresIn else 3600
    return CloudSession(
        userId = userId,
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAtMillis = System.currentTimeMillis() + lifetime * 1000,
    )
}
