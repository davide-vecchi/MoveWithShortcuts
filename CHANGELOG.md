# Module MoveWithShortcuts

# Changelog

## [4.0.0-SNAPSHOT]


### Changed

- Update the `dlog` dependency to its latest version.

- Update the `dutil` dependency to its latest version.

- Update the `duserinputoutput` dependency to its latest version.

- Update the `dcommandlinearg` dependency to its latest version.

- Update the `dapplication` dependency to its latest version.

- Update the `dfile` dependency to its latest version.

- MWS now creates or updates its per-user Windows Explorer context-menu integration at startup, unless the new
  `CreateContextMenu` configuration parameter or the new `-create-context-menu <true|false>` command-line option
  disables it.

- GUI mode is now the default. The console flow starts with `-console` or with `Console = true` in the configuration
  file; `-gui` forces the GUI. In GUI mode `-from`, `-to` and `-search` prefill the fields and the other operation
  options are honored; invalid GUI-mode values are reported in the operation-detail area. The console launchers add
  `-console`, and new GUI launchers use `javaw`.

- In GUI mode the operation-detail area now shows the same execution messages as console mode, in execution order, at the
  GUI default verbosity of 1; warnings and errors are prefixed with `WARNING: ` / `ERROR: `, the raw progress lines
  stay in the progress bar, and the history is capped at 1000 lines.

- The GUI now offers the shortcuts-updater and shortcut-file-name-policy combos; `-shortcuts-updater` and
  `-shortcut-file-name` set their initial values, the confirmation shows the selected values, and each entry shows a
  short description of the choice.

- The default shortcut-file name policy is now `exact` instead of `never`: a shortcut file whose name matches the old
  leaf and whose target is the moved leaf is renamed by default; pass `-shortcut-file-name never` to keep the old
  behavior.


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
  operation-detail text area in the GUI that keeps the previous status lines, in execution order. The Java updater loops now
  also report their progress in the console.

- The GUI progress bar now shows the `Parsing read results` and `Summarizing read results` phases while the read results
  are parsed and summarized, instead of staying on `Reading shortcuts: 100%` until the analysis starts.

- Per-instance log folders named `<timestamp>_<original leaf>` : several GUI and console instances can run at the same
  time without sharing log files. The summary shows the actual log folder with a trailing separator.

- The program icon in the dialog title bar, the About dialog and the Windows Explorer context menu. The distributable
  ZIP includes `MWS.ico` next to the JAR.

- A Help button with the question-mark icon, showing the command-line usage text in a resizable, scrollable dialog
  with the MWS icon in the title bar. The usage text, including the console `-help` output, now has one empty line
  before each option description.


### Removed


### Fixed

- The three `...` browse buttons, the Help button and the About button are now disabled while an operation is running,
  like the path fields.

- The GUI now releases the screen, user and developer log file handles when an operation ends, so the logs can be
  opened, copied, renamed or deleted while the application stays open; the next operation reopens them in append
  mode.


### Internal changes

- Add the GUI controller, the drop-aware path text field and the registry helper, all unit-tested without touching the
  real Windows registry or creating Swing components.
