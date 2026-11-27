# Which WebSocket specs the tool generates a client for

The tool reads an AsyncAPI definition of a WebSocket API and generates a Ballerina client for it. Specs are written in many styles, so this page lists what the tool reads as you meant it, what it generates while leaving something out, and what stops generation. Every warning and error names the place in the spec and the change that fixes it; this page has the same fixes with examples.

## At a glance

| The spec has | What the tool does |
|---|---|
| AsyncAPI 2.x or 3.x, with a `ws` or `wss` server | Generates a client |
| JSON messages told apart by a field, such as `type` | Generates a client; see [How a WebSocket client tells messages apart](websocket-dispatcher-key.md) |
| JSON messages and no `x-dispatcherKey` | Works out the field when it can, with a warning; [stops](#received-messages-cant-be-told-apart) when it can't |
| One kind of message received on a connection, including plain text | Generates a client; no key is needed |
| Requests with replies, as a 3.0 operation `reply` or `x-response` | The request's function returns the reply |
| Several channels | A client for each channel |
| Channel parameters, such as `/rooms/{roomId}` | The client takes them when it connects |
| `http` basic or bearer, `httpApiKey`, `oauth2`, `apiKey` or `userPassword` security | The client sends the credentials when it connects |
| A spec written from the server's side | Generates a client with `--spec-side service` |
| A message without a JSON Schema payload, or with its payload in another file | [Leaves the message out](#a-message-has-no-json-schema-payload), with a warning |
| A binary message | [Leaves the message out](#a-message-is-binary), with a warning |
| A reply on another channel, or a message the server expects the client to answer | [Warns](#a-reply-the-client-doesnt-wait-for) |
| A server for another protocol, such as `mqtt` or `kafka` | [Stops](#the-servers-use-another-protocol) |
| A Socket.IO endpoint, or a STOMP, SignalR or other framing named in the description | [Stops or warns](#the-server-speaks-a-framing-over-websocket) |
| Schemas that include each other through `allOf` | [Stops](#schemas-include-each-other) |

The tool doesn't support binary frames, text framings over WebSocket such as STOMP or Socket.IO, or messages that are JSON arrays told apart by position, such as `["EVENT", ...]`.

## Supported

### AsyncAPI versions and servers

AsyncAPI 2.0 to 2.6 and 3.x are read. The tool generates a WebSocket client for a spec with a `ws` or `wss` server. A server labelled `http` or `https`, as some WebSocket APIs are, needs `--protocol ws`:

```bash
bal asyncapi chat-api.yaml --protocol ws
```

### JSON messages told apart by a field

A connection carries every message on one stream of frames, so the client needs a field whose fixed value tells the received messages apart. Name it with `x-dispatcherKey`:

```yaml
x-dispatcherKey: type
components:
  messages:
    OrderAccepted:
      payload:
        type: object
        required: [type, orderId]
        properties:
          type: {type: string, const: order_accepted}
          orderId: {type: string}
```

The key can be inside an object (`x-dispatcherKey: /data/type`), or a message can name its own. Without `x-dispatcherKey`, the tool works the field out when exactly one fits, and tells the messages apart by a field only each has when no single field does. Each choice it makes is announced with a warning that shows the line to add. [How a WebSocket client tells messages apart](websocket-dispatcher-key.md) covers every case.

### One kind of message, no key

A connection that receives one kind of message needs no key, as every frame is that message. That message can be a JSON object, a string or a number. Messages the client sends never need a key.

### Requests with replies

A 3.0 send operation with a `reply` gets a function that sends the request and returns the reply:

```yaml
operations:
  placeOrder:
    action: send
    channel: {$ref: '#/channels/orders'}
    messages: [{$ref: '#/channels/orders/messages/PlaceOrder'}]
    reply:
      channel: {$ref: '#/channels/orders'}
      messages:
        - {$ref: '#/channels/orders/messages/OrderAccepted'}
        - {$ref: '#/channels/orders/messages/OrderRejected'}
```

```ballerina
OrderAccepted|OrderRejected result = check orders->doPlaceOrder({'type: "place_order", item: "apple"}, 5);
```

A reply without `messages` is any other message of its channel. In a 2.x spec, or on a message, `x-response` names the reply:

```yaml
PlaceOrder:
  payload: {$ref: '#/components/schemas/PlaceOrder'}
  x-response: {$ref: '#/components/messages/OrderAccepted'}
  x-response-type: simple-rpc
```

A message's own `x-response` wins over its operation's `reply`. When requests and replies name the same `correlationId` field, the client pairs each reply with its request on that field, so several requests can wait at once.

### Several channels and their parameters

Each channel is a connection of its own and gets a client of its own, named after the spec's title and the channel, such as `OrdersOrdersClient`, or after the title alone for a channel at `/`. A channel address with parameters, such as `/rooms/{roomId}`, gives the client a parameters record to connect with:

```ballerina
ChatRoomClient room = check new ({roomId: "general"});
```

### Security

The client sends the credentials of the server's security scheme when it connects:

| Scheme | How the client gets and sends it |
|---|---|
| `http` with `basic` or `bearer` | Set in the `auth` field of the client configuration, sent in the `Authorization` header |
| `httpApiKey` in a header or the query | Passed when the client is created, as `ApiKeysConfig`, and sent as that header or query parameter |
| `oauth2` | Client credentials set in the `auth` field of the client configuration |
| `apiKey` or `userPassword` | Set in the `auth` field as HTTP Basic credentials, with a warning |

### A spec written from the server's side

Some specs describe what the server does: it receives `Subscribe` and sends `Tick`. Read it from the server's side so the client sends `Subscribe` and receives `Tick`:

```bash
bal asyncapi prices.yaml --spec-side service
```

## Generated, with a part left out

These warnings don't stop generation. The client is generated without the part named, and the warning shows where it is.

### A message has no JSON Schema payload

```
warning: message 'Heartbeat' has no JSON Schema payload, so the client has no type to receive it as
  Give the message a JSON Schema 'payload' to get a type for it.
```

A message without a `payload`, or with an Avro or Protobuf payload, has no type to send or receive. Give it a JSON Schema payload:

```yaml
Heartbeat:
  payload:
    type: object
    properties:
      at: {type: string, format: date-time}
```

When the payload is in another file, the warning names the reference:

```
warning: message 'Tick' takes its payload from 'common.yaml#/Tick', another file, and the tool only reads references inside the spec, so the client has no type to receive it as
  Bundle the spec into one file first, for example with 'asyncapi bundle' from the AsyncAPI CLI, or copy the schema into components/schemas.
```

```bash
asyncapi bundle feed.yaml --output feed-bundled.yaml
bal asyncapi feed-bundled.yaml
```

### A message is binary

```
warning: message 'Frame' is binary (contentType 'image/png'), and the client sends and reads text frames, so it leaves the message out
  Binary WebSocket frames aren't supported. If the server also takes the data as JSON or text, describe that form of the message instead.
```

A message is binary when its `contentType` is binary, such as `application/octet-stream`, `image/png` or `application/x-protobuf`, or when its payload is a `format: binary` string. If the server also accepts the data as JSON, for example base64 inside an object, describe that form:

```yaml
Frame:
  payload:
    type: object
    required: [type, data]
    properties:
      type: {type: string, const: frame}
      data: {type: string, contentEncoding: base64}
```

### A received message can't be told apart

When the other messages a connection receives can be told apart but one can't, that one is left out:

```
warning: message 'Said' has no 'type' field, so the client can't tell it apart from the other messages and leaves it out
  Add a required 'type' string field with a fixed value to the message's payload, such as type: {type: string, const: said}.
```

Give it the key field with its own fixed value. [Messages without the key](websocket-dispatcher-key.md#messages-without-the-key) has the details.

### A reply the client doesn't wait for

Each channel is a separate connection, so a reply on another channel can't be paired with its request:

```
warning: operation 'ask' gets its reply on another channel, which the client connects to separately, so its function doesn't wait for the reply
  Receive the reply with the client of that channel.
```

The request's function sends it and returns; read the reply with the receive function on the other channel's client. If the reply really arrives on the same connection, point the reply's `channel` at the operation's own channel.

A server message that expects an answer from the client, such as a ping, is received as usual, and the client doesn't answer on its own:

```
warning: the server expects 'Pong' in reply to 'Ping', and the client doesn't reply on its own
  Send the reply with its function after the client receives the message.
```

```ballerina
Ping ping = check live->doPing(30);
check live->doPong({'type: "pong"}, 5);
```

## Generation stops

These errors stop generation before anything is written. The tool exits with code 1.

### Received messages can't be told apart

```
error: x-dispatcherKey extension is required in the AsyncAPI spec, and no field has a different fixed string value (a const or a one-value enum) in every message received on channel 'room', so the client can't tell them apart:
    - 'Joined', received at /operations/onJoined
    - 'Said', received at /operations/onSaid
  Give each received JSON object a field with its own fixed value, and name the field in x-dispatcherKey, such as:
    x-dispatcherKey: type
    Joined payload: type: {type: string, const: joined}
    Said payload: type: {type: string, const: said}
```

Make the change the error shows, or name a field your messages already carry. When several fields could be the key, the error names them; add `x-dispatcherKey` with the one the server routes on. [When generation stops](websocket-dispatcher-key.md#when-generation-stops) has more examples.

Messages that are JSON arrays told apart by position, such as `["EVENT", ...]` and `["NOTICE", ...]`, stop here too, as an array has no field to name. A connection that receives one kind of array works, as it needs no key.

### The servers use another protocol

```
bal asyncapi: error: the servers of 'sensors.yaml' use mqtt, which the tool doesn't generate for
  The tool generates webhook triggers (http) and WebSocket clients (ws or wss). If a server is a plain WebSocket endpoint, set its protocol to ws or wss.
```

The tool generates WebSocket clients only. If the API also has a plain WebSocket endpoint, describe that server with `protocol: wss`.

### The server speaks a framing over WebSocket

Socket.IO, STOMP, SignalR, SockJS, WAMP, Phoenix Channels and Action Cable wrap each message in their own framing, which the client doesn't speak. A server on a Socket.IO path stops generation:

```
bal asyncapi: error: server 'main' is a Socket.IO endpoint (example.com/socket.io/). Socket.IO wraps every message in its own framing, which a WebSocket client doesn't speak, so the client couldn't talk to it. If the server also takes plain WebSocket connections, point the server at that endpoint instead
```

A spec whose title or description names a framing is generated with a warning, as the client can only talk to the server if it also takes plain JSON frames:

```
warning: the spec's description names STOMP, and the client sends and reads plain JSON frames, so it can't talk to a server that speaks STOMP
  Generate from a spec of the server's plain WebSocket endpoint, if it has one; STOMP itself isn't supported.
```

### Schemas include each other

```
error: schemas 'Base' and 'JoinBody' include each other through allOf (Base -> JoinBody -> Base), and a Ballerina record can't include itself
  Remove 'Base' from the allOf of 'JoinBody' at /components/schemas/JoinBody, and add the fields it needs from 'Base' to its properties.
```

### Streamed replies without a key

A reply streamed with `x-response-type: streaming` is read from frames that every message shares, so it needs `x-dispatcherKey`:

```
error: x-dispatcherKey extension is required in the AsyncAPI spec to stream the replies of message 'Subscribe'. Add x-dispatcherKey naming the field that tells the messages apart.
```
