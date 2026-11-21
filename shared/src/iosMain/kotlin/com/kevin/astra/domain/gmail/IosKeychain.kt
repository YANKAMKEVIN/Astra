@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.kevin.astra.domain.gmail

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSNumber
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/** Keychain service holding the Gmail tokens. */
const val GMAIL_KEYCHAIN_SERVICE = "com.kevin.astra.gmail"

/**
 * [SecureStringStore] backed by the iOS Keychain: one generic-password item per key, under
 * [service], readable only after the first unlock and never synced to another device.
 */
class IosKeychain(private val service: String = GMAIL_KEYCHAIN_SERVICE) : SecureStringStore {

    override fun read(key: String): String? = memScoped {
        val query = dictionary(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to cf(service),
            kSecAttrAccount to cf(key),
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne,
        )
        val result = alloc<CFTypeRefVar>()
        if (SecItemCopyMatching(query, result.ptr) != errSecSuccess) return@memScoped null
        val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
        data.toByteArray().decodeToString()
    }

    override fun write(key: String, value: String): Boolean = memScoped {
        val data = cf(value.encodeToByteArray().toNSData())
        val query = dictionary(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to cf(service),
            kSecAttrAccount to cf(key),
        )
        val update = dictionary(
            kSecValueData to data,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        )
        when (SecItemUpdate(query, update)) {
            errSecSuccess -> true
            errSecItemNotFound -> {
                val add = dictionary(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to cf(service),
                    kSecAttrAccount to cf(key),
                    kSecValueData to data,
                    kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                )
                SecItemAdd(add, null) == errSecSuccess
            }
            else -> false
        }
    }

    override fun delete(key: String) {
        memScoped {
            SecItemDelete(
                dictionary(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to cf(service),
                    kSecAttrAccount to cf(key),
                ),
            )
        }
    }

    /** Removes every item of [service]. */
    fun deleteAll() {
        memScoped {
            SecItemDelete(
                dictionary(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to cf(service),
                ),
            )
        }
    }

    /** Bridges [value] to a CoreFoundation object released when the scope ends. */
    private fun MemScope.cf(value: Any): CFTypeRef? =
        CFBridgingRetain(value).also { ref -> defer { CFRelease(ref) } }

    /** Builds a CFDictionary (retaining its keys and values) released when the scope ends. */
    private fun MemScope.dictionary(vararg pairs: Pair<CFTypeRef?, CFTypeRef?>): CFDictionaryRef? {
        val keys = allocArrayOf(*pairs.map { it.first }.toTypedArray())
        val values = allocArrayOf(*pairs.map { it.second }.toTypedArray())
        val dict = CFDictionaryCreate(
            kCFAllocatorDefault,
            keys.reinterpret(),
            values.reinterpret(),
            pairs.size.convert(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
        defer { CFRelease(dict) }
        return dict
    }
}

private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.convert()) }
    }

private fun NSData.toByteArray(): ByteArray {
    val length = length.toInt()
    if (length == 0) return ByteArray(0)
    val pointer = bytes ?: return ByteArray(0)
    return pointer.readBytes(length)
}

/** The pre-Keychain NSUserDefaults storage, read once by [GmailTokenMigration]. */
class UserDefaultsLegacyGmailTokenSource(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : LegacyGmailTokenSource {
    override fun readString(key: String): String? = defaults.stringForKey(key)
    override fun readDouble(key: String): Double? = (defaults.objectForKey(key) as? NSNumber)?.doubleValue
    override fun remove(key: String) = defaults.removeObjectForKey(key)
}
