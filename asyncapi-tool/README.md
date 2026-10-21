## Overview

The AsyncAPI contract is a specification that creates an event-driven architecture based contract for APIs by detailing all of its resources and channels in a human and machine-readable format for easy development, discovery, and integration. The Ballerina AsyncAPI tool generates Ballerina code from an AsyncAPI definition, and AsyncAPI definitions from Ballerina code.

What the tool generates depends on the protocol and on what you give it as input:

| Protocol | Input | Output |
|----------|-------|--------|
| `http` | An AsyncAPI definition | A webhook listener: event types, service types, the listener and the dispatcher |
| `ws` | An AsyncAPI definition | A WebSocket client |
| `ws` | A Ballerina package or a standalone `.bal` file | An AsyncAPI definition of its WebSocket services |

The tool requires Ballerina Swan Lake Update 13 (2201.13.0) or later.

## Usage

```bash
bal asyncapi <input> --protocol <http|ws> [options]
```

`<input>` decides the direction:

- An AsyncAPI definition (`.yaml`, `.yml` or `.json`) generates Ballerina code.
- A Ballerina package directory or a standalone `.bal` file generates an AsyncAPI definition. A `.bal` file that belongs to a package is not accepted; pass the package directory instead, as `bal run` does.

### Options

| Option | Description |
|--------|-------------|
| `--protocol <http\|ws>` | Protocol of the AsyncAPI definition. Required. |
| `-o`, `--output <dir>` | Directory the generated files are written to. Defaults to the current directory. |
| `--module <name>` | Module of the Ballerina package to read services from, or to write generated code into. Not used with a standalone `.bal` file. |
| `--license <file>` | File with the license header placed on generated files. Supported by `ws` only. |
| `--format <yaml\|json>` | Format of a generated AsyncAPI definition. Defaults to `yaml`. |
| `--service <base-path>` | Base path of the service to generate the definition for, for example `/chat`. Every service with that base path is generated. |
| `--with-tests` | Generate test files along with the code. For `http`, the definition needs `x-ballerina-auth`. |
| `--force` | Overwrite files that already exist in the output location. |
| `--dry-run` | Show which files would be created, overwritten or skipped, without writing anything. |

An option that does not apply to what you are generating is ignored with a warning, for example `--license` with `--protocol http`, or `--format` when generating code. Run `bal asyncapi --help` for the same list.

### Existing files

The tool never overwrites a file you have edited unless you ask it to. For every file it generates:

- a file that does not exist yet is created;
- a file that already has the same content is left alone;
- a file with different content is skipped with a warning, unless `--force` is given.

```
$ bal asyncapi slack-events.yaml --protocol http
warning: Skipped "types.bal": the file already exists. Use --force to overwrite it.
No files were written.
warning: 1 file(s) skipped, so the generated code may be out of sync with the definition. Use --dry-run to see what differs, or --force to overwrite.
```

Skipping a file leaves the output out of sync with the definition, so a run that skips any file exits with a non-zero code. A script that regenerates code, such as a CI workflow, should pass `--force`.

To see what a run would do before running it, add `--dry-run`:

```
$ bal asyncapi slack-events.yaml --protocol http --dry-run --force
Dry run: no files were written.

  unchanged  dispatcher_service.bal
  unchanged  listener.bal
  unchanged  service_types.bal
  overwrite  types.bal

0 to create, 1 to overwrite, 0 to skip, 3 unchanged.
```

Generated Ballerina code is written into a Ballerina package, so run the command inside one (create it with `bal new` first), or point `-o` at one.

## Examples

### Generate a webhook listener from an AsyncAPI definition

```bash
# Into the package in the current directory
bal asyncapi slack-events.yaml --protocol http

# Into another package, replacing files generated earlier
bal asyncapi slack-events.yaml --protocol http -o ./slack-listener --force

# Into a module of the package
bal asyncapi slack-events.yaml --protocol http --module events
```

### Generate a WebSocket client from an AsyncAPI definition

```bash
bal asyncapi chat-api.yaml --protocol ws

# With tests and a license header
bal asyncapi chat-api.yaml --protocol ws -o ./chat-client --with-tests --license ./license.txt
```

### Generate an AsyncAPI definition from Ballerina code

```bash
# Every WebSocket service in the package in the current directory
bal asyncapi . --protocol ws

# One service, as JSON
bal asyncapi . --protocol ws --service /chat --format json -o ./specs

# Services in one module of the package
bal asyncapi . --protocol ws --module chat

# A standalone file
bal asyncapi chat_service.bal --protocol ws
```

## Webhook listeners

### Sample AsyncAPI definition

```yaml
asyncapi: 3.0.0
info:
  title: Slack Events
  version: 1.0.0
x-ballerina-event-identifier:
  type: body
  path: event.type
channels:
  app:
    address: app
    messages:
      appMention:
        x-ballerina-event-type: app_mention
        summary: Subscribe to only the message events that mention your app or bot
        payload:
          $ref: "#/components/schemas/GenericEventWrapper"
      appRateLimited:
        x-ballerina-event-type: app_rate_limited
        summary: Indicates that your app's event subscriptions are being rate limited
        payload:
          $ref: "#/components/schemas/GenericEventWrapper"
operations:
  app:
    action: receive
    channel:
      $ref: "#/channels/app"
    messages:
      - $ref: "#/channels/app/messages/appMention"
      - $ref: "#/channels/app/messages/appRateLimited"
components:
  schemas:
    GenericEventWrapper:
      title: Standard event wrapper for the Events API
      type: object
      required:
        - event
      properties:
        event:
          title: The actual event, an object, which happened
          type: object
          required:
            - type
            - event_ts
          properties:
            type:
              title: The specific name of the event
              type: string
            event_ts:
              title: When the event was dispatched
              type: string
            text:
              title: The message content
              type: string
```

Every event the listener should handle is a message of a channel, and every channel is listed in a `receive` operation.

The extensions starting with `x-ballerina` tell the tool how to dispatch events. They must be added to the definition before using the tool.

1\. `x-ballerina-event-identifier` - When the listener receives an event, there should be a way to identify the event type. This has two parts, `type` and `path`.

- `type` - Either `header` or `body`: whether the event type arrives as an HTTP header or as an attribute in the body.
- `path` - The header name if the type is `header`, or the JSON path of the attribute if the type is `body`.

2\. `x-ballerina-event-type` - This should be there in every message inside the channel. It is the value the identifier above has for that event.

3\. `x-ballerina-event-label` (optional) - By default, the generated remote function's name and doc comment are derived directly from `x-ballerina-event-type`. This works well when that value is itself a clean, readable string (e.g. `company.propertyChange`), but some real event sources use type strings with a required prefix/suffix that only makes sense for matching (e.g. a CloudEvents-style `qbo.account.merged.v1`), which produces an awkward function name if used for naming too (`onQboAccountMergedV1`). Set `x-ballerina-event-label` on a message to control the generated name independently (e.g. `AccountMerged` produces `onAccountMerged`), without changing the value actually compared against incoming payloads.

4\. `x-ballerina-auth` (optional) - How the listener verifies that a request really comes from the event source, for example an HMAC signature header. See the [webhook signature DSL reference](https://github.com/ballerina-platform/asyncapi-tools/blob/main/docs/webhook-dsl-reference.md).

### Generated files

Generating a listener writes these files:

1. `types.bal` - the Ballerina data types extracted from the AsyncAPI definition
2. `service_types.bal` - the service types for the events described in the definition
3. `listener.bal` - the HTTP listener that receives the events
4. `dispatcher_service.bal` - the logic that dispatches each event to its remote function

With `--with-tests`, the tool also writes `tests/dispatch_test.bal`, which sends signed sample events to the listener and checks that each reaches its remote function. The events are signed, so this needs `x-ballerina-auth` in the definition; without it the tool warns and writes no tests.

When modifying the generated code, keep the following in mind.

- All the incoming requests are received by the resource method in the `dispatcher_service.bal` file. Hence, if there is a necessity to add an authentication logic for the incoming calls, that logic can be included there before processing the incoming HTTP request.
- If more information is needed when initializing the listener such as secrets, endpoint URLs, tokens, refresh tokens, etc., update the `init` function in the `listener.bal` file.

### Using the generated listener

1\. Create a package with `bal new slack_listener` and generate the listener into it:

```bash
cd slack_listener
bal asyncapi slack-events.yaml --protocol http
```

2\. Add a service for the events to `main.bal`:

```ballerina
listener Listener webhookListener = new ({}, 8090);

service AppService on webhookListener {
    remote function onAppMention(GenericEventWrapper event) returns error? {
        // Implement the logic to use the received `event` here.
    }

    remote function onAppRateLimited(GenericEventWrapper event) returns error? {
        // Implement the logic to use the received `event` here.
    }
}
```

3\. Execute `bal run`.

### Choosing the path

A service without a path receives events at `/`. Give it a path to receive them somewhere else. This also lets several triggers, or your own HTTP services, share one port:

```ballerina
listener http:Listener httpListener = new (8090);
listener github:Listener githubListener = new ({webhookSecret: githubSecret}, httpListener);
listener slack:Listener slackListener = new ({verificationToken: slackToken}, httpListener);

service github:PushService /github on githubListener { ... }   // http://host:8090/github
service slack:AppService /slack on slackListener { ... }       // http://host:8090/slack
```

Services on the same path share its dispatcher, so each service type can be attached once per path. Detaching the last service on a path frees that path.

## Exporting trigger endpoints

`bal build --export-endpoints` lists the ports and paths a program listens on in `target/artifact/endpoints.yaml`. The `ballerina/http` compiler plugin only reports services attached to an `http:Listener`, so each trigger package ships its own compiler plugin to report services attached to its `Listener`, with the same rules and defaults as HTTP services (`type: "REST"`, an empty `schemaPath`).

As `ballerina/http` does, the trigger repo builds the plugin from source and a `CompilerPlugin.toml` next to `Ballerina.toml` points at the built jar, so nothing is published separately:

```toml
[plugin]
id = "asyncapi-trigger-compiler-plugin"
class = "io.ballerina.asyncapi.trigger.plugin.TriggerCompilerPlugin"

[[dependency]]
path = "../compiler-plugin/build/libs/github.trigger-compiler-plugin-0.2.0.jar"
```

The plugin source and the files a trigger repo needs for it are in [`trigger-template`](../trigger-template), which the trigger scaffold copies into each repo.
