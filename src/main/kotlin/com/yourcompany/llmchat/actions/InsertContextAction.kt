package com.yourcompany.llmchat.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBList
import com.yourcompany.llmchat.model.ChatEntry
import com.yourcompany.llmchat.service.ChatHistoryService
import javax.swing.DefaultListModel
import javax.swing.ListSelectionModel

class InsertContextAction : AnAction("Insert Chat Context", "Insert selected chat entry as context", null) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        showChatSelectionPopup(project, editor)
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val project = e.project
        e.presentation.isEnabledAndVisible = editor != null && project != null
    }

    private fun showChatSelectionPopup(project: Project, editor: Editor) {
        val chatHistoryService = ChatHistoryService.getInstance(project)
        val allEntries = chatHistoryService.getAllEntries()

        if (allEntries.isEmpty()) {
            return
        }

        val listModel = DefaultListModel<ChatEntryListItem>()
        allEntries.sortedByDescending { it.timestamp }.forEach { entry ->
            listModel.addElement(ChatEntryListItem(entry))
        }

        val list = JBList(listModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            cellRenderer = ChatEntryListCellRenderer()
        }

        val popup = JBPopupFactory.getInstance()
            .createListPopupBuilder(list)
            .setTitle("Select Chat Entry to Insert")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setItemChoosenCallback {
                val selectedItem = list.selectedValue as? ChatEntryListItem
                selectedItem?.let { item ->
                    insertChatEntry(project, editor, item.entry)
                }
            }
            .createPopup()

        popup.showInBestPositionFor(editor)
    }

    private fun insertChatEntry(project: Project, editor: Editor, entry: ChatEntry) {
        val insertText = formatChatEntryForInsertion(entry)

        WriteCommandAction.runWriteCommandAction(project, "Insert Chat Context", null, {
            val document = editor.document
            val offset = editor.caretModel.offset

            document.insertString(offset, insertText)
            editor.caretModel.moveToOffset(offset + insertText.length)
        })
    }

    private fun formatChatEntryForInsertion(entry: ChatEntry): String {
        val builder = StringBuilder()

        builder.append("# ${entry.type.name} Context\n")
        builder.append("# Added: ${java.time.Instant.ofEpochMilli(entry.timestamp)}\n")

        if (entry.tags.isNotEmpty()) {
            builder.append("# Tags: ${entry.tags.joinToString(", ")}\n")
        }

        builder.append("\n")
        builder.append(entry.content)

        if (entry.context != null) {
            builder.append("\n\n# Original Context:\n")
            builder.append(entry.context)
        }

        builder.append("\n\n")

        return builder.toString()
    }

    private data class ChatEntryListItem(val entry: ChatEntry) {
        override fun toString(): String {
            val timestamp = java.time.Instant.ofEpochMilli(entry.timestamp)
            val preview = entry.content.take(50).replace("\n", " ")
            return "${entry.type} - $timestamp - $preview${if (entry.content.length > 50) "..." else ""}"
        }
    }

    private class ChatEntryListCellRenderer : javax.swing.DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: javax.swing.JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): java.awt.Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

            if (value is ChatEntryListItem) {
                val entry = value.entry
                val typeIcon = when (entry.type) {
                    com.yourcompany.llmchat.model.ChatType.PROMPT -> "?"
                    com.yourcompany.llmchat.model.ChatType.RESPONSE -> "✓"
                    com.yourcompany.llmchat.model.ChatType.CONTEXT -> "📄"
                    com.yourcompany.llmchat.model.ChatType.SYSTEM -> "⚙"
                    else -> "💬"
                }

                text = "$typeIcon ${value.toString()}"

                if (entry.tags.isNotEmpty()) {
                    toolTipText = "Tags: ${entry.tags.joinToString(", ")}"
                }
            }

            return component
        }
    }
}