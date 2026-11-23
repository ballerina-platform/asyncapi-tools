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

## A key inside an object

Some services put the key inside an object, such as `{"data": {"type": "ready", ...}}`. Name it with a JSON Pointer, a path that starts with `/`:

```yaml
x-dispatcherKey: /data/type
components:
  schemas:
    Ready:
      type: object
      required: [data]
      properties:
        data:
          type: object
          required: [type, session]
          properties:
            type: {type: string, const: ready}
            session: {type: string}
```

The client reads the value at that path in each frame and routes the frame to the message with that fixed value. A pointer to a top-level field, such as `/type`, is the same as `type`. Escape a `/` in a field name as `~1` and a `~` as `~0`, as in any JSON Pointer.

## A key for one message

A message that carries its key somewhere else, such as a heartbeat acknowledgement among events keyed on `/data/type`, can name its own key with `x-dispatcherKey` on the message:

```yaml
components:
  messages:
    Ack:
      x-dispatcherKey: /ack
      payload:
        type: object
        required: [ack]
        properties:
          ack: {type: string, const: heartbeat}
```

A message's own key wins over the spec's. Each received message needs a fixed value of its own at its key's path; one without is left out, with a warning that shows the field to add:

```
warning: message 'Posted' received on channel 'gateway' has no fixed value of its own at '/data/type', the dispatcher key it's routed on, so the client can't tell it apart from the others and leaves it out
  Give its payload a fixed value of its own at '/data/type', such as:
  data:
    type: object
    properties:
      type: {type: string, const: posted}
```

With a nested key or a message's own key, the client reads each frame as JSON and routes it by those values, as it does in [When messages are told apart by what they carry](#when-messages-are-told-apart-by-what-they-carry). A reply streamed to a call needs a key every frame carries at the top level, so generation stops for one.

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

## When messages are told apart by what they carry

If no one field works for the whole spec, the tool looks at each connection on its own:

1. **A field with its own fixed value on that connection.** Two channels can use different fields, such as `type` on one and `kind` on the other.
2. **Otherwise, a field a message fixes to a value no other can carry.** Each other message fixes that field to another value, or doesn't declare it. A frame with `"status": "online"` can only be an `Online` when no other message declares `status`, even if the others are told apart by `kind`.
3. **Otherwise, a field only one message has.** A frame with `price` can only be a `Trade` when no other message the connection receives declares `price`:

   ```yaml
   Trade: {payload: {type: object, required: [symbol, price], properties: {symbol: {type: string}, price: {type: number}}}}
   Status: {payload: {type: object, required: [online], properties: {online: {type: boolean}, symbol: {type: string}}}}
   ```

   The field has to be `required`, so every frame of that message carries it.

Each route is announced with a warning:

```
warning: the spec has no x-dispatcherKey, so the client tells apart the messages received on channel 'feed' by a field only each has: 'price' for 'Trade', 'online' for 'Status'
  Give each a field with its own fixed value and name it in x-dispatcherKey if a frame can carry another message's field. See https://github.com/ballerina-platform/asyncapi-tools/blob/main/docs/websocket-dispatcher-key.md
```

A message that has neither is left out, with a warning that says where it is and how to fix it:

```
warning: message 'Note' received on channel 'feed' has no field only it has and no field with its own fixed value, so the client can't tell it apart from the others and leaves it out
  Give it a required field the channel's other messages don't declare, or give each of them a field with its own fixed value. See https://github.com/ballerina-platform/asyncapi-tools/blob/main/docs/websocket-dispatcher-key.md
```

A frame that matches none of the routes is dropped and logged. Naming a key with `x-dispatcherKey` is still the most reliable choice: a frame that happens to carry another message's field could otherwise be read as that message.

## When generation stops

The tool doesn't guess. When a connection receives more than one kind of message and nothing tells any of them apart, it stops, names each message and where the spec receives it, and shows the smallest change that adds a key:

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

It also stops when several fields could be the key. It names them; add `x-dispatcherKey` with the one the server routes on.

## Messages without the key

When `x-dispatcherKey` is given and a connection receives several kinds of message, a received message that doesn't declare the key can't be told apart from the others. The client leaves it out and warns:

```
warning: message 'Said' has no 'type' field, so the client can't tell it apart from the other messages and leaves it out
  Add a required 'type' string field with a fixed value to the message's payload, such as type: {type: string, const: said}. See https://github.com/ballerina-platform/asyncapi-tools/blob/main/docs/websocket-dispatcher-key.md
```

A received message that isn't a JSON object, such as plain text, can't carry a field. The client reads it as a frame without the key: it goes to the call waiting for a reply, or to a receive-only function for it.
