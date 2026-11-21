package com.kevin.astra.domain.gmail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeSecureStore(var failWrites: Boolean = false) : SecureStringStore {
    val values = mutableMapOf<String, String>()
    override fun read(key: String): String? = values[key]
    override fun write(key: String, value: String): Boolean {
        if (failWrites) return false
        values[key] = value
        return true
    }
    override fun delete(key: String) {
        values.remove(key)
    }
}

private class FakeLegacySource(
    val strings: MutableMap<String, String> = mutableMapOf(),
    val doubles: MutableMap<String, Double> = mutableMapOf(),
) : LegacyGmailTokenSource {
    override fun readString(key: String): String? = strings[key]
    override fun readDouble(key: String): Double? = doubles[key]
    override fun remove(key: String) {
        strings.remove(key)
        doubles.remove(key)
    }
    val isEmpty: Boolean get() = strings.isEmpty() && doubles.isEmpty()
}

class GmailTokenStoreTest {

    @Test
    fun savesAndReadsTokens() {
        val store = GmailTokenStore(FakeSecureStore())
        assertFalse(store.hasRefreshToken())

        assertTrue(store.saveRefreshToken("refresh"))
        assertTrue(store.saveAccessToken("access", 1_000.5))

        assertEquals("refresh", store.refreshToken)
        assertEquals("access", store.accessToken)
        assertEquals(1_000.5, store.accessTokenExpiry)
        assertTrue(store.hasRefreshToken())
    }

    @Test
    fun accessTokenIsOnlyReturnedBeforeExpiryMinusMargin() {
        val store = GmailTokenStore(FakeSecureStore())
        store.saveAccessToken("access", 1_000.0)

        assertEquals("access", store.validAccessToken(nowSeconds = 900.0))
        assertNull(store.validAccessToken(nowSeconds = 940.0))
        assertNull(store.validAccessToken(nowSeconds = 2_000.0))
    }

    @Test
    fun clearRemovesEverything() {
        val secure = FakeSecureStore()
        val store = GmailTokenStore(secure)
        store.saveRefreshToken("refresh")
        store.saveAccessToken("access", 1.0)

        store.clear()

        assertTrue(secure.values.isEmpty())
        assertFalse(store.hasRefreshToken())
    }

    @Test
    fun migrationMovesLegacyTokensThenErasesThem() {
        val legacy = FakeLegacySource(
            strings = mutableMapOf(
                GmailTokenStore.KEY_REFRESH to "old-refresh",
                GmailTokenStore.KEY_ACCESS to "old-access",
            ),
            doubles = mutableMapOf(GmailTokenStore.KEY_EXPIRY to 42.0),
        )
        val store = GmailTokenStore(FakeSecureStore())

        val result = GmailTokenMigration.migrate(legacy, store)

        assertEquals(GmailTokenMigration.Result.Migrated, result)
        assertEquals("old-refresh", store.refreshToken)
        assertEquals("old-access", store.accessToken)
        assertEquals(42.0, store.accessTokenExpiry)
        assertTrue(legacy.isEmpty)
    }

    @Test
    fun migrationKeepsLegacyTokensWhenSecureWriteFails() {
        val legacy = FakeLegacySource(strings = mutableMapOf(GmailTokenStore.KEY_REFRESH to "old-refresh"))
        val store = GmailTokenStore(FakeSecureStore(failWrites = true))

        val result = GmailTokenMigration.migrate(legacy, store)

        assertEquals(GmailTokenMigration.Result.Failed, result)
        assertEquals("old-refresh", legacy.strings[GmailTokenStore.KEY_REFRESH])
    }

    @Test
    fun migrationIsANoOpWithoutLegacyTokens() {
        val secure = FakeSecureStore()
        val result = GmailTokenMigration.migrate(FakeLegacySource(), GmailTokenStore(secure))

        assertEquals(GmailTokenMigration.Result.NothingToMigrate, result)
        assertTrue(secure.values.isEmpty())
    }

    @Test
    fun migrationKeepsANewerSecureSignInAndErasesLegacy() {
        val store = GmailTokenStore(FakeSecureStore())
        store.saveRefreshToken("new-refresh")
        val legacy = FakeLegacySource(strings = mutableMapOf(GmailTokenStore.KEY_REFRESH to "old-refresh"))

        val result = GmailTokenMigration.migrate(legacy, store)

        assertEquals(GmailTokenMigration.Result.AlreadyMigrated, result)
        assertEquals("new-refresh", store.refreshToken)
        assertTrue(legacy.isEmpty)
    }

    @Test
    fun secondMigrationFindsNothing() {
        val legacy = FakeLegacySource(strings = mutableMapOf(GmailTokenStore.KEY_REFRESH to "old-refresh"))
        val store = GmailTokenStore(FakeSecureStore())

        GmailTokenMigration.migrate(legacy, store)

        assertEquals(GmailTokenMigration.Result.NothingToMigrate, GmailTokenMigration.migrate(legacy, store))
        assertEquals("old-refresh", store.refreshToken)
    }
}
