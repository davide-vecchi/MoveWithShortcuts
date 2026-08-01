# Module MoveWithShortcuts

# Changelog

## [3.0.0]


### Changed

- Bump version to 3.0.0 .<br><br>

- **Breaking:**
  Rename program to MoveWithShortcuts. The old name RenameWithLinks must not appear anywhere anymore.<br><br>

- Improve several user messages and prompts.<br><br>

- Update DFile dependency to 2.2.0 .<br><br>

- Update DApplication dependency to 2.2.0 .<br><br>

- Update DUtil dependency to 2.2.0 .<br><br>

- Update DLog dependency to 2.2.0 .<br><br>

- Update DUserInputOutput dependency to 2.2.0 .<br><br>

### Added

- Support "posthumous" execution :<br>
  if the given original path does not exist and the given destination path exists, assume that the renaming / moving has
  already happened before the program started, and it is being used only to adjust the shortcuts, so proceed normally as
  if the renaming / moving from that original to that destination had been performed by the program.


### Removed


### Fixed


### Internal changes
