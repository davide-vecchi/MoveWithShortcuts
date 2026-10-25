# Module MoveWithShortcuts

# Changelog

## [4.0.0-SNAPSHOT]


### Changed

- MWS now creates or updates its per-user Windows Explorer context-menu integration at startup, unless the new
  `CreateContextMenu` configuration parameter or the new `-create-context-menu <true|false>` command-line option
  disables it.

- GUI mode is now the default. The console flow starts with `-console` or with `Console = true` in the configuration
  file; `-gui` forces the GUI. In GUI mode `-from`, `-to` and `-search` prefill the fields and the other operation
  options are honored; invalid GUI-mode values are reported in the operation-detail area. The console launchers add
  `-console`, and new GUI launchers use `javaw`.


### Added

- A Swing GUI entry point (`MoveWithShortcutsGuiMain`) with original, destination and search-folder fields : each field
  accepts a dropped file or folder and has a browse button ; the search folder defaults to the current folder. The
  operation runs in the background after one confirmation, and a final dialog shows the summary or the error.

- The `Move/rename with MWS` Windows Explorer right-click context menu for files and folders, running the GUI with the
  selected item's path. On Windows 11 the entry appears under "Show more options".

- The `CreateContextMenu` configuration parameter and the `-create-context-menu <true|false>` command-line option
  (default `true`), which control the context-menu integration.

- The first start creates or updates the context-menu integration and shows a message only when it did so ; the
  subsequent starts are quiet.

- Phase-labelled progress with the total number of shortcuts, a green `Completed` / red `Failed` progress bar, and an
  operation-detail text area in the GUI that keeps the previous status lines, newest first. The Java updater loops now
  also report their progress in the console.

- Per-instance log folders named `<timestamp>_<original leaf>` : several GUI and console instances can run at the same
  time without sharing log files. The summary shows the actual log folder with a trailing separator.

- The program icon in the dialog title bar, the About dialog and the Windows Explorer context menu. The distributable
  ZIP includes `MWS.ico` next to the JAR.

- A Help button with the question-mark icon, showing the command-line usage text in a scrollable dialog.


### Removed


### Fixed

- The three `...` browse buttons, the Help button and the About button are now disabled while an operation is running,
  like the path fields.


### Internal changes

- Add the GUI controller, the drop-aware path text field and the registry helper, all unit-tested without touching the
  real Windows registry or creating Swing components.
