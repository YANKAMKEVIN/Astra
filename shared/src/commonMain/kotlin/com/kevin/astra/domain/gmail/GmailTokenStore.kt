package com.kevin.astra.domain.gmail

/**
 * Small secure string store (the iOS Keychain in production, an in-memory map in tests).
 * [write] returns false when the platform refused the write, so callers never drop data they
 * could not save elsewhere.
 */
interface SecureStringStore {
    fun read(key: String): String?
    fun write(key: String, value: String): Boolean
    fun delete(key: String)
}

/**
 * The old, unprotected storage the iOS app used before the Keychain (NSUserDefaults). Only read
 * by [GmailTokenMigration], then emptied.
 */
interface LegacyGmailTokenSource {
    fun readString(key: String): String?
    fun readDouble(key: String): Double?
    fun remove(key: String)
}

/** Gmail OAuth tokens kept in a [SecureStringStore]. Expiry is in seconds since 1970. */
class GmailTokenStore(private val store: SecureStringStore) {

    val refreshToken: String? get() = store.read(KEY_REFRESH)
    val accessToken: String? get() = store.read(KEY_ACCESS)
    val accessTokenExpiry: Double? get() = store.read(KEY_EXPIRY)?.toDoubleOrNull()

    fun hasRefreshToken(): Boolean = refreshToken != null

    /** Returns the cached access token while it is still valid for at least [marginSeconds]. */
    fun validAccessToken(nowSeconds: Double, marginSeconds: Double = 60.0): String? {
        val token = accessToken ?: return null
        val expiry = accessTokenExpiry ?: return null
        return if (nowSeconds < expiry - marginSeconds) token else null
    }

    fun saveAccessToken(token: String, expirySeconds: Double): Boolean =
        store.write(KEY_ACCESS, token) && store.write(KEY_EXPIRY, expirySeconds.toString())

    fun saveRefreshToken(token: String): Boolean = store.write(KEY_REFRESH, token)

    fun clear() {
        store.delete(KEY_REFRESH)
        store.delete(KEY_ACCESS)
        store.delete(KEY_EXPIRY)
    }

    companion object {
        // Same names as the old NSUserDefaults keys, so the migration is a straight copy.
        const val KEY_REFRESH = "astra_gmail_refresh"
        const val KEY_ACCESS = "astra_gmail_access"
        const val KEY_EXPIRY = "astra_gmail_expiry"
    }
}

/**
 * One-time move of the Gmail tokens from the legacy store to the secure one, run at startup.
 * The legacy values are erased only once they are safely in the secure store; if the secure
 * write fails they stay where they are and the migration is retried on the next launch.
 */
object GmailTokenMigration {

    enum class Result { NothingToMigrate, Migrated, AlreadyMigrated, Failed }

    fun migrate(legacy: LegacyGmailTokenSource, target: GmailTokenStore): Result {
        val refresh = legacy.readString(GmailTokenStore.KEY_REFRESH)
        val access = legacy.readString(GmailTokenStore.KEY_ACCESS)
        val expiry = legacy.readDouble(GmailTokenStore.KEY_EXPIRY)
        if (refresh == null && access == null && expiry == null) return Result.NothingToMigrate

        // The secure store already holds a sign-in (newer than the legacy one): keep it.
        if (target.hasRefreshToken()) {
            clearLegacy(legacy)
            return Result.AlreadyMigrated
        }

        val saved = (refresh == null || target.saveRefreshToken(refresh)) &&
            (access == null || expiry == null || target.saveAccessToken(access, expiry))
        if (!saved || (refresh != null && target.refreshToken != refresh)) {
            return Result.Failed
        }
        clearLegacy(legacy)
        return Result.Migrated
    }

    private fun clearLegacy(legacy: LegacyGmailTokenSource) {
        legacy.remove(GmailTokenStore.KEY_REFRESH)
        legacy.remove(GmailTokenStore.KEY_ACCESS)
        legacy.remove(GmailTokenStore.KEY_EXPIRY)
    }
}
