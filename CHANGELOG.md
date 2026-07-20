# Changelog

## [2.1.0]


### Changed

- **Breaking:**
  Make IShortcutTargetUpdater.updateTargetIfMatch return TargetUpdateOutcome
  which carries more info to show to the user about what update took place
  if any.

- Bump version to 2.1.0 .

- Update all dependencies on DLibs to 2.1.0 .


### Added

- Use new WinShortcutsUpdater_PSScriptsMulti class (note it starts
  with "WinShortcuts", plural). It uses 2 PowerShell scripts, the first to
  read all the required shortcuts targets and working directories in one process
  and the second to update all the necessary shortcuts target and working
  directories in one process.

  Previously it was using WinShortcutUpdater_PS_WSH01 (starts with "WinShortcut",
  singular) which, for each shortcut, creates one PowerShell process to read its
  target and working directory and (if update is necessary) creates another
  PowerShell process to update the target.

- Set system property "file.encoding" to UTF-8.

- Add display of total processed shortcuts and add the info to progress display.

- **Breaking:**
  Add a new abstract class AAppContext from new module DApplication to be used
  by consumers to extend their own AppContext concrete class, which must now
  extend AAppContext.

- Add CHANGELOG.md .


### Removed

- Remove build scripts that first attempt to build the "necessary" DLibs.
  Doing that doesn't cover transitive dependencies anyway, and the protocol
  is now to always have all the DLibs built before building DLibs consumers.

### Fixed

- Use Windows Terminal instead of batch / PowerShell scripts to start 
  the executable JAR . The purpose is avoiding Windows Console API encoding
  bugs and quirks in handling Unicode chars. See e.g.
  * JDK-8266674, JDK-8356165 .
  * https://stackoverflow.com/a/59989494
  * https://stackoverflow.com/a/11930722
