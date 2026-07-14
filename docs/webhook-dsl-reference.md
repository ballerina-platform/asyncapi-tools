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

- `algorithm`: crypto algorithm used for HMAC verification (for example `sha256`).
- `encoding`: expected signature encoding from the header (for example `hex` or `base64`).
- `headerFormat`: template used to extract values from the signature header.
- `input`: DSL expression used to construct the signed payload before hashing.

If `algorithm` is omitted or blank, generation uses static token comparison fallback instead of HMAC.

## Payload Vocabulary

Allowed built-in input tokens:

- `$body`: raw request body payload.
- `$method`: HTTP method.
- `$uri`: request URI.

Header-derived variables become available in `input` when they are declared in `headerFormat`, and can
be referenced either as `$name` or `{name}` (for example `$timestamp` or `{timestamp}`).

Header lookup function:

- `$header('Header-Name')`: includes a request header value directly in input.

Concatenation:

- Dot-style concatenation is supported: `$timestamp . '.' . $body`

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

## Backwards Compatibility

If `x-ballerina-auth` only declares `header` (no nested `signature` block), generation defaults to the
GitHub-style configuration: `algorithm: sha256`, `encoding: hex`, `headerFormat: {signature}`,
`input: $body`.
