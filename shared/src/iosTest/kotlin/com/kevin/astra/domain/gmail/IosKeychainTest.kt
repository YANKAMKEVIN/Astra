package com.kevin.astra.domain.gmail

import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosKeychainTest {

    // A dedicated service and defaults suite, so the test never touches the app's real tokens.
    private val keychain = IosKeychain(service = "com.kevin.astra.gmail.test")
    private val suiteName = "com.kevin.astra.gmail.test"
    private val defaults = NSUserDefaults(suiteName = suiteName)

    @BeforeTest
    fun setUp() {
        keychain.deleteAll()
        defaults.removePersistentDomainForName(suiteName)
    }

    @AfterTest
    fun tearDown() {
        keychain.deleteAll()
        defaults.removePersistentDomainForName(suiteName)
    }

    /**
     * The Kotlin/Native test executable is spawned on the simulator without an app bundle, so
     * iOS may refuse it a keychain (errSecNotAvailable -25291, or -34018 for a missing
     * entitlement). The round trip then cannot run; it runs whenever a keychain is available.
     */
    private fun keychainAvailable(): Boolean {
        val ok = keychain.write("probe", "probe")
        keychain.delete("probe")
        if (!ok && keychain.lastStatus in setOf(-25291, -34018)) {
            println("IosKeychainTest: no keychain for the test process (status ${keychain.lastStatus}), skipped.")
            return false
        }
        assertTrue(ok, "probe write status ${keychain.lastStatus}")
        return true
    }

    @Test
    fun writeReadUpdateDeleteRoundTrip() {
        if (!keychainAvailable()) return
        assertNull(keychain.read("token"), "initial read status ${keychain.lastStatus}")

        assertTrue(keychain.write("token", "first"), "write status ${keychain.lastStatus}")
        assertEquals("first", keychain.read("token"), "read status ${keychain.lastStatus}")

        assertTrue(keychain.write("token", "second é"))
        assertEquals("second é", keychain.read("token"))

        keychain.delete("token")
        assertNull(keychain.read("token"))
    }

    @Test
    fun migratesUserDefaultsTokensIntoTheKeychain() {
        if (!keychainAvailable()) return
        defaults.setObject("old-refresh", forKey = GmailTokenStore.KEY_REFRESH)
        defaults.setObject("old-access", forKey = GmailTokenStore.KEY_ACCESS)
        defaults.setDouble(1_234.5, forKey = GmailTokenStore.KEY_EXPIRY)
        val store = GmailTokenStore(keychain)

        val result = GmailTokenMigration.migrate(UserDefaultsLegacyGmailTokenSource(defaults), store)

        assertEquals(GmailTokenMigration.Result.Migrated, result, "keychain status ${keychain.lastStatus}")
        assertEquals("old-refresh", store.refreshToken)
        assertEquals("old-access", store.accessToken)
        assertEquals(1_234.5, store.accessTokenExpiry)
        assertNull(defaults.objectForKey(GmailTokenStore.KEY_REFRESH))
        assertNull(defaults.objectForKey(GmailTokenStore.KEY_ACCESS))
        assertNull(defaults.objectForKey(GmailTokenStore.KEY_EXPIRY))

        store.clear()
        assertNull(keychain.read(GmailTokenStore.KEY_REFRESH))
    }

    @Test
    fun readsAndErasesTheLegacyUserDefaultsTokens() {
        defaults.setObject("old-refresh", forKey = GmailTokenStore.KEY_REFRESH)
        defaults.setDouble(1_234.5, forKey = GmailTokenStore.KEY_EXPIRY)
        val legacy = UserDefaultsLegacyGmailTokenSource(defaults)

        assertEquals("old-refresh", legacy.readString(GmailTokenStore.KEY_REFRESH))
        assertEquals(1_234.5, legacy.readDouble(GmailTokenStore.KEY_EXPIRY))
        assertNull(legacy.readString(GmailTokenStore.KEY_ACCESS))
        assertNull(legacy.readDouble(GmailTokenStore.KEY_ACCESS))

        legacy.remove(GmailTokenStore.KEY_REFRESH)
        legacy.remove(GmailTokenStore.KEY_EXPIRY)
        assertNull(defaults.objectForKey(GmailTokenStore.KEY_REFRESH))
        assertNull(defaults.objectForKey(GmailTokenStore.KEY_EXPIRY))
    }
}
