@file:OptIn(ExperimentalForeignApi::class)

package com.kevin.astra.domain.gmail

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
private const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"

private const val KEY_REFRESH = "astra_gmail_refresh"
private const val KEY_ACCESS = "astra_gmail_access"
private const val KEY_EXPIRY = "astra_gmail_expiry"

@Serializable
private data class GoogleTokenResponse(
    val access_token: String = "",
    val refresh_token: String? = null,
    val expires_in: Long = 0,
)

/**
 * iOS Gmail OAuth using the native [ASWebAuthenticationSession] (no third-party SDK) plus a Ktor
 * token exchange. Implements [GmailTokenProvider]; refreshes the access token as needed.
 *
 * NOTE: tokens are stored in NSUserDefaults for now. Production should move the refresh token to
 * the Keychain — flagged intentionally.
 */
class IosGmailAuthenticator(
    private val clientId: String,
    private val httpClient: HttpClient,
) : GmailTokenProvider {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val redirectScheme = "com.googleusercontent.apps." +
        clientId.removeSuffix(".apps.googleusercontent.com")
    private val redirectUri = "$redirectScheme:/oauth2redirect"

    fun hasRefreshToken(): Boolean = defaults.stringForKey(KEY_REFRESH) != null

    fun signOut() {
        defaults.removeObjectForKey(KEY_REFRESH)
        defaults.removeObjectForKey(KEY_ACCESS)
        defaults.removeObjectForKey(KEY_EXPIRY)
    }

    /** Launches the interactive consent screen and exchanges the code for tokens. */
    suspend fun authorize() {
        val pkce = Pkce.generate()
        // Build via NSURLComponents so query values (scope, redirect_uri) are percent-encoded.
        val components = NSURLComponents(string = AUTH_ENDPOINT)
        components.queryItems = listOf(
            NSURLQueryItem(name = "client_id", value = clientId),
            NSURLQueryItem(name = "redirect_uri", value = redirectUri),
            NSURLQueryItem(name = "response_type", value = "code"),
            NSURLQueryItem(name = "scope", value = GMAIL_SCOPE),
            NSURLQueryItem(name = "code_challenge", value = pkce.challenge),
            NSURLQueryItem(name = "code_challenge_method", value = pkce.method),
            NSURLQueryItem(name = "access_type", value = "offline"),
            NSURLQueryItem(name = "prompt", value = "consent"),
        )
        val authUrl = components.URL ?: throw IllegalStateException("Could not build Gmail auth URL.")

        val callbackUrl = presentAuthSession(authUrl)
        val code = queryValue(callbackUrl, "code")
            ?: throw IllegalStateException("Gmail authorization returned no code.")

        val response = exchangeToken(
            Parameters.build {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri)
                append("client_id", clientId)
                append("code_verifier", pkce.verifier)
            },
        )
        persist(response)
    }

    /** [GmailTokenProvider] — returns a valid access token, refreshing when expired. */
    override suspend fun accessToken(): String {
        val cached = defaults.stringForKey(KEY_ACCESS)
        val expiry = defaults.doubleForKey(KEY_EXPIRY)
        if (cached != null && NSDate().timeIntervalSince1970 < expiry - 60.0) {
            return cached
        }
        val refresh = defaults.stringForKey(KEY_REFRESH)
            ?: throw IllegalStateException("Gmail is not connected.")
        val response = exchangeToken(
            Parameters.build {
                append("grant_type", "refresh_token")
                append("refresh_token", refresh)
                append("client_id", clientId)
            },
        )
        persist(response)
        return response.access_token
    }

    private suspend fun exchangeToken(params: Parameters): GoogleTokenResponse =
        httpClient.submitForm(url = TOKEN_ENDPOINT, formParameters = params).body()

    private fun persist(response: GoogleTokenResponse) {
        if (response.access_token.isNotEmpty()) {
            defaults.setObject(response.access_token, KEY_ACCESS)
            defaults.setDouble(NSDate().timeIntervalSince1970 + response.expires_in.toDouble(), KEY_EXPIRY)
        }
        // A refresh token is only returned on the first consent; keep the existing one otherwise.
        response.refresh_token?.let { defaults.setObject(it, KEY_REFRESH) }
    }

    private suspend fun presentAuthSession(authUrl: NSURL): NSURL =
        suspendCancellableCoroutine { cont ->
            val session = ASWebAuthenticationSession(
                uRL = authUrl,
                callbackURLScheme = redirectScheme,
            ) { callbackURL, error ->
                when {
                    callbackURL != null -> cont.resume(callbackURL)
                    else -> cont.resumeWithException(
                        IllegalStateException(error?.localizedDescription ?: "Gmail sign-in was cancelled."),
                    )
                }
            }
            session.presentationContextProvider = PresentationContextProvider()
            session.prefersEphemeralWebBrowserSession = false
            if (!session.start()) {
                cont.resumeWithException(IllegalStateException("Could not start Gmail sign-in session."))
            }
            cont.invokeOnCancellation { session.cancel() }
        }

    private fun queryValue(url: NSURL, name: String): String? {
        val components = NSURLComponents(uRL = url, resolvingAgainstBaseURL = false)
        @Suppress("UNCHECKED_CAST")
        val items = components.queryItems as? List<NSURLQueryItem> ?: return null
        return items.firstOrNull { it.name == name }?.value
    }
}

private class PresentationContextProvider :
    NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(
        session: ASWebAuthenticationSession,
    ): ASPresentationAnchor = UIApplication.sharedApplication.keyWindow ?: UIWindow()
}

/** iOS implementation of the shared [GmailController]. */
class IosGmailController(
    private val clientId: String,
    private val authenticator: IosGmailAuthenticator,
) : GmailController {
    private val scope = CoroutineScope(Dispatchers.Main)

    override val isSupported: Boolean get() = clientId.isNotBlank()
    override fun isConnected(): Boolean = authenticator.hasRefreshToken()
    override fun connect() {
        scope.launch { runCatching { authenticator.authorize() } }
    }
    override fun disconnect() = authenticator.signOut()
    override fun tokenProvider(): GmailTokenProvider = authenticator
}

/**
 * Publishes the iOS Gmail controller. Called from MainViewController at startup. The client id is
 * read from the app's Info.plist (`GIDClientID`); when absent, Gmail simply stays hidden.
 */
fun registerIosGmailController() {
    val clientId = NSBundle.mainBundle.objectForInfoDictionaryKey("GIDClientID") as? String ?: return
    if (clientId.isBlank()) return
    val authenticator = IosGmailAuthenticator(clientId, defaultGmailHttpClient())
    GmailIntegration.controller = IosGmailController(clientId, authenticator)
}
