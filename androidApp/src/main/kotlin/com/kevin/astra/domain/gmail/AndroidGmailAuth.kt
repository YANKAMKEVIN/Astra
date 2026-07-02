package com.kevin.astra.domain.gmail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
private val AUTH_ENDPOINT = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
private val TOKEN_ENDPOINT = Uri.parse("https://oauth2.googleapis.com/token")

/**
 * Android Gmail OAuth via AppAuth (authorization-code + PKCE, no backend). Persists the [AuthState]
 * — including the refresh token — in [EncryptedSharedPreferences], and implements
 * [GmailTokenProvider] so the shared Gmail layer can obtain a fresh access token transparently.
 *
 * The interactive consent step must be launched from an Activity: call [authorizationIntent] to get
 * the Intent, start it with an ActivityResultLauncher, then feed the result to
 * [handleAuthorizationResponse].
 */
class AndroidGmailAuthenticator(
    context: Context,
    private val clientId: String,
) : GmailTokenProvider {

    private val appContext = context.applicationContext
    private val authService = AuthorizationService(appContext)
    private val serviceConfig = AuthorizationServiceConfiguration(AUTH_ENDPOINT, TOKEN_ENDPOINT)

    // Google native clients redirect to the reversed client id scheme.
    private val redirectUri: Uri = Uri.parse(
        "com.googleusercontent.apps." +
            clientId.removeSuffix(".apps.googleusercontent.com") +
            ":/oauth2redirect",
    )

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "astra_gmail_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private var authState: AuthState = loadState()

    val isAuthorized: Boolean get() = authState.isAuthorized

    /** Intent that opens the Google consent screen; launch it via an ActivityResultLauncher. */
    fun authorizationIntent(): Intent {
        val request = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri,
        )
            .setScope(GMAIL_SCOPE)
            // access_type=offline + prompt=consent guarantees a refresh token is issued.
            .setAdditionalParameters(mapOf("access_type" to "offline", "prompt" to "consent"))
            .build()
        return authService.getAuthorizationRequestIntent(request)
    }

    /** Processes the redirect Intent and exchanges the authorization code for tokens. */
    suspend fun handleAuthorizationResponse(data: Intent) {
        val response = AuthorizationResponse.fromIntent(data)
        val exception = AuthorizationException.fromIntent(data)
        authState.update(response, exception)
        if (response == null) {
            throw exception ?: IllegalStateException("Gmail authorization was cancelled or failed.")
        }
        val tokenResponse = performTokenRequest(response)
        authState.update(tokenResponse, null)
        persistState()
    }

    /** [GmailTokenProvider] — returns a valid access token, refreshing it if expired. */
    override suspend fun accessToken(): String = suspendCancellableCoroutine { cont ->
        authState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
            if (accessToken != null) {
                persistState()
                cont.resume(accessToken)
            } else {
                cont.resumeWithException(ex ?: IllegalStateException("No Gmail access token available."))
            }
        }
    }

    fun signOut() {
        authState = AuthState()
        prefs.edit().remove(KEY_STATE).apply()
    }

    fun dispose() = authService.dispose()

    private suspend fun performTokenRequest(response: AuthorizationResponse): TokenResponse =
        suspendCancellableCoroutine { cont ->
            authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, ex ->
                if (tokenResponse != null) {
                    cont.resume(tokenResponse)
                } else {
                    cont.resumeWithException(ex ?: IllegalStateException("Gmail token exchange failed."))
                }
            }
        }

    private fun persistState() {
        prefs.edit().putString(KEY_STATE, authState.jsonSerializeString()).apply()
    }

    private fun loadState(): AuthState {
        val json = prefs.getString(KEY_STATE, null) ?: return AuthState()
        return runCatching { AuthState.jsonDeserialize(json) }.getOrDefault(AuthState())
    }

    private companion object {
        const val KEY_STATE = "auth_state"
    }
}

// ── Singleton wiring (mirrors the other initializeAndroidXxx services) ────────

private var gmailAuthenticatorInstance: AndroidGmailAuthenticator? = null

/** Called from the Android app entry point with the client id from BuildConfig. */
fun initializeAndroidGmailAuth(context: Context, clientId: String) {
    if (clientId.isBlank()) return
    if (gmailAuthenticatorInstance == null) {
        gmailAuthenticatorInstance = AndroidGmailAuthenticator(context.applicationContext, clientId)
    }
}

fun androidGmailAuthenticatorOrNull(): AndroidGmailAuthenticator? = gmailAuthenticatorInstance

/**
 * Bridge so the shared UI (Phase C) can request the interactive sign-in that only the Activity can
 * launch. The Activity sets [onRequestSignIn]; the UI invokes it.
 */
object GmailSignInBridge {
    var onRequestSignIn: (() -> Unit)? = null
}

/**
 * Android implementation of the shared [GmailController]. Published via [GmailIntegration] at
 * startup so common code can query connection state and trigger sign-in without touching AppAuth.
 */
class AndroidGmailController(
    private val clientIdConfigured: Boolean,
) : GmailController {
    override val isSupported: Boolean
        get() = clientIdConfigured && androidGmailAuthenticatorOrNull() != null

    override fun isConnected(): Boolean = androidGmailAuthenticatorOrNull()?.isAuthorized == true

    override fun connect() {
        GmailSignInBridge.onRequestSignIn?.invoke()
    }

    override fun disconnect() {
        androidGmailAuthenticatorOrNull()?.signOut()
    }

    override fun tokenProvider(): GmailTokenProvider? = androidGmailAuthenticatorOrNull()
}
