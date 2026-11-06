# Module MoveWithShortcuts

# Changelog

## [4.0.0] - 2026-11-06


### Changed

- Update the `dlog` dependency to its latest version.

- Update the `dutil` dependency to its latest version.

- Update the `duserinputoutput` dependency to its latest version.

- Update the `dcommandlinearg` dependency to its latest version.

- Update the `dapplication` dependency to its latest version.

- Update the `dfile` dependency to its latest version.

- The default shortcut-file name policy is now `exact` instead of `never`: a shortcut file whose name matches the old
  leaf and whose target is the moved leaf is renamed by default; pass `-shortcut-file-name never` to keep the old
  behavior.

- Warnings and errors are now preceded by a blank line in the GUI operation-detail area and in the console.

- The GUI search field and the console prompt now default to `DefaultSearchPath` / `-default-search-path` /
  `user.home` instead of the Explorer item's parent / the current folder; an unusable default falls back to
  `user.home` with a warning at startup.


### Added

- The new `DefaultSearchPath` configuration parameter and `-default-search-path` command-line option set the default
  folder to scan for shortcuts.

- **New GUI** (the Swing interface is new in 4.0.0):
  - The new `MoveWithShortcutsGuiMain` entry point with original, destination and search-folder fields; each field
    accepts a dropped file or folder and has a browse button. The operation runs in the background after one
    confirmation, and a final dialog shows the summary or the error.
  - The search field uses the `DefaultSearchPath` / `-default-search-path` / `user.home` default; an unusable default
    falls back to `user.home` with a warning at startup.
  - GUI mode is the default. The console flow starts with `-console` or with `Console = true` in the configuration
    file; `-gui` forces the GUI. In GUI mode `-from`, `-to` and `-search` prefill the fields and the other operation
    options are honored; invalid GUI-mode values are reported in the operation-detail area. The console launchers add
    `-console`, and the new GUI launchers use `javaw`.
  - The `Move/rename with MWS` Windows Explorer right-click context menu for files and folders, running the GUI with
    the selected item's path. On Windows 11 the entry appears under "Show more options".
  - The `CreateContextMenu` configuration parameter and the `-create-context-menu <true|false>` command-line option
    (default `true`), which control the context-menu integration. The first start creates or updates it and shows a
    message only when it did so; subsequent starts are quiet.
  - In GUI mode the operation-detail area shows the same execution messages as console mode, in execution order, at
    the GUI default verbosity of 1; warnings and errors are prefixed with `WARNING: ` / `ERROR: `, the raw progress
    lines stay in the progress bar, and the history is capped at 1000 lines.
  - Phase-labelled progress with the total number of shortcuts, a green `Completed` / red `Failed` progress bar, and
    an operation-detail text area that keeps the previous status lines, in execution order. The bar follows the
    window width and shows the `Parsing read results` and `Summarizing read results` phases.
  - The operation-detail output starts with a blank line, a row of 50 dashes, a blank line, the current date and a
    blank line; the text is selectable and copyable (drag selection, `Ctrl+A`, `Ctrl+C` / `Ctrl+Insert`, right-click
    menu with `Copy`, `Copy all`, `Select all`; `Enter` still starts the operation).
  - A `Clear operation-detail text before each move / rename` checkbox (unchecked by default, state not persisted,
    cleared only when the operation actually starts) and a `Save text...` button (UTF-8, suggested name
    `operation-detail-<timestamp>.txt`, disabled when the text is empty or an operation is running).
  - The shortcuts-updater and shortcut-file-name-policy combos; `-shortcuts-updater` and `-shortcut-file-name` set
    their initial values, the confirmation shows the selected values, and each entry shows a short description.
  - The `Cancel` button is named `Close`, with the tooltip `Close this window (Esc).`; the path fields reject a drop
    of more than one file or folder with a warning, and the Help dialog explains that they accept files and folders
    dropped from Windows Explorer.
  - A Help button with the question-mark icon, showing the command-line usage text in a resizable, scrollable dialog
    with the MWS icon; the Help dialog places the drop-aware note after the first usage paragraph.
  - The program icon in the dialog title bar, the About dialog and the Windows Explorer context menu. The
    distributable ZIP includes `MWS.ico` next to the JAR.
  - The browse, Help and About buttons are disabled while an operation is running, like the path fields. When an
    operation ends, the GUI releases the screen, user and developer log file handles, so the logs can be opened,
    copied, renamed or deleted while the application stays open; the next operation reopens them in append mode.

- Per-instance log folders named `<timestamp>_<original leaf>` : several GUI and console instances can run at the
  same time without sharing log files. The summary shows the actual log folder with a trailing separator.

- The Java updater loops now report their progress in the console.

- The usage text, including the console `-help` output, now has one empty line before each option description.


### Removed


### Fixed

- The captured PowerShell output is now hidden at verbosity 0, in the console and in the GUI operation-detail area,
  while the progress lines keep flowing; at verbosity 1 and above it is shown as before.


### Internal changes
