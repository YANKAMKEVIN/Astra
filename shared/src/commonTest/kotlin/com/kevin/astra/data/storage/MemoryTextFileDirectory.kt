package com.kevin.astra.data.storage

/** In-memory [TextFileDirectory] for tests; [failWrites] simulates a full or read-only disk. */
class MemoryTextFileDirectory(var failWrites: Boolean = false) : TextFileDirectory {
    val files = linkedMapOf<String, String>()

    override fun read(name: String): String? = files[name]

    override fun write(name: String, text: String): Boolean {
        if (failWrites) return false
        files[name] = text
        return true
    }

    override fun list(): List<String> = files.keys.toList()

    override fun delete(name: String): Boolean = files.remove(name) != null
}
