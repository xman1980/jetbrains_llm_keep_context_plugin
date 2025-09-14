package com.vitalizasimovich.llmchat.service

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.vitalizasimovich.llmchat.model.*
import com.vitalizasimovich.llmchat.settings.LLMChatSettings
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class ChatHistoryService(private val project: Project) {

    private val chatHistory = ChatHistory()
    private val listeners = ConcurrentHashMap<String, (ChatEntry) -> Unit>()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        private val LOG = Logger.getInstance(ChatHistoryService::class.java)
        private const val HISTORY_DIR = ".llm-chat-history"
        private const val JSON_FILE = "chat-history.json"
        private const val MARKDOWN_DIR = "markdown"

        fun getInstance(project: Project): ChatHistoryService =
            project.getService(ChatHistoryService::class.java)
    }

    init {
        loadFromDisk()
    }

    private val storageDir: Path
        get() = Paths.get(project.basePath ?: ".", HISTORY_DIR)

    private val jsonFile: Path
        get() = storageDir.resolve(JSON_FILE)

    private val markdownDir: Path
        get() = storageDir.resolve(MARKDOWN_DIR)

    fun saveChatEntry(entry: ChatEntry, sessionTitle: String = "Default Session"): ChatSession {
        val session = findOrCreateSession(sessionTitle)
        session.entries.add(entry)
        session.updatedAt = System.currentTimeMillis()

        chatHistory.metadata = chatHistory.metadata.copy(
            totalEntries = chatHistory.sessions.sumOf { it.entries.size }
        )

        saveToDisk()
        notifyListeners(entry)
        return session
    }

    fun createSession(title: String): ChatSession {
        val session = ChatSession(title = title)
        chatHistory.sessions.add(session)
        saveToDisk()
        return session
    }

    fun getSessions(): List<ChatSession> = chatHistory.sessions.toList()

    fun getSession(sessionId: String): ChatSession? =
        chatHistory.sessions.find { it.id == sessionId }

    fun getAllEntries(): List<ChatEntry> =
        chatHistory.sessions.flatMap { it.entries }

    fun searchEntries(query: String): List<Pair<ChatSession, ChatEntry>> {
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<Pair<ChatSession, ChatEntry>>()
        val queryLower = query.lowercase()

        chatHistory.sessions.forEach { session ->
            session.entries.forEach { entry ->
                if (entry.content.lowercase().contains(queryLower) ||
                    entry.tags.any { it.lowercase().contains(queryLower) } ||
                    entry.context?.lowercase()?.contains(queryLower) == true ||
                    session.title.lowercase().contains(queryLower)) {
                    results.add(session to entry)
                }
            }
        }

        return results.sortedByDescending { it.second.timestamp }
    }

    fun searchEntriesByTags(tags: List<String>): List<Pair<ChatSession, ChatEntry>> {
        if (tags.isEmpty()) return emptyList()

        val results = mutableListOf<Pair<ChatSession, ChatEntry>>()
        val tagsLower = tags.map { it.lowercase() }

        chatHistory.sessions.forEach { session ->
            session.entries.forEach { entry ->
                if (entry.tags.any { tag ->
                    tagsLower.any { searchTag -> tag.lowercase().contains(searchTag) }
                }) {
                    results.add(session to entry)
                }
            }
        }

        return results.sortedByDescending { it.second.timestamp }
    }

    fun deleteEntry(sessionId: String, entryId: String): Boolean {
        val session = getSession(sessionId) ?: return false
        val removed = session.entries.removeIf { it.id == entryId }
        if (removed) {
            session.updatedAt = System.currentTimeMillis()
            saveToDisk()
        }
        return removed
    }

    fun deleteSession(sessionId: String): Boolean {
        val removed = chatHistory.sessions.removeIf { it.id == sessionId }
        if (removed) {
            saveToDisk()
        }
        return removed
    }

    fun exportToMarkdown(): Boolean {
        return try {
            Files.createDirectories(markdownDir)

            chatHistory.sessions.forEach { session ->
                val fileName = "${session.title.replace(Regex("[^a-zA-Z0-9\\s-]"), "").replace(" ", "-")}-${session.id.take(8)}.md"
                val markdownFile = markdownDir.resolve(fileName)
                Files.writeString(markdownFile, session.toMarkdown(), StandardCharsets.UTF_8)
            }

            val indexContent = generateMarkdownIndex()
            Files.writeString(markdownDir.resolve("index.md"), indexContent, StandardCharsets.UTF_8)

            true
        } catch (e: IOException) {
            LOG.error("Failed to export to markdown", e)
            false
        }
    }

    private fun generateMarkdownIndex(): String {
        val sessions = chatHistory.sessions.sortedByDescending { it.updatedAt }
        val sessionsList = sessions.joinToString("\n") { session ->
            val fileName = "${session.title.replace(Regex("[^a-zA-Z0-9\\s-]"), "").replace(" ", "-")}-${session.id.take(8)}.md"
            "- [${session.title}](./$fileName) (${session.entries.size} entries, updated: ${java.time.Instant.ofEpochMilli(session.updatedAt)})"
        }

        return """
            # LLM Chat History Index

            Total Sessions: ${chatHistory.sessions.size}
            Total Entries: ${chatHistory.metadata.totalEntries}

            ## Sessions
            $sessionsList
        """.trimIndent()
    }

    private fun findOrCreateSession(title: String): ChatSession {
        return chatHistory.sessions.find { it.title == title }
            ?: createSession(title)
    }

    private fun loadFromDisk() {
        try {
            if (Files.exists(jsonFile)) {
                val json = Files.readString(jsonFile, StandardCharsets.UTF_8)
                val loaded = gson.fromJson(json, ChatHistory::class.java)
                chatHistory.sessions.clear()
                chatHistory.sessions.addAll(loaded.sessions)
                chatHistory.metadata = loaded.metadata
                LOG.info("Loaded chat history: ${chatHistory.sessions.size} sessions, ${chatHistory.metadata.totalEntries} entries")
            }
        } catch (e: Exception) {
            LOG.warn("Failed to load chat history from $jsonFile", e)
        }
    }

    private fun saveToDisk() {
        try {
            Files.createDirectories(storageDir)
            val json = gson.toJson(chatHistory)
            Files.writeString(jsonFile, json, StandardCharsets.UTF_8)

            val settings = LLMChatSettings.getInstance()
            if (settings.exportFormat == ExportFormat.MARKDOWN || settings.exportFormat == ExportFormat.BOTH) {
                exportToMarkdown()
            }

            LOG.debug("Saved chat history to $jsonFile")
        } catch (e: IOException) {
            LOG.error("Failed to save chat history to $jsonFile", e)
        }
    }

    fun addListener(id: String, listener: (ChatEntry) -> Unit) {
        listeners[id] = listener
    }

    fun removeListener(id: String) {
        listeners.remove(id)
    }

    private fun notifyListeners(entry: ChatEntry) {
        listeners.values.forEach { listener ->
            try {
                listener(entry)
            } catch (e: Exception) {
                LOG.warn("Error notifying chat history listener", e)
            }
        }
    }

    fun getStatistics(): ChatStatistics {
        val totalSessions = chatHistory.sessions.size
        val totalEntries = chatHistory.metadata.totalEntries
        val oldestEntry = getAllEntries().minByOrNull { it.timestamp }?.timestamp
        val newestEntry = getAllEntries().maxByOrNull { it.timestamp }?.timestamp

        return ChatStatistics(
            totalSessions = totalSessions,
            totalEntries = totalEntries,
            oldestEntryTimestamp = oldestEntry,
            newestEntryTimestamp = newestEntry,
            averageEntriesPerSession = if (totalSessions > 0) totalEntries.toDouble() / totalSessions else 0.0
        )
    }
}

data class ChatStatistics(
    val totalSessions: Int,
    val totalEntries: Int,
    val oldestEntryTimestamp: Long?,
    val newestEntryTimestamp: Long?,
    val averageEntriesPerSession: Double
)