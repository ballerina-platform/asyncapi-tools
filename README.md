# Ballerina AsyncAPI Tools

[![Build](https://github.com/ballerina-platform/asyncapi-tools/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/asyncapi-tools/actions/workflows/build-timestamped-master.yml)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/asyncapi-tools.svg)](https://github.com/ballerina-platform/asyncapi-tools/commits/master)
[![GitHub issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-standard-library/module/asyncpi-tools.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-library/labels/module%2Fasyncapi-tools)

The AsyncAPI Specification is a specification, which creates an event driven architecture based contract for APIs
detailing all of its resources and channels in both human and machine-readable format for easy development, discovery,
and integration. AsyncAPI tools currently s the following capabilities.

1. Generate Ballerina client code from a given AsyncAPI contract with `http` and `ws` protocols.
2. Export the AsyncAPI definition of a Ballerina service using the `ws` protocol.
3. The asyncapi command in Ballerina is used for AsyncAPI to Ballerina and Ballerina to AsyncAPI code generations.

The `asyncapi` command in Ballerina is used for AsyncAPI to Ballerina and Ballerina to AsyncAPI code generations.

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
