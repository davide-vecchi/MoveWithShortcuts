# Module MoveWithShortcuts

# Changelog

## [3.0.1-SNAPSHOT]


### Changed

- Bump version to 3.0.1-SNAPSHOT .<br><br> @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 3.0.1

- Use new version `2.3.0` of `DFile` dependency to bypass Windows' Execution Policy when launching PowerShell scripts.<br>
  
  This prevents the failure, on systems with PowerShell execution policy set to `Restricted`, of the PowerShell scripts
  that the `WinShortcutsUpdater_PSScriptsMulti` implementation of `IShortcutsUpdater` in DFile uses to read and write
  shortcuts.<br><br>


### Added


### Removed


### Fixed


### Internal changes
