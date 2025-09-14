package com.vitalizasimovich.llmchat.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.vitalizasimovich.llmchat.model.ChatEntry
import com.vitalizasimovich.llmchat.model.ChatMetadata
import com.vitalizasimovich.llmchat.model.ChatType
import com.vitalizasimovich.llmchat.service.ChatHistoryService
import com.vitalizasimovich.llmchat.settings.LLMChatSettings

class SaveChatAction : AnAction("Save as LLM Chat", "Save selected text to LLM chat history", null) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val selectedText = getSelectedTextOrCurrentLine(editor)
        if (selectedText.isBlank()) {
            Messages.showInfoMessage(project, "No text selected or cursor line is empty.", "Save Chat")
            return
        }

        val settings = LLMChatSettings.getInstance()
        val context = if (settings.enableContextCapture) {
            captureContext(project, editor, settings)
        } else null

        val metadata = extractMetadata(project, editor)
        val chatType = determineChatType(selectedText)

        val chatEntry = ChatEntry(
            content = selectedText.trim(),
            type = chatType,
            context = context,
            metadata = metadata
        )

        ApplicationManager.getApplication().invokeLater {
            val sessionTitle = promptForSessionTitle(project, settings.defaultSessionName)
            if (sessionTitle != null) {
                val chatHistoryService = ChatHistoryService.getInstance(project)
                chatHistoryService.saveChatEntry(chatEntry, sessionTitle)

                Messages.showInfoMessage(
                    project,
                    "Chat entry saved to session '$sessionTitle'",
                    "Chat Saved"
                )
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val project = e.project
        e.presentation.isEnabledAndVisible = editor != null && project != null
    }

    private fun getSelectedTextOrCurrentLine(editor: Editor): String {
        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection()) {
            return selectionModel.selectedText ?: ""
        }

        val caretModel = editor.caretModel
        val document = editor.document
        val lineNumber = document.getLineNumber(caretModel.offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)

        return document.getText(com.intellij.openapi.util.TextRange(lineStartOffset, lineEndOffset))
    }

    private fun captureContext(project: Project, editor: Editor, settings: LLMChatSettings): String? {
        val contextBuilder = StringBuilder()

        if (settings.includeFileContext) {
            val fileContext = captureFileContext(project, editor, settings.maxContextLength / 2)
            if (fileContext.isNotEmpty()) {
                contextBuilder.append("File Context:\n").append(fileContext).append("\n\n")
            }
        }

        if (settings.includeProjectContext) {
            val projectContext = captureProjectContext(project, settings.maxContextLength / 2)
            if (projectContext.isNotEmpty()) {
                contextBuilder.append("Project Context:\n").append(projectContext)
            }
        }

        val fullContext = contextBuilder.toString()
        return if (fullContext.isNotEmpty()) {
            if (fullContext.length > settings.maxContextLength) {
                fullContext.take(settings.maxContextLength) + "..."
            } else {
                fullContext
            }
        } else null
    }

    private fun captureFileContext(project: Project, editor: Editor, maxLength: Int): String {
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return ""

        val caretOffset = editor.caretModel.offset
        val lineNumber = document.getLineNumber(caretOffset)

        val startLine = maxOf(0, lineNumber - 10)
        val endLine = minOf(document.lineCount - 1, lineNumber + 10)

        val contextBuilder = StringBuilder()
        for (i in startLine..endLine) {
            val lineContent = document.getText(
                com.intellij.openapi.util.TextRange(
                    document.getLineStartOffset(i),
                    document.getLineEndOffset(i)
                )
            )
            val marker = if (i == lineNumber) " >>> " else "     "
            contextBuilder.append("${i + 1}$marker$lineContent\n")
        }

        val result = contextBuilder.toString()
        return if (result.length > maxLength) {
            result.take(maxLength) + "..."
        } else result
    }

    private fun captureProjectContext(project: Project, maxLength: Int): String {
        val contextBuilder = StringBuilder()
        contextBuilder.append("Project: ${project.name}\n")
        contextBuilder.append("Base Path: ${project.basePath ?: "Unknown"}\n")

        return contextBuilder.toString().let { context ->
            if (context.length > maxLength) {
                context.take(maxLength) + "..."
            } else context
        }
    }

    private fun extractMetadata(project: Project, editor: Editor): ChatMetadata {
        val document = editor.document
        val virtualFile = FileDocumentManager.getInstance().getFile(document)
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)

        val caretOffset = editor.caretModel.offset
        val lineNumber = document.getLineNumber(caretOffset) + 1

        return ChatMetadata(
            fileName = virtualFile?.name,
            projectName = project.name,
            language = psiFile?.language?.displayName,
            lineNumber = lineNumber
        )
    }

    private fun determineChatType(content: String): ChatType {
        val trimmed = content.trim().lowercase()

        return when {
            trimmed.startsWith("prompt:") || trimmed.startsWith("query:") -> ChatType.PROMPT
            trimmed.startsWith("response:") || trimmed.startsWith("answer:") -> ChatType.RESPONSE
            trimmed.startsWith("context:") || trimmed.startsWith("background:") -> ChatType.CONTEXT
            trimmed.startsWith("system:") -> ChatType.SYSTEM
            else -> ChatType.CONVERSATION
        }
    }

    private fun promptForSessionTitle(project: Project, defaultTitle: String): String? {
        return Messages.showInputDialog(
            project,
            "Enter session title:",
            "Save Chat Entry",
            null,
            defaultTitle,
            null
        )
    }
}