# JetBrains LLM Keep Context Plugin

A lightweight JetBrains IDE plugin that automatically saves LLM chat history (prompts + responses) to your project directory as markdown or JSON. Features viewing, searching past sessions, and quick insertion of previous context into new prompts.

**Author:** Vitali Zasimovich
**Repository:** https://github.com/xman1980/jetbrains_llm_keep_context_plugin

## Features

- **Automatic Chat History Storage**: Save chat entries to `.llm-chat-history/` directory in your project
- **Multiple Export Formats**: Supports JSON and Markdown formats (or both)
- **Advanced Search**: Search through all chat history by content, tags, or session names
- **Context Insertion**: Quickly insert previous chat entries into your current work
- **Session Management**: Organize chats into named sessions
- **Remote Development Support**: Fully compatible with JetBrains remote development environments
- **Lightweight & Fast**: Minimal performance impact with efficient storage
- **Open Source**: MIT licensed and extensible

## Installation

### From Release

1. Download the latest release from [GitHub Releases](https://github.com/xman1980/jetbrains_llm_keep_context_plugin/releases)
2. In your JetBrains IDE: **File > Settings > Plugins > Install Plugin from Disk**
3. Select the downloaded `.zip` file

### From Source

1. Clone this repository
2. Open in IntelliJ IDEA
3. Run `./gradlew buildPlugin`
4. Install the generated plugin file from `build/distributions/`

### Configuration

Access settings via **File > Settings > Tools > LLM Chat History**

- **Auto-save**: Automatically save chat entries
- **Export Format**: Choose JSON, Markdown, or both
- **Context Capture**: Include file and project context with entries
- **Session Settings**: Configure default session names and organization

## Usage

### Saving Chat Entries

1. **Select text** in any editor
2. **Right-click** and choose "Save as LLM Chat"
3. **Or use keyboard shortcut**: `Ctrl+Shift+S`
4. Enter a session title or use the default

### Viewing Chat History

1. Open the **LLM Chat History** tool window (right sidebar)
2. Browse sessions and entries in the tree view
3. Use the **search box** to find specific content
4. Click on any entry to view its details

### Inserting Previous Context

1. Place cursor where you want to insert context
2. **Right-click** and choose "Insert Chat Context"
3. **Or use keyboard shortcut**: `Ctrl+Shift+I`
4. Select from the popup list of previous chat entries

### Keyboard Shortcuts

- `Ctrl+Shift+S` - Save selected text as chat entry
- `Ctrl+Shift+I` - Insert previous chat context
- Access via **Tools > LLM Chat History** menu

## File Structure

The plugin stores data in your project's `.llm-chat-history/` directory:

```
.llm-chat-history/
├── chat-history.json          # Main storage (JSON format)
└── markdown/                  # Markdown exports
    ├── index.md              # Session index
    ├── Session-1.md          # Individual session files
    └── ...
```

## Remote Development

This plugin fully supports JetBrains remote development:

- **Backend Services**: Data storage runs on the remote backend
- **UI Compatibility**: Tool windows work in JetBrains Client
- **File Access**: Uses project-relative paths for cross-environment compatibility
- **Performance**: Optimized for network scenarios

## Data Format

### JSON Structure

```json
{
  "sessions": [
    {
      "id": "uuid",
      "title": "Session Name",
      "entries": [
        {
          "id": "uuid",
          "content": "Chat content...",
          "timestamp": 1640995200000,
          "type": "CONVERSATION",
          "tags": ["tag1", "tag2"],
          "context": "File context...",
          "metadata": {
            "fileName": "example.py",
            "lineNumber": 42,
            "language": "Python"
          }
        }
      ],
      "createdAt": 1640995200000,
      "updatedAt": 1640995200000
    }
  ],
  "version": 1
}
```

### Markdown Export

Each session exports to a separate markdown file with structured formatting:

```markdown
# Session Title
**Created:** 2023-12-31T12:00:00Z

---

## Chat Entry - CONVERSATION
**Timestamp:** 2023-12-31T12:05:00Z
**Tags:** python, debugging

**Content:**
How do I fix this error in my Python code?

**Context:**
```python
def example_function():
    return undefined_variable  # Error here
```
```

## Development

### Requirements

- IntelliJ IDEA 2023.1+
- JDK 17+
- Kotlin 1.9+

### Building

```bash
# Build plugin
./gradlew buildPlugin

# Run in development PyCharm instance
./gradlew runIde

# Run tests
./gradlew test

# Verify plugin compatibility
./gradlew runPluginVerifier
```

### Project Structure

```
src/main/kotlin/com/vitalizasimovich/llmchat/
├── actions/           # Editor actions (save, insert)
├── model/            # Data models and serialization
├── service/          # Chat history service
├── settings/         # Configuration and UI
└── ui/              # Tool window and panels
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

MIT License - see [LICENSE](LICENSE) for details.

## Changelog

### Version 1.0.0
- Initial release
- Basic chat history saving and viewing
- Search functionality
- Context insertion feature
- Remote development support
- JSON and Markdown export