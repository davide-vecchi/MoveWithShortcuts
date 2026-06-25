# Changelog

## [2.1.0-SNAPSHOT] - Unreleased


### Changed

- **Breaking:**
  Make IShortcutTargetUpdater.updateTargetIfMatch return TargetUpdateOutcome
  which carries more info to show to the user about what update took place
  if any.

- Bump version to 2.1.0-SNAPSHOT

- Update all dependencies on DLibs to 2.1.0-SNAPSHOT


### Added

- **Breaking:**
  Add a new abstract class AAppContext from new module DApplication to be used
  by consumers to extend their own AppContext concrete class, which must now
  extend AAppContext.

- Add CHANGELOG.md .


### Removed


### Fixed
