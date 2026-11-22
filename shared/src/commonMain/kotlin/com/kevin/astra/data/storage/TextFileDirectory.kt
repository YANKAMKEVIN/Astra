package com.kevin.astra.data.storage

/**
 * One private app directory holding small text files (the saved RAG index, the conversations).
 * The platform adapter only reads and writes files; what goes in them stays in common code.
 */
interface TextFileDirectory {
    /** The file's text, or null when it does not exist or cannot be read. */
    fun read(name: String): String?

    /** Replaces [name] in one step, so a crash mid-write never leaves a truncated file. False if it failed. */
    fun write(name: String, text: String): Boolean

    /** Names of the files in the directory (empty when it does not exist yet). */
    fun list(): List<String>

    /** False when there was nothing to delete or the deletion failed. */
    fun delete(name: String): Boolean
}
