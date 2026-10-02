# Module MoveWithShortcuts

# Changelog

## [3.0.3-SNAPSHOT]


### Changed


### Added


### Removed


### Fixed


### Internal changes


## [3.0.2] - 2026-10-02


### Changed

- Update all dependencies (DLibs and 3rd party) to their latest versions.


### Added

- Add the packaging scripts (`Packaging/Package_MoveWithShortcuts.BAT` / `.sh`) that build the distributable ZIP.


### Internal changes

- Fix some comments.

- Explicitly add transitive dependency javalibraries3rdparty:threadsafenumberformat .

- Infer some values instead of hardcoding.

- Remove DTestNG DLib, use TestNG directly.


## [3.0.1] - 2026-08-14


### Changed

- Bump version to `3.0.1`.

- Update all `DLibs` dependencies to their latest versions.


### Fixed

- Use new version `2.3.0` of `DFile` dependency to bypass Windows' Execution Policy when launching PowerShell scripts.

  This prevents the failure, on systems with PowerShell execution policy set to `Restricted`, of the PowerShell scripts
  that the `WinShortcutsUpdater_PSScriptsMulti` implementation of `IShortcutsUpdater` in `DFile` uses to read and write
  shortcuts.

- Use new version `2.2.1` of `DUtil` dependency to fix `NoSuchMethodException` when a `Supplier` for `Throwable`s that
  don't have the required constructor (f.ex. `org.apache.commons.lang3.exception.UncheckedException`) is requested.


### Internal changes

- Fix tests that assert on absolute paths; must assert on paths relative to the project folder.

- Fix tests that expect an existing `LOG` folder.


## [3.0.0] - 2026-08-07


### Added

- Rename and/or move files and folders while automatically updating Windows shortcuts (`.lnk` files).

- Updates both the target path and working directory ("Start in" field) of affected shortcuts.

- Supports file and folder operations.

- Interactive console mode for manual use.

- Command-line mode with parameters for scripting and automation.

- Verbosity levels (0-3) for control over output detail.
