# Module MoveWithShortcuts

# Changelog

## [3.0.1-SNAPSHOT] @@@@@@@@@@@@@@@@@@@@@@@@@


### Changed

- Bump version to 3.0.1-SNAPSHOT .<br><br> @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 3.0.1


### Added


### Removed


### Fixed

- Use new version `2.3.0` of `DFile` dependency to bypass Windows' Execution Policy when launching PowerShell scripts.<br>

  This prevents the failure, on systems with PowerShell execution policy set to `Restricted`, of the PowerShell scripts
  that the `WinShortcutsUpdater_PSScriptsMulti` implementation of `IShortcutsUpdater` in DFile uses to read and write
  shortcuts.<br><br>

- Use new version `2.2.1` of `DUtil` dependency to fix NoSuchMethodException when a Supplier for Throwable-s that don't
  have the required constructor (f.ex. `org.apache.commons.lang3.exception.UncheckedException`) are requested.<br><br>


### Internal changes
