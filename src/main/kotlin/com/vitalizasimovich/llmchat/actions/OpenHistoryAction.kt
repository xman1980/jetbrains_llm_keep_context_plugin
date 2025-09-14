package com.vitalizasimovich.llmchat.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

class OpenHistoryAction : AnAction("Open Chat History", "Open the LLM chat history tool window", null) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        openChatHistoryToolWindow(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    private fun openChatHistoryToolWindow(project: Project) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("LLMChatHistory")

        if (toolWindow != null) {
            toolWindow.activate(null)
        }
    }
}