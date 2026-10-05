# Module MoveWithShortcuts

# Changelog

## [3.0.3-SNAPSHOT]


### Changed


### Added


### Removed


### Fixed


### Internal changes

- Make some private constructors protected.
  For the factory method pattern there is no need that all constructors are private, which would signal
  non-extensibility and make the class effectively final. If one doesn't want to signal non-extensibility, constructors
  can be protected instead.
