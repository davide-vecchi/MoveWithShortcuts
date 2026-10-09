# Module MoveWithShortcuts

# Changelog

## [3.0.3-SNAPSHOT]


### Changed

- Update the DApplication dependency to 2.4.0-SNAPSHOT .

- Update the DUtil dependency to 2.4.0-SNAPSHOT .


### Added


### Removed


### Fixed


### Internal changes

- Make some private constructors protected.
  For the factory method pattern there is no need that all constructors are private, which would signal
  non-extensibility and make the class effectively final. If one doesn't want to signal non-extensibility, constructors
  can be protected instead.
