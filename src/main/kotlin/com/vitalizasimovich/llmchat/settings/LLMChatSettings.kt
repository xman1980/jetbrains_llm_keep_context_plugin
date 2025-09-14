package com.vitalizasimovich.llmchat.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.vitalizasimovich.llmchat.model.ExportFormat

@Service(Service.Level.APP)
@State(
    name = "LLMChatSettings",
    storages = [Storage("llmChatHistory.xml")]
)
class LLMChatSettings : PersistentStateComponent<LLMChatSettings.State> {

    data class State(
        var autoSave: Boolean = true,
        var exportFormat: ExportFormat = ExportFormat.BOTH,
        var maxHistoryEntries: Int = 10000,
        var enableContextCapture: Boolean = true,
        var defaultSessionName: String = "Chat Session",
        var autoBackup: Boolean = true,
        var backupIntervalHours: Int = 24,
        var includeFileContext: Boolean = true,
        var includeProjectContext: Boolean = true,
        var maxContextLength: Int = 1000,
        var enableTimestamps: Boolean = true,
        var enableTags: Boolean = true,
        var compressionEnabled: Boolean = false
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var autoSave: Boolean
        get() = myState.autoSave
        set(value) { myState.autoSave = value }

    var exportFormat: ExportFormat
        get() = myState.exportFormat
        set(value) { myState.exportFormat = value }

    var maxHistoryEntries: Int
        get() = myState.maxHistoryEntries
        set(value) { myState.maxHistoryEntries = value }

    var enableContextCapture: Boolean
        get() = myState.enableContextCapture
        set(value) { myState.enableContextCapture = value }

    var defaultSessionName: String
        get() = myState.defaultSessionName
        set(value) { myState.defaultSessionName = value }

    var autoBackup: Boolean
        get() = myState.autoBackup
        set(value) { myState.autoBackup = value }

    var backupIntervalHours: Int
        get() = myState.backupIntervalHours
        set(value) { myState.backupIntervalHours = value }

    var includeFileContext: Boolean
        get() = myState.includeFileContext
        set(value) { myState.includeFileContext = value }

    var includeProjectContext: Boolean
        get() = myState.includeProjectContext
        set(value) { myState.includeProjectContext = value }

    var maxContextLength: Int
        get() = myState.maxContextLength
        set(value) { myState.maxContextLength = value }

    var enableTimestamps: Boolean
        get() = myState.enableTimestamps
        set(value) { myState.enableTimestamps = value }

    var enableTags: Boolean
        get() = myState.enableTags
        set(value) { myState.enableTags = value }

    var compressionEnabled: Boolean
        get() = myState.compressionEnabled
        set(value) { myState.compressionEnabled = value }

    companion object {
        fun getInstance(): LLMChatSettings {
            return ApplicationManager.getApplication().getService(LLMChatSettings::class.java)
        }
    }
}