package com.kevin.astra.data.history

import com.kevin.astra.domain.history.ChatConversation
import com.kevin.astra.domain.history.ChatMessage

internal object ConversationSerializer {

    fun toJson(conversation: ChatConversation): String = buildString {
        append("{")
        appendKv("id", conversation.id)
        append(",")
        appendKv("title", conversation.title)
        append(",")
        appendKv("model", conversation.modelName)
        append(",")
        appendKv("backend", conversation.backendName)
        append(",")
        appendKv("industry", conversation.industry)
        append(",")
        appendKv("createdAt", conversation.createdAt)
        append(",\"messages\":[")
        conversation.messages.forEachIndexed { index, message ->
            if (index > 0) append(",")
            append("{")
            appendKv("role", message.role)
            append(",")
            appendKv("content", message.content)
            append(",")
            appendKv("ts", message.timestamp)
            append("}")
        }
        append("]}")
    }

    fun fromJson(json: String): ChatConversation? = runCatching {
        val id = extractString(json, "id") ?: return null
        val title = extractString(json, "title") ?: return null
        val model = extractString(json, "model") ?: ""
        val backend = extractString(json, "backend") ?: ""
        val industry = extractString(json, "industry") ?: ""
        val createdAt = extractString(json, "createdAt") ?: ""
        val messages = extractMessages(json)
        ChatConversation(
            id = id,
            title = title,
            modelName = model,
            backendName = backend,
            industry = industry,
            messages = messages,
            createdAt = createdAt,
        )
    }.getOrNull()

    private fun extractMessages(json: String): List<ChatMessage> {
        val marker = "\"messages\":["
        val start = json.indexOf(marker)
        if (start < 0) return emptyList()
        val arrayContent = json.substring(start + marker.length)
        return extractObjects(arrayContent).mapNotNull { obj ->
            val role = extractString(obj, "role") ?: return@mapNotNull null
            val content = extractString(obj, "content") ?: ""
            val ts = extractString(obj, "ts") ?: ""
            ChatMessage(role = role, content = content, timestamp = ts)
        }
    }

    private fun extractObjects(json: String): List<String> {
        val objects = mutableListOf<String>()
        var depth = 0
        var start = -1
        var inString = false
        var escaped = false
        for (i in json.indices) {
            val c = json[i]
            when {
                escaped -> escaped = false
                c == '\\' && inString -> escaped = true
                c == '"' -> inString = !inString
                !inString && c == '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                !inString && c == '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        objects.add(json.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return objects
    }

    private fun extractString(json: String, key: String): String? {
        val marker = "\"$key\":\""
        val markerStart = json.indexOf(marker)
        if (markerStart < 0) return null
        val valueStart = markerStart + marker.length
        val sb = StringBuilder()
        var i = valueStart
        var escaped = false
        while (i < json.length) {
            val c = json[i]
            when {
                escaped -> {
                    when (c) {
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        else -> { sb.append('\\'); sb.append(c) }
                    }
                    escaped = false
                }
                c == '\\' -> escaped = true
                c == '"' -> return sb.toString()
                else -> sb.append(c)
            }
            i++
        }
        return null
    }

    private fun StringBuilder.appendKv(key: String, value: String) {
        append("\"")
        append(key)
        append("\":\"")
        append(value.escapeJson())
        append("\"")
    }

    private fun String.escapeJson(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
}
