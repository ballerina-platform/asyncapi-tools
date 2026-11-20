# How a WebSocket client tells messages apart

A WebSocket connection carries every message on one stream of frames. When a frame arrives, the generated client has to know which message it is, so it can hand it to the call waiting for it. It does that with a **dispatcher key**: a field whose fixed value is different in each message the client receives.

```json
{"type": "joined", "user": "ana"}
{"type": "said", "text": "hi"}
```

Here `type` is the key: `joined` and `said` tell the two messages apart.

You name the key with `x-dispatcherKey` at the top of the spec. Often you don't have to, as the tool works it out from the spec. This page explains when a key is needed, how the tool finds one, and how to fix a spec it can't generate from.

## Name the key

Add `x-dispatcherKey` at the root of the spec, and give each received message's payload that field with its own fixed value, as a `const` or a one-value `enum`:

```yaml
asyncapi: 3.0.0
info: {title: Chat, version: 1.0.0}
x-dispatcherKey: type
channels:
  room:
    address: /rooms/{roomId}
    messages:
      Joined:
        payload:
          type: object
          required: [type, user]
          properties:
            type: {type: string, const: joined}
            user: {type: string}
      Said:
        payload:
          type: object
          required: [type, text]
          properties:
            type: {type: string, const: said}
            text: {type: string}
operations:
  onJoined: {action: receive, channel: {$ref: '#/channels/room'}, messages: [{$ref: '#/channels/room/messages/Joined'}]}
  onSaid: {action: receive, channel: {$ref: '#/channels/room'}, messages: [{$ref: '#/channels/room/messages/Said'}]}
```

The tool reads a loosely described key the way you meant it, with a warning where it changes something:

- A key with only a `const` or an `enum` of strings, and no `type`, is read as a string.
- A key a payload doesn't list in `required` is treated as required, as every frame is routed on it.

## When no key is needed

Only the messages the client **receives** have to be told apart. So:

- **Sent messages don't need the key.** They are sent as they are.
- **A connection that receives one kind of message needs no key at all**, as every frame is that message. For example, a ticker where the client sends `Subscribe` and only ever receives `Tick`:

  ```yaml
  operations:
    subscribe: {action: send, channel: {$ref: '#/channels/prices'}, messages: [{$ref: '#/channels/prices/messages/Subscribe'}]}
    onTick: {action: receive, channel: {$ref: '#/channels/prices'}, messages: [{$ref: '#/channels/prices/messages/Tick'}]}
  ```

Each channel is a connection of its own and gets a client of its own, so this is worked out channel by channel.

## When the tool works the key out

If the spec has no `x-dispatcherKey`, the tool looks for the one field with a different fixed value in every message, and then in every message each connection receives. If it finds exactly one, it routes on it and warns:

```
warning: the spec has no x-dispatcherKey, so the client routes messages on 'type', the only field with a different fixed value in every message
  Add 'x-dispatcherKey: type' to the spec to make it explicit.
```

Add the `x-dispatcherKey` line the warning suggests to make it explicit.

## When generation stops

The tool doesn't guess. When a connection receives more than one kind of message and nothing tells them apart, it stops, names each message and where the spec receives it, and shows the smallest change that adds a key:

```
error: x-dispatcherKey extension is required in the AsyncAPI spec, and no field has a different fixed string value (a const or a one-value enum) in every message received on channel 'room', so the client can't tell them apart:
  - 'Joined', received at /operations/onJoined
  - 'Said', received at /operations/onSaid
Give each received JSON object a field with its own fixed value, and name the field in x-dispatcherKey, such as:
  x-dispatcherKey: type
  Joined payload: type: {type: string, const: joined}
  Said payload: type: {type: string, const: said}
See https://github.com/ballerina-platform/asyncapi-tools/blob/main/docs/websocket-dispatcher-key.md for how the client tells messages apart.
```

Make the change it shows, or use a field your messages already carry, and generate again.

It also stops when:

- **Several fields could be the key.** It names them; add `x-dispatcherKey` with the one the server routes on.
- **The channels use different fields.** A spec has one `x-dispatcherKey`, so give the received messages of every channel the same field.

## Messages without the key

When `x-dispatcherKey` is given and a connection receives several kinds of message, a received message that doesn't declare the key can't be told apart from the others. The client leaves it out and warns:

```
warning: message 'Said' has no 'type' field, so the client can't tell it apart from the other messages and leaves it out
  Add a required 'type' string field with a fixed value to the message's payload, such as type: {type: string, const: said}. See https://github.com/ballerina-platform/asyncapi-tools/blob/main/docs/websocket-dispatcher-key.md
```

A received message that isn't a JSON object, such as plain text, can't carry a field. The client reads it as a frame without the key: it goes to the call waiting for a reply, or to a receive-only function for it.
