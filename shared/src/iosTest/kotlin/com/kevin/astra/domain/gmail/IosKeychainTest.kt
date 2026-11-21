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

    @Test
    fun writeReadUpdateDeleteRoundTrip() {
        assertNull(keychain.read("token"))

        assertTrue(keychain.write("token", "first"))
        assertEquals("first", keychain.read("token"))

        assertTrue(keychain.write("token", "second é"))
        assertEquals("second é", keychain.read("token"))

        keychain.delete("token")
        assertNull(keychain.read("token"))
    }

    @Test
    fun migratesUserDefaultsTokensIntoTheKeychain() {
        defaults.setObject("old-refresh", forKey = GmailTokenStore.KEY_REFRESH)
        defaults.setObject("old-access", forKey = GmailTokenStore.KEY_ACCESS)
        defaults.setDouble(1_234.5, forKey = GmailTokenStore.KEY_EXPIRY)
        val store = GmailTokenStore(keychain)

        val result = GmailTokenMigration.migrate(UserDefaultsLegacyGmailTokenSource(defaults), store)

        assertEquals(GmailTokenMigration.Result.Migrated, result)
        assertEquals("old-refresh", store.refreshToken)
        assertEquals("old-access", store.accessToken)
        assertEquals(1_234.5, store.accessTokenExpiry)
        assertNull(defaults.objectForKey(GmailTokenStore.KEY_REFRESH))
        assertNull(defaults.objectForKey(GmailTokenStore.KEY_ACCESS))
        assertNull(defaults.objectForKey(GmailTokenStore.KEY_EXPIRY))

        store.clear()
        assertNull(keychain.read(GmailTokenStore.KEY_REFRESH))
    }
}
