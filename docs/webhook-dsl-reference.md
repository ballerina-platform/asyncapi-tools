# Webhook Signature DSL Reference

This document describes the webhook verification DSL consumed from root-level `x-ballerina-auth` in AsyncAPI specs,
and how endpoint verification handshakes are described with AsyncAPI's request/reply pattern (see
[Verification Handshakes](#verification-handshakes)).

## Location

Define webhook authentication config at the AsyncAPI root:

```yaml
x-ballerina-auth:
  header: "X-Hub-Signature-256"
  signature:
    algorithm: "sha256"
    encoding: "hex"
    headerFormat: "sha256=${signature}"
    input: "$body"
```

## Event Identifiers per Channel

`x-ballerina-event-identifier` is normally declared once, at the root, and the listener serves every channel at
`/`. When channels name their events differently, give a channel its own identifier. Twilio, for example, puts the
call status in `CallStatus` and the message status in `MessageStatus`, and both use values such as `failed`:

```yaml
channels:
  callStatus:
    address: callStatus
    x-ballerina-event-identifier:
      type: body
      path: CallStatus
  smsStatus:
    address: smsStatus
    x-ballerina-event-identifier:
      type: body
      path: MessageStatus
```

- As soon as any channel declares its own identifier, the listener serves **each channel at its own path**, its
  `address` (`/callStatus`, `/smsStatus`), instead of one resource at `/`. Each path reads the event type with that
  channel's identifier and dispatches only to that channel's service, so a `failed` call can never reach the
  messaging service. Register each path with the provider separately.
- A channel without its own identifier uses the root one. Generation fails if a channel has neither.
- A channel's identifier must be `body` or `header`. Batched payloads are not supported with channel identifiers.
- Each address must be one or more path segments made of letters, digits, `_` or `-`, starting with a letter or
  `_`, and no two channels may share an address.
- `$uri` in a signature `input` is the request path, so it is each channel's own path.
- The generated dispatch tests post each event to its channel's path, read its payload from a folder that follows the
  channel's address (for example `tests/resources/trigger_payloads/callStatus/failed.json`, or
  `trigger_payloads/voice/status/failed.json` for `address: voice/status`), and check that only that channel's
  remote function fired.

## Signature Config

Fields under `x-ballerina-auth.signature`:

- `algorithm`: crypto algorithm used for verification (for example `sha256`).
- `encoding`: expected signature encoding from the header (for example `hex` or `base64`).
- `headerFormat`: template used to extract values from the signature header.
- `input`: DSL expression used to construct the signed payload before hashing.
- `strategy`: `"hmac"` (default, if omitted) for a secret-keyed HMAC, or `"hash"` for a plain,
  unkeyed digest. In `hash` mode the secret is **not** used as a key — it must be folded into
  `input` explicitly via the `$secret` token (see [Non-HMAC / Plain-Hash Schemes](#non-hmac--plain-hash-schemes)).

If `algorithm` is omitted or blank, generation uses static token comparison fallback instead of HMAC.

## Payload Vocabulary

Allowed built-in input tokens:

- `$body`: raw request body payload.
- `$method`: HTTP method.
- `$uri`: request URI path (`request.rawPath` — does **not** include scheme/host; see
  [Config-Derived Values](#config-derived-values) if a provider signs the full external URL).
- `$secret`: the configured webhook secret itself, for schemes that fold it directly into the
  hashed payload instead of using it as an HMAC key (see `strategy: "hash"` above).
- `$formParams`: the form parameters of a form-encoded body, sorted by name and each appended as name then value
  (see [Form-Encoded Bodies](#form-encoded-bodies)).

Header-derived variables become available in `input` when they are declared in `headerFormat`, and can
be referenced either as `$name` or `{name}` (for example `$timestamp` or `{timestamp}`).

Header lookup function:

- `$header('Header-Name')`: includes a request header value directly in input.

Concatenation:

- Dot-style concatenation is supported: `$timestamp . '.' . $body`

## Config-Derived Values

Some providers sign a value that isn't derived from the incoming request at all — for example
HubSpot's v3 signature includes the externally-visible callback URL the webhook was registered
against, which the server can't reconstruct from `request.rawPath` alone. The `$config('name')`
token references a statically-configured value instead:

```yaml
input: "$method . $config('callbackUrl') . $body"
```

Each distinct `$config('name')` referenced in `input` becomes a required `string` field on the
generated `ListenerConfiguration` (alongside `webhookSecret`), threaded through to the listener at
initialization — so `callbackUrl` above must be supplied when constructing the listener, the same
way `webhookSecret` is today.

## Freshness (Replay-Window) Checks

A signature check alone doesn't protect against replay of an old, still-validly-signed request.
Some providers (HubSpot, Stripe) additionally require rejecting requests whose timestamp is older
than a tolerance window. Declare this with a `freshness` block, a sibling of `signature`:

```yaml
x-ballerina-auth:
  header: "X-HubSpot-Signature-v3"
  signature:
    ...
  freshness:
    header: "X-HubSpot-Request-Timestamp"
    toleranceMillis: 300000
```

- `header`: the header carrying the request timestamp, as epoch milliseconds by default.
- `toleranceMillis`: the maximum allowed age of a request before it's rejected as stale.
- `unit` (optional): `milliseconds` (default) or `seconds`, the unit of the timestamp in `header`. Slack
  sends epoch seconds; with `unit: seconds` the generated check scales the value to milliseconds before
  comparing it, and the generated test client sends a seconds timestamp.

The freshness check runs before signature verification and is independent of it — it's evaluated
whenever `freshness` is present, regardless of `algorithm`/`strategy`. `header` and `toleranceMillis` are required if
the block is present.

## Non-HMAC / Plain-Hash Schemes

A few providers (e.g. older HubSpot v1/v2 webhooks) don't use a keyed HMAC at all — instead they
compute a plain digest over the secret concatenated with the payload. Set `strategy: "hash"` and
fold the secret into `input` explicitly with `$secret`:

```yaml
x-ballerina-auth:
  header: "X-HubSpot-Signature"
  signature:
    strategy: "hash"
    algorithm: "sha256"
    encoding: "hex"
    headerFormat: "$signature"
    input: "$secret . $body"
```

This computes `hash = SHA256(secret + body)` (no HMAC key) and compares it against the header,
instead of `HMAC-SHA256(body, secret)`.

## Public-Key (RSA) Signatures

Some providers sign each payload with a private key and publish the matching public key, so there is no
shared secret to key an HMAC with. Set `strategy: "rsa"` and an `algorithm`:

```yaml
x-ballerina-auth:
  header: "mftg-wh-signature"
  signature:
    strategy: "rsa"
    algorithm: "sha256"
    encoding: "base64"
    headerFormat: "$signature"
    input: "$body"
```

The generated verifier decodes the signature header per `encoding` and checks it against the bytes built
from `input` with `crypto:verifyRsaSha256Signature` (or the Sha1/Sha384/Sha512 variant). A malformed or
non-matching signature is rejected as a signature mismatch.

The listener's `webhookSecret` field carries the provider's **X.509 certificate in PEM form** (the one
holding the public key), not a shared secret. A bare `-----BEGIN PUBLIC KEY-----` PEM is not accepted,
because `ballerina/crypto` can only decode a public key from a certificate. If the provider only
publishes a bare public key, wrap it in a certificate first.

`strategy` must be one of `hmac` (the default), `hash` or `rsa`; any other value fails generation.

The generated dispatch tests sign with a fixed, throwaway key pair embedded in the test file and
configure the test listener with its certificate, so the suite exercises real verification.

## Form-Encoded Bodies

Some providers send webhooks as `application/x-www-form-urlencoded` instead of JSON. Twilio, for example, sends
`MessageSid=SM123&MessageStatus=delivered&To=%2B15551234567`. Declare it with the standard AsyncAPI content type,
on each message or for the whole document:

```yaml
defaultContentType: application/x-www-form-urlencoded
```

- The listener reads the form parameters into a JSON object and then identifies and dispatches the event exactly
  as for a JSON body, so `x-ballerina-event-identifier` names a parameter (for example `path: MessageStatus`).
- Every parameter arrives as a string. Top-level payload fields the schema types as `integer`, `number` or `boolean`
  are converted to the type the generated record field has; a value that does not convert gets `400 Bad Request`.
- All messages must use the same content type, and form bodies do not support batched payloads.

Providers that sign form bodies usually sign the parameters, not the raw body. `$formParams` is the parameters
sorted by name (case-sensitive, byte order) and each appended as name then value, with no separators and with
values decoded. Twilio signs the full URL followed by these parameters, with HMAC-SHA1 and Base64:

```yaml
x-ballerina-auth:
  header: X-Twilio-Signature
  signature:
    algorithm: sha1
    encoding: base64
    headerFormat: "$signature"
    input: "$config('webhookUrl') . $formParams"
```

`webhookUrl` becomes a field of the listener configuration, set to the URL registered with Twilio (see
[Config-Derived Values](#config-derived-values)). `$formParams` can only be used when the messages are form-encoded.
The generated dispatch tests send each JSON fixture's fields as form parameters and sign them the same way.

## Header Format Placeholders

`headerFormat` supports these placeholder syntaxes:

- `$name`
- `${name}`
- `{name}`

Example:

```yaml
headerFormat: "t=${timestamp},v1=${signature}"
```

Validation rules:

- Adjacent placeholders are not allowed (for example `$ts$signature`).
- When `algorithm` is configured, `headerFormat` must include signature placeholder `$signature`, `${signature}`, or `{signature}`.

## Verification Handshakes

Some providers check the endpoint before they send any events: they send a challenge and only activate the
webhook once the endpoint returns it. Describe the handshake with AsyncAPI's own request/reply pattern: a `receive`
operation with a `reply`. The generated listener answers it itself; it never reaches a service, and no remote
function is generated for it.

For example, Slack's `url_verification`:

```yaml
channels:
  events:
    address: events
    messages:
      urlVerification:
        payload:
          type: object
          properties:
            type:
              type: string
              const: url_verification
            challenge:
              type: string
        correlationId:
          location: $message.payload#/challenge
      urlVerificationReply:
        payload:
          type: string
        bindings:
          http:
            statusCode: 200
operations:
  verifyUrl:
    action: receive
    channel:
      $ref: '#/channels/events'
    messages:
      - $ref: '#/channels/events/messages/urlVerification'
    reply:
      channel:
        $ref: '#/channels/events'
      messages:
        - $ref: '#/channels/events/messages/urlVerificationReply'
```

- A `receive` operation with a `reply` is the handshake. A definition can have one.
- The `const` values in the request message's payload schema tell the handshake apart from events: a request whose
  body has every one of them is the handshake. At least one is required.
- The request message's `correlationId` points at the challenge, as a runtime expression into the payload
  (`$message.payload#/challenge`). The listener responds with the challenge as plain text, or `400` if it is
  missing.
- The reply message's HTTP binding `statusCode` is the response status; without it, `200`.
- The handshake is answered after the `x-ballerina-auth` signature check, so a provider that signs it (Slack does)
  is verified like any other request.
- The challenge in a header is not supported yet.

**WebSub verification of intent** (for example WSO2 Identity Server and Asgardeo): the hub sends
`GET /?hub.mode=subscribe&hub.topic=...&hub.challenge=...` and the subscriber confirms by echoing `hub.challenge`.
A runtime expression cannot point into the query, so a `GET` handshake is read as the
[W3C WebSub](https://www.w3.org/TR/websub/#hub-verifies-intent) verification: describe it with the operation's
HTTP binding, and the challenge is the `hub.challenge` query parameter.

```yaml
operations:
  verifyIntent:
    action: receive
    channel:
      $ref: '#/channels/events'
    bindings:
      http:
        method: GET
        query:
          type: object
          required:
            - hub.mode
            - hub.topic
            - hub.challenge
          properties:
            hub.mode:
              type: string
              const: subscribe
            hub.topic:
              type: string
            hub.challenge:
              type: string
            hub.lease_seconds:
              type: integer
    reply:
      channel:
        $ref: '#/channels/events'
      messages:
        - $ref: '#/channels/events/messages/verificationReply'
```

- The listener gets a `get` resource that responds with `hub.challenge` as plain text, or `400` if it is missing.
- The listener confirms a request whose query has the schema's `const` values (here `hub.mode: subscribe`) and
  every parameter the schema marks `required` (here `hub.topic`). Any other request gets `404 Not Found`, which
  WebSub defines as the subscriber declining. The listener keeps no subscription state, so it cannot check that
  `hub.topic` is one it subscribed to.
- A `GET` has no body, so it is not signature-checked.
- The query schema must have `hub.challenge`; any method other than `GET` or `POST` fails generation.

When dispatch tests are generated, they include `testVerificationHandshake`, which sends the handshake (signed, for
a `POST`) and checks the status code and that the challenge comes back. A WebSub verification also gets
`testVerificationHandshakeIsNotConfirmedForOtherRequests` (another `hub.mode` gets `404`) and
`testVerificationHandshakeNeedsEveryRequiredParameter` (a missing required parameter gets `404`).

## Examples

### Stripe-style signed payload

```yaml
x-ballerina-auth:
  header: "Stripe-Signature"
  signature:
    algorithm: "sha256"
    encoding: "hex"
    headerFormat: "t=${timestamp},v1=${signature}"
    input: "$timestamp . '.' . $body"
```

### Slack-style base string

```yaml
x-ballerina-auth:
  header: "X-Slack-Signature"
  signature:
    algorithm: "sha256"
    encoding: "hex"
    headerFormat: "v0=${signature}"
    input: "'v0' . ':' . $header('X-Slack-Request-Timestamp') . ':' . $body"
```

### GitLab-style static token fallback

```yaml
x-ballerina-auth:
  header: "X-Gitlab-Token"
  signature:
    headerFormat: "$signature"
```

In static-token mode, generated verification performs constant-time comparison between the incoming token and configured secret.

### HubSpot v3: config-derived URL + freshness check

HubSpot's v3 signature is `HMAC-SHA256("POST" + callbackUrl + body + timestamp)`, base64-encoded,
where `callbackUrl` is the externally-registered webhook URL (not derivable from the request) and
requests older than 5 minutes must be rejected outright:

```yaml
x-ballerina-auth:
  header: "X-HubSpot-Signature-v3"
  signature:
    algorithm: "sha256"
    encoding: "base64"
    headerFormat: "$signature"
    input: "$method . $config('callbackUrl') . $body . $header('X-HubSpot-Request-Timestamp')"
  freshness:
    header: "X-HubSpot-Request-Timestamp"
    toleranceMillis: 300000
```

### Slack: timestamp in the signature, freshness in seconds

Slack signs `v0:<timestamp>:<body>` with HMAC-SHA256 (hex, `v0=` prefix), where the timestamp is the
`X-Slack-Request-Timestamp` header in epoch seconds, and recommends rejecting requests older than
5 minutes:

```yaml
x-ballerina-auth:
  header: "X-Slack-Signature"
  signature:
    algorithm: "sha256"
    encoding: "hex"
    headerFormat: "v0=$signature"
    input: "'v0' . ':' . $header('X-Slack-Request-Timestamp') . ':' . $body"
  freshness:
    header: "X-Slack-Request-Timestamp"
    toleranceMillis: 300000
    unit: "seconds"
```

### HubSpot v1/v2: plain hash, no HMAC

Legacy HubSpot webhooks hash the secret and body directly, with no HMAC key and no header prefix:

```yaml
x-ballerina-auth:
  header: "X-HubSpot-Signature"
  signature:
    strategy: "hash"
    algorithm: "sha256"
    encoding: "hex"
    headerFormat: "$signature"
    input: "$secret . $body"
```

## Event Named by a Key (`body-key`)

`x-ballerina-event-identifier` normally reads the event type from a value: a header (`type: header`), a body
field (`type: body`), or both (`type: composite`). Some providers name the event by a **key** instead. WSO2 Identity
Server and Asgardeo send one event per delivery, keyed by its URI:

```json
{
  "iss": "https://api.asgardeo.io/t/myorg",
  "events": {
    "https://schemas.identity.wso2.org/events/login/event-type/loginSuccess": { "action": "LOGIN" }
  }
}
```

For these, use `type: body-key` with the path of the object whose key names the event:

```yaml
x-ballerina-event-identifier:
  type: body-key
  path: events
```

- The listener reads the object at `path` and uses its single key as the event type, matched against each
  message's `x-ballerina-event-type` (here, the full URI).
- A delivery where that object is missing, is not an object, or does not have exactly one key gets `400 Bad Request`.
- Use `x-ballerina-event-label` on each message to name its remote function, since a URI makes a poor function
  name (for example `LoginSuccess` gives `onLoginSuccess`).
- `body-key` does not support batched payloads; generation fails if the payload is an array of events.

The generated dispatch tests name each payload file after the message's key in `channels` (for example
`tests/resources/trigger_payloads/loginSuccess.json`), because an event type such as a URI cannot be a file name,
and check that each event reaches only its own remote function.

## Backwards Compatibility

If `x-ballerina-auth` only declares `header` (no nested `signature` block), generation defaults to the
GitHub-style configuration: `algorithm: sha256`, `encoding: hex`, `headerFormat: {signature}`,
`input: $body`.
