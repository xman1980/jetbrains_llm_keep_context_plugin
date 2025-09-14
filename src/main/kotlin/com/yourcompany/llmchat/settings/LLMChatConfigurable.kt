package com.yourcompany.llmchat.settings

import com.intellij.openapi.options.Configurable
import com.yourcompany.llmchat.model.ExportFormat
import javax.swing.*

class LLMChatConfigurable : Configurable {
    private var settingsPanel: LLMChatSettingsPanel? = null
    private val settings = LLMChatSettings.getInstance()

    override fun getDisplayName(): String = "LLM Chat History"

    override fun createComponent(): JComponent {
        settingsPanel = LLMChatSettingsPanel()
        return settingsPanel!!.createPanel()
    }

    override fun isModified(): Boolean {
        val panel = settingsPanel ?: return false
        return panel.autoSaveCheckBox.isSelected != settings.autoSave ||
                panel.getSelectedExportFormat() != settings.exportFormat ||
                panel.maxHistorySpinner.value != settings.maxHistoryEntries ||
                panel.contextCaptureCheckBox.isSelected != settings.enableContextCapture ||
                panel.defaultSessionField.text != settings.defaultSessionName ||
                panel.autoBackupCheckBox.isSelected != settings.autoBackup ||
                panel.backupIntervalSpinner.value != settings.backupIntervalHours ||
                panel.includeFileContextCheckBox.isSelected != settings.includeFileContext ||
                panel.includeProjectContextCheckBox.isSelected != settings.includeProjectContext ||
                panel.maxContextSpinner.value != settings.maxContextLength ||
                panel.timestampsCheckBox.isSelected != settings.enableTimestamps ||
                panel.tagsCheckBox.isSelected != settings.enableTags ||
                panel.compressionCheckBox.isSelected != settings.compressionEnabled
    }

    override fun apply() {
        val panel = settingsPanel ?: return
        settings.autoSave = panel.autoSaveCheckBox.isSelected
        settings.exportFormat = panel.getSelectedExportFormat()
        settings.maxHistoryEntries = panel.maxHistorySpinner.value as Int
        settings.enableContextCapture = panel.contextCaptureCheckBox.isSelected
        settings.defaultSessionName = panel.defaultSessionField.text
        settings.autoBackup = panel.autoBackupCheckBox.isSelected
        settings.backupIntervalHours = panel.backupIntervalSpinner.value as Int
        settings.includeFileContext = panel.includeFileContextCheckBox.isSelected
        settings.includeProjectContext = panel.includeProjectContextCheckBox.isSelected
        settings.maxContextLength = panel.maxContextSpinner.value as Int
        settings.enableTimestamps = panel.timestampsCheckBox.isSelected
        settings.enableTags = panel.tagsCheckBox.isSelected
        settings.compressionEnabled = panel.compressionCheckBox.isSelected
    }

    override fun reset() {
        val panel = settingsPanel ?: return
        panel.autoSaveCheckBox.isSelected = settings.autoSave
        panel.setSelectedExportFormat(settings.exportFormat)
        panel.maxHistorySpinner.value = settings.maxHistoryEntries
        panel.contextCaptureCheckBox.isSelected = settings.enableContextCapture
        panel.defaultSessionField.text = settings.defaultSessionName
        panel.autoBackupCheckBox.isSelected = settings.autoBackup
        panel.backupIntervalSpinner.value = settings.backupIntervalHours
        panel.includeFileContextCheckBox.isSelected = settings.includeFileContext
        panel.includeProjectContextCheckBox.isSelected = settings.includeProjectContext
        panel.maxContextSpinner.value = settings.maxContextLength
        panel.timestampsCheckBox.isSelected = settings.enableTimestamps
        panel.tagsCheckBox.isSelected = settings.enableTags
        panel.compressionCheckBox.isSelected = settings.compressionEnabled
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}

class LLMChatSettingsPanel {
    val autoSaveCheckBox = JCheckBox("Auto-save chat entries")
    val exportFormatCombo = JComboBox(ExportFormat.values())
    val maxHistorySpinner = JSpinner(SpinnerNumberModel(10000, 100, 100000, 100))
    val contextCaptureCheckBox = JCheckBox("Enable context capture")
    val defaultSessionField = JTextField("Chat Session")
    val autoBackupCheckBox = JCheckBox("Enable automatic backups")
    val backupIntervalSpinner = JSpinner(SpinnerNumberModel(24, 1, 168, 1))
    val includeFileContextCheckBox = JCheckBox("Include file context")
    val includeProjectContextCheckBox = JCheckBox("Include project context")
    val maxContextSpinner = JSpinner(SpinnerNumberModel(1000, 100, 10000, 100))
    val timestampsCheckBox = JCheckBox("Enable timestamps")
    val tagsCheckBox = JCheckBox("Enable tags")
    val compressionCheckBox = JCheckBox("Enable compression")

    fun createPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        // General Settings
        val generalPanel = createTitledPanel("General Settings")
        generalPanel.add(autoSaveCheckBox)
        generalPanel.add(createLabeledComponent("Export Format:", exportFormatCombo))
        generalPanel.add(createLabeledComponent("Max History Entries:", maxHistorySpinner))
        generalPanel.add(createLabeledComponent("Default Session Name:", defaultSessionField))

        // Context Settings
        val contextPanel = createTitledPanel("Context Settings")
        contextPanel.add(contextCaptureCheckBox)
        contextPanel.add(includeFileContextCheckBox)
        contextPanel.add(includeProjectContextCheckBox)
        contextPanel.add(createLabeledComponent("Max Context Length:", maxContextSpinner))

        // Backup Settings
        val backupPanel = createTitledPanel("Backup Settings")
        backupPanel.add(autoBackupCheckBox)
        backupPanel.add(createLabeledComponent("Backup Interval (hours):", backupIntervalSpinner))

        // Feature Settings
        val featurePanel = createTitledPanel("Features")
        featurePanel.add(timestampsCheckBox)
        featurePanel.add(tagsCheckBox)
        featurePanel.add(compressionCheckBox)

        panel.add(generalPanel)
        panel.add(Box.createVerticalStrut(10))
        panel.add(contextPanel)
        panel.add(Box.createVerticalStrut(10))
        panel.add(backupPanel)
        panel.add(Box.createVerticalStrut(10))
        panel.add(featurePanel)
        panel.add(Box.createVerticalGlue())

        return panel
    }

    private fun createTitledPanel(title: String): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createTitledBorder(title)
        return panel
    }

    private fun createLabeledComponent(label: String, component: JComponent): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.add(JLabel(label))
        panel.add(Box.createHorizontalStrut(10))
        panel.add(component)
        panel.add(Box.createHorizontalGlue())
        return panel
    }

    fun getSelectedExportFormat(): ExportFormat {
        return exportFormatCombo.selectedItem as ExportFormat
    }

    fun setSelectedExportFormat(format: ExportFormat) {
        exportFormatCombo.selectedItem = format
    }
}