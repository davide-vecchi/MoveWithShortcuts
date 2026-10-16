# Module MoveWithShortcuts

# Changelog

## [3.1.0-SNAPSHOT]


### Changed

- Update the DFile dependency to its latest version and use its new overload of listFiles() to always read the shortcuts
  sorted by timestamp ascending, so the ones that will get modified will be in the same relative order of their
  timestamp.

- Update the DApplication dependency to 2.4.0-SNAPSHOT .

- Update the DUtil dependency to 2.4.0-SNAPSHOT .

- Improve some user messages.


### Added


### Removed


### Fixed

- The updated `DFile` dependency also fixes MWS setting an empty work folder ("Start in") in the updated shortcuts
  instead of remapping it to the new target's parent folder, when moving / renaming a file or folder.


### Internal changes

- Make some private constructors protected.
  For the factory method pattern there is no need that all constructors are private, which would signal
  non-extensibility and make the class effectively final. If one doesn't want to signal non-extensibility, constructors
  can be protected instead.
