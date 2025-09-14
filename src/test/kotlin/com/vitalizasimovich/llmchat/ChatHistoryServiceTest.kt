package com.vitalizasimovich.llmchat

import com.vitalizasimovich.llmchat.model.ChatEntry
import com.vitalizasimovich.llmchat.model.ChatType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.nio.file.Files

class ChatHistoryServiceTest {

    @Test
    fun testChatEntryCreation() {
        val entry = ChatEntry(
            content = "Test content",
            type = ChatType.CONVERSATION
        )

        assertNotNull(entry.id)
        assertEquals("Test content", entry.content)
        assertEquals(ChatType.CONVERSATION, entry.type)
        assertTrue(entry.timestamp > 0)
    }

    @Test
    fun testChatEntryToMarkdown() {
        val entry = ChatEntry(
            content = "Test content",
            type = ChatType.PROMPT,
            tags = listOf("test", "example")
        )

        val markdown = entry.toMarkdown()

        assertTrue(markdown.contains("Test content"))
        assertTrue(markdown.contains("PROMPT"))
        assertTrue(markdown.contains("test, example"))
    }

    @Test
    fun testSearchFunctionality() {
        // This would test the search functionality if we had a mock project
        // For now, just test that the models work correctly
        val entries = listOf(
            ChatEntry(content = "How to debug Python code?", tags = listOf("python", "debug")),
            ChatEntry(content = "Kotlin coroutines example", tags = listOf("kotlin")),
            ChatEntry(content = "Python async programming", tags = listOf("python", "async"))
        )

        // Simple search logic test
        val pythonEntries = entries.filter { entry ->
            entry.content.lowercase().contains("python") ||
            entry.tags.any { it.lowercase().contains("python") }
        }

        assertEquals(2, pythonEntries.size)
    }
}