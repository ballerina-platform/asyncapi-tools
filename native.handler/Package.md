Native support for the dispatch step of the AsyncAPI trigger connectors.

A generated trigger's HTTP resource function cannot call a user's remote function directly, and it also
cannot discover what type that function expects. Both need Java interop, so this module wraps them for the
generated dispatcher to call.

## Functions

- `invokeRemoteFunction`: calls a named remote function on the service attached to the listener, passing it an
  already-bound event.
- `bindEventPayload`: binds a raw JSON payload to the record type declared on the first parameter of a named
  remote function of the attached service. The type is discovered from the service object at runtime, so the
  generator no longer needs to emit an event-to-type table. Binding is delegated to `ballerina/data.jsondata`'s
  `parseAsType` with data projection enabled, so `@jsondata:Name` field renames are honoured and an explicit JSON
  `null` for an optional field is tolerated. Returns an error if the function is not found on the service, if it
  declares no parameter, or if the payload does not fit the declared type.

## Requirements

Triggers generated with a version of `asyncapi-tools` that includes dynamic payload binding depend on version 0.6.0 or later of this module.
