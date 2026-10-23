# Changelog

All notable changes to this module are documented in this file. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.6.0] - Unreleased

### Added

- `NativeHandler.bindEventPayload`, which binds a raw JSON payload to the record type declared on a named remote
  function of the attached service, discovered by reflection. It binds through `jsondata:parseAsType`, so
  `@jsondata:Name` renames are honoured and explicit JSON `null` values for optional fields are tolerated.
- A dependency on `ballerina/data.jsondata`, so the module is always available to the binding call even when a
  connector has no renamed fields of its own.

### Changed

- The packaged Java library reference now points at the current `java-wrapper` build output (it was pinned to an
  older, no longer built version).

## [0.5.0]

- Last release before this changelog was introduced. Provides `invokeRemoteFunction`.
