# Ballerina AsyncAPI Tools

[![Build](https://github.com/ballerina-platform/asyncapi-tools/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/asyncapi-tools/actions/workflows/build-timestamped-master.yml)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/asyncapi-tools.svg)](https://github.com/ballerina-platform/asyncapi-tools/commits/master)
[![GitHub issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-standard-library/module/asyncpi-tools.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-library/labels/module%2Fasyncapi-tools)

An [AsyncAPI](https://www.asyncapi.com/) definition describes an event-driven API: its channels, the messages on each,
and who sends and receives them. The `bal asyncapi` tool generates Ballerina code from an AsyncAPI definition, and
AsyncAPI definitions from Ballerina code:

| From | To |
|------|----|
| A webhook definition (`http`) | A webhook listener that verifies each event and dispatches it to a remote function |
| A WebSocket definition (`ws`) | A WebSocket client for each channel |
| A Ballerina WebSocket service | Its AsyncAPI definition |

Install the tool with Ballerina Swan Lake Update 13 (2201.13.0) or later:

```bash
bal tool pull asyncapi
bal asyncapi --help
```

## Documentation

* [Using the tool](asyncapi-tool/README.md): options, examples, webhook listeners and WebSocket clients
* [Webhook signature DSL reference](docs/webhook-dsl-reference.md): the `x-ballerina` extensions that describe how a
  webhook is verified and dispatched
* [Which WebSocket specs the tool generates a client for](docs/websocket-spec-support.md): what is supported, the
  warnings and errors, and how to fix a spec
* [How a WebSocket client tells messages apart](docs/websocket-dispatcher-key.md): `x-dispatcherKey` and how the tool
  works the key out

## Repository layout

| Directory | Contents |
|-----------|----------|
| `asyncapi-core` | Reads AsyncAPI 2.x and 3.x definitions into one model |
| `asyncapi-generator` | Generates webhook listeners, WebSocket clients and definitions |
| `asyncapi-cli` | The `bal asyncapi` command |
| `asyncapi-tool` | The Ballerina tool package published to Ballerina Central |
| `asyncapi-trigger-plugin` | The compiler plugin generated triggers ship, to export their endpoints |
| `native.handler` | The runtime library generated webhook listeners use |

## Building from the Source

### Setting Up the Prerequisites

1. OpenJDK 21 ([Adopt OpenJDK](https://adoptopenjdk.net/) or any other OpenJDK distribution)

   >**Info:** You can also use [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html). Set the JAVA_HOME environment variable to the pathname of the directory into which you installed JDK.

2. Export GitHub Personal access token with read package permissions as follows,
   ```
   export packageUser=<Username>
   export packagePAT=<Personal access token>
   ```

### Building the Source

Execute the commands below to build from the source.

1. To build the library:

        ./gradlew clean build

2. To run the integration tests:

        ./gradlew clean test

3. To build the module without the tests:

        ./gradlew clean build -x test

4. To publish to maven local:

        ./gradlew clean build publishToMavenLocal

## Contributing to Ballerina

As an open-source project, Ballerina welcomes contributions from the community.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of Conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful Links

* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
