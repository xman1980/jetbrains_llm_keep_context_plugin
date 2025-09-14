package com.vitalizasimovich.llmchat.model

import java.time.Instant
import java.util.*

data class ChatEntry(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: ChatType = ChatType.CONVERSATION,
    val context: String? = null,
    val tags: List<String> = emptyList(),
    val metadata: ChatMetadata = ChatMetadata()
)

data class ChatMetadata(
    val fileName: String? = null,
    val projectName: String? = null,
    val language: String? = null,
    val lineNumber: Int? = null,
    val model: String? = null,
    val tokensUsed: Int? = null
)

enum class ChatType {
    CONVERSATION,
    PROMPT,
    RESPONSE,
    CONTEXT,
    SYSTEM
}

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val entries: MutableList<ChatEntry> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()
)

data class ChatHistory(
    val sessions: MutableList<ChatSession> = mutableListOf(),
    val version: Int = 1,
    var metadata: HistoryMetadata = HistoryMetadata()
)

data class HistoryMetadata(
    val totalEntries: Int = 0,
    val lastBackup: Long? = null,
    val exportFormat: ExportFormat = ExportFormat.JSON
)

enum class ExportFormat {
    JSON,
    MARKDOWN,
    BOTH
}

fun ChatEntry.toMarkdown(): String {
    val timeStr = Instant.ofEpochMilli(timestamp).toString()
    val contextSection = if (context != null) "\n\n**Context:**\n```\n$context\n```" else ""
    val tagsSection = if (tags.isNotEmpty()) "\n\n**Tags:** ${tags.joinToString(", ")}" else ""

    return """
        ## Chat Entry - ${type.name}
        **ID:** $id
        **Timestamp:** $timeStr
        $tagsSection

        **Content:**
        $content
        $contextSection

        ---
    """.trimIndent()
}

fun ChatSession.toMarkdown(): String {
    val timeStr = Instant.ofEpochMilli(createdAt).toString()
    val entriesMarkdown = entries.joinToString("\n\n") { it.toMarkdown() }
    val tagsSection = if (tags.isNotEmpty()) "\n**Tags:** ${tags.joinToString(", ")}" else ""

    return """
        # $title
        **Session ID:** $id
        **Created:** $timeStr$tagsSection

        ---

        $entriesMarkdown
    """.trimIndent()
}