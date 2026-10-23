# Module MoveWithShortcuts

# Changelog

## [4.0.0-SNAPSHOT]


### Changed

- MWS now creates or updates its per-user Windows Explorer context-menu integration at startup, unless the new
  `CreateContextMenu` configuration parameter or the new `-create-context-menu <true|false>` command-line option
  disables it. The existing command-line and interactive console flows are unchanged.


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


### Removed


### Fixed


### Internal changes

- Add the GUI controller, the drop-aware path text field and the registry helper, all unit-tested without touching the
  real Windows registry or creating Swing components.
