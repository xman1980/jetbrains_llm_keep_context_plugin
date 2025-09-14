package com.vitalizasimovich.llmchat.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBTextField
import com.intellij.ui.treeStructure.Tree
import com.vitalizasimovich.llmchat.model.ChatEntry
import com.vitalizasimovich.llmchat.model.ChatSession
import com.vitalizasimovich.llmchat.service.ChatHistoryService
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class ChatHistoryPanel(private val project: Project) : SimpleToolWindowPanel(true, true) {

    private val chatHistoryService = ChatHistoryService.getInstance(project)
    private val searchField = JBTextField()
    private val chatTree = Tree()
    private val contentArea = JTextArea()
    private val rootNode = DefaultMutableTreeNode("Chat History")
    private val treeModel = DefaultTreeModel(rootNode)

    init {
        setupUI()
        setupListeners()
        refreshChatList()

        chatHistoryService.addListener("ui-panel") { entry ->
            ApplicationManager.getApplication().invokeLater {
                refreshChatList()
            }
        }
    }

    private fun setupUI() {
        chatTree.model = treeModel
        chatTree.isRootVisible = true
        chatTree.showsRootHandles = true

        contentArea.isEditable = false
        contentArea.lineWrap = true
        contentArea.wrapStyleWord = true

        val searchPanel = createSearchPanel()
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = ScrollPaneFactory.createScrollPane(chatTree)
            rightComponent = ScrollPaneFactory.createScrollPane(contentArea)
            dividerLocation = 300
        }

        val mainPanel = JPanel(BorderLayout()).apply {
            add(searchPanel, BorderLayout.NORTH)
            add(splitPane, BorderLayout.CENTER)
        }

        setContent(mainPanel)

        val toolbar = createToolbar()
        setToolbar(toolbar.component)
    }

    private fun createSearchPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        searchField.toolTipText = "Search chat history..."

        val searchLabel = JLabel("Search: ")
        val clearButton = JButton("Clear").apply {
            addActionListener {
                searchField.text = ""
                refreshChatList()
            }
        }

        val searchInputPanel = JPanel(BorderLayout()).apply {
            add(searchLabel, BorderLayout.WEST)
            add(searchField, BorderLayout.CENTER)
            add(clearButton, BorderLayout.EAST)
        }

        panel.add(searchInputPanel, BorderLayout.CENTER)
        return panel
    }

    private fun createToolbar(): ActionToolbar {
        val actionGroup = DefaultActionGroup().apply {
            add(RefreshAction())
            add(ExportAction())
            add(DeleteSessionAction())
            add(Separator.getInstance())
            add(NewSessionAction())
            add(ClearHistoryAction())
        }

        return ActionManager.getInstance()
            .createActionToolbar("ChatHistoryToolbar", actionGroup, true)
    }

    private fun setupListeners() {
        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    performSearch()
                }
            }
        })

        chatTree.addTreeSelectionListener { event ->
            val selectedNode = event.path?.lastPathComponent as? DefaultMutableTreeNode
            selectedNode?.let { node ->
                when (val userObject = node.userObject) {
                    is ChatEntry -> displayChatEntry(userObject)
                    is ChatSession -> displaySession(userObject)
                    is String -> contentArea.text = if (userObject == "Chat History") {
                        generateOverview()
                    } else {
                        "Select a chat entry to view its content"
                    }
                }
            }
        }
    }

    private fun refreshChatList() {
        rootNode.removeAllChildren()

        val sessions = chatHistoryService.getSessions()
        sessions.sortedByDescending { it.updatedAt }.forEach { session ->
            val sessionNode = DefaultMutableTreeNode(session)
            session.entries.sortedByDescending { it.timestamp }.forEach { entry ->
                val entryNode = DefaultMutableTreeNode(entry)
                sessionNode.add(entryNode)
            }
            rootNode.add(sessionNode)
        }

        treeModel.reload()
        expandAllNodes()
    }

    private fun performSearch() {
        val query = searchField.text.trim()
        rootNode.removeAllChildren()

        if (query.isEmpty()) {
            refreshChatList()
            return
        }

        val searchResults = chatHistoryService.searchEntries(query)
        if (searchResults.isEmpty()) {
            val noResultsNode = DefaultMutableTreeNode("No results found for '$query'")
            rootNode.add(noResultsNode)
        } else {
            val resultsNode = DefaultMutableTreeNode("Search Results for '$query' (${searchResults.size})")
            searchResults.forEach { (session, entry) ->
                val entryNode = DefaultMutableTreeNode(SearchResult(entry, session))
                resultsNode.add(entryNode)
            }
            rootNode.add(resultsNode)
        }

        treeModel.reload()
        expandAllNodes()
    }

    private fun displayChatEntry(entry: ChatEntry) {
        val content = buildString {
            append("=== Chat Entry ===\n\n")
            append("ID: ${entry.id}\n")
            append("Type: ${entry.type}\n")
            append("Timestamp: ${java.time.Instant.ofEpochMilli(entry.timestamp)}\n")
            if (entry.tags.isNotEmpty()) {
                append("Tags: ${entry.tags.joinToString(", ")}\n")
            }
            append("\n--- Content ---\n")
            append(entry.content)

            if (entry.context != null) {
                append("\n\n--- Context ---\n")
                append(entry.context)
            }

            if (entry.metadata.fileName != null) {
                append("\n\n--- Metadata ---\n")
                append("File: ${entry.metadata.fileName}\n")
                entry.metadata.lineNumber?.let { append("Line: $it\n") }
                entry.metadata.language?.let { append("Language: $it\n") }
                entry.metadata.model?.let { append("Model: $it\n") }
            }
        }
        contentArea.text = content
    }

    private fun displaySession(session: ChatSession) {
        val content = buildString {
            append("=== Session: ${session.title} ===\n\n")
            append("ID: ${session.id}\n")
            append("Created: ${java.time.Instant.ofEpochMilli(session.createdAt)}\n")
            append("Updated: ${java.time.Instant.ofEpochMilli(session.updatedAt)}\n")
            append("Entries: ${session.entries.size}\n")
            if (session.tags.isNotEmpty()) {
                append("Tags: ${session.tags.joinToString(", ")}\n")
            }
            append("\n--- Recent Entries ---\n")

            session.entries.sortedByDescending { it.timestamp }.take(3).forEach { entry ->
                append("\n• ${entry.type} - ${java.time.Instant.ofEpochMilli(entry.timestamp)}\n")
                append("  ${entry.content.take(100)}${if (entry.content.length > 100) "..." else ""}\n")
            }
        }
        contentArea.text = content
    }

    private fun generateOverview(): String {
        val stats = chatHistoryService.getStatistics()
        return buildString {
            append("=== LLM Chat History Overview ===\n\n")
            append("Total Sessions: ${stats.totalSessions}\n")
            append("Total Entries: ${stats.totalEntries}\n")
            append("Average Entries per Session: ${"%.1f".format(stats.averageEntriesPerSession)}\n")

            if (stats.oldestEntryTimestamp != null) {
                append("Oldest Entry: ${java.time.Instant.ofEpochMilli(stats.oldestEntryTimestamp)}\n")
            }
            if (stats.newestEntryTimestamp != null) {
                append("Newest Entry: ${java.time.Instant.ofEpochMilli(stats.newestEntryTimestamp)}\n")
            }

            append("\nSelect a session or entry from the tree to view details.")
        }
    }

    private fun expandAllNodes() {
        for (i in 0 until chatTree.rowCount) {
            chatTree.expandRow(i)
        }
    }

    private fun getSelectedSession(): ChatSession? {
        val selectedPath = chatTree.selectionPath ?: return null
        val selectedNode = selectedPath.lastPathComponent as? DefaultMutableTreeNode ?: return null
        return when (val userObject = selectedNode.userObject) {
            is ChatSession -> userObject
            is ChatEntry -> {
                val parentNode = selectedNode.parent as? DefaultMutableTreeNode
                parentNode?.userObject as? ChatSession
            }
            else -> null
        }
    }

    private fun getSelectedEntry(): ChatEntry? {
        val selectedPath = chatTree.selectionPath ?: return null
        val selectedNode = selectedPath.lastPathComponent as? DefaultMutableTreeNode ?: return null
        return when (val userObject = selectedNode.userObject) {
            is ChatEntry -> userObject
            is SearchResult -> userObject.entry
            else -> null
        }
    }

    override fun getContent(): JComponent = this

    // Action classes
    inner class RefreshAction : AnAction("Refresh", "Refresh chat history", AllIcons.Actions.Refresh) {
        override fun actionPerformed(e: AnActionEvent) {
            refreshChatList()
        }
    }

    inner class ExportAction : AnAction("Export", "Export chat history to markdown", AllIcons.Actions.Download) {
        override fun actionPerformed(e: AnActionEvent) {
            val success = chatHistoryService.exportToMarkdown()
            val message = if (success) "Chat history exported to markdown successfully!"
                         else "Failed to export chat history"
            JOptionPane.showMessageDialog(this@ChatHistoryPanel, message)
        }
    }

    inner class DeleteSessionAction : AnAction("Delete Session", "Delete selected session", AllIcons.Actions.Cancel) {
        override fun actionPerformed(e: AnActionEvent) {
            val session = getSelectedSession() ?: return
            val result = JOptionPane.showConfirmDialog(
                this@ChatHistoryPanel,
                "Are you sure you want to delete session '${session.title}'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
            )
            if (result == JOptionPane.YES_OPTION) {
                chatHistoryService.deleteSession(session.id)
                refreshChatList()
            }
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = getSelectedSession() != null
        }
    }

    inner class NewSessionAction : AnAction("New Session", "Create a new chat session", AllIcons.Actions.AddList) {
        override fun actionPerformed(e: AnActionEvent) {
            val title = JOptionPane.showInputDialog(
                this@ChatHistoryPanel,
                "Enter session title:",
                "New Session",
                JOptionPane.PLAIN_MESSAGE
            ) ?: return

            if (title.isNotBlank()) {
                chatHistoryService.createSession(title.trim())
                refreshChatList()
            }
        }
    }

    inner class ClearHistoryAction : AnAction("Clear History", "Clear all chat history", AllIcons.Actions.GC) {
        override fun actionPerformed(e: AnActionEvent) {
            val result = JOptionPane.showConfirmDialog(
                this@ChatHistoryPanel,
                "Are you sure you want to clear ALL chat history? This cannot be undone.",
                "Clear History",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            )
            if (result == JOptionPane.YES_OPTION) {
                chatHistoryService.getSessions().forEach { session ->
                    chatHistoryService.deleteSession(session.id)
                }
                refreshChatList()
            }
        }
    }

    data class SearchResult(val entry: ChatEntry, val session: ChatSession) {
        override fun toString(): String {
            return "${session.title} - ${entry.type} (${java.time.Instant.ofEpochMilli(entry.timestamp)})"
        }
    }
}