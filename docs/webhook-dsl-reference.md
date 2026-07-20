# Webhook Signature DSL Reference

This document describes the webhook verification DSL consumed from root-level `x-ballerina-auth` in AsyncAPI specs.

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

- `header`: the header carrying the request timestamp, as epoch milliseconds.
- `toleranceMillis`: the maximum allowed age of a request before it's rejected as stale.

The freshness check runs before signature verification and is independent of it — it's evaluated
whenever `freshness` is present, regardless of `algorithm`/`strategy`. Both fields are required if
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

## Backwards Compatibility

If `x-ballerina-auth` only declares `header` (no nested `signature` block), generation defaults to the
GitHub-style configuration: `algorithm: sha256`, `encoding: hex`, `headerFormat: {signature}`,
`input: $body`.
