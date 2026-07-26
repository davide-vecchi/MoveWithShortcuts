# Changelog

## [3.0.0-SNAPSHOT]


### Changed

- Bump version to 3.0.0 .<br><br>

- **Breaking:**
  Program renamed to MoveWithShortcuts. The old name RenameWithLinks must not appear anywhere anymore.<br><br>

- Improved several user messages and prompts.<br><br>

- Update dependency on DFile to 2.2.0 .

### Added

- Support "posthumous" execution :<br>
  if the given original path does not exist and the given destination path exists, assume that the renaming / moving has
  already happened before the program started, and it is being used only to adjust the shortcuts, so proceed normally as
  if the renaming / moving from that original to that destination had been performed by the program.


### Removed


### Fixed
