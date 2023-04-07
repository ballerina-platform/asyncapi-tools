# Ballerina-AsyncAPI

[//]: # ([![Build master]&#40;https://github.com/ballerina-platform/openapi-tools/actions/workflows/build-timestamped-master.yml/badge.svg&#41;]&#40;https://github.com/ballerina-platform/openapi-tools/actions/workflows/build-timestamped-master.yml&#41;)

[//]: # ([![Daily build]&#40;https://github.com/ballerina-platform/openapi-tools/workflows/Daily%20build/badge.svg&#41;]&#40;https://github.com/ballerina-platform/openapi-tools/actions?query=workflow%3A%22Daily+build%22&#41;)

[//]: # ([![GitHub Last Commit]&#40;https://img.shields.io/github/last-commit/ballerina-platform/openapi-tools.svg&#41;]&#40;https://github.com/ballerina-platform/openapi-tools/commits/master&#41;)

[//]: # ([![codecov]&#40;https://codecov.io/gh/ballerina-platform/openapi-tools/branch/master/graph/badge.svg&#41;]&#40;https://codecov.io/gh/ballerina-platform/openapi-tools&#41;)

The AsyncAPI Specification is a specification, which creates a Event driven architecture based contract for APIs detailing all of its services , producers ,consumers ,channels  etc
in both human and machine-readable format for easy development, discovery, and integration. Ballerina
AsyncAPI tooling will make it currently easy for users to export AsyncAPI definition of a Ballerina service. The AsyncAPI tools provide the following capabilities.

 1. Export the AsyncAPI definition of a Ballerina service.

The `asyncapi` command in Ballerina is used for Ballerina to AsyncAPI code generations.

## Building from the Source

### Setting Up the Prerequisites

1. OpenJDK 11 ([Adopt OpenJDK](https://adoptopenjdk.net/) or any other OpenJDK distribution)

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

[//]: # (4. To publish to maven local:)

[//]: # ()
[//]: # (        ./gradlew clean build publishToMavenLocal)

## Contributing to Ballerina

As an open-source project, Ballerina welcomes contributions from the community.

[//]: # (You can also check for [open issues]&#40;https://github.com/ballerina-platform/openapi-tools/issues&#41; that)

[//]: # (interest you. We look forward to receiving your contributions.)

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of Conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful Links

* Discuss about code changes of the Ballerina project in [ballerina-dev@googlegroups.com](mailto:ballerina-dev@googlegroups.com).
* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
* View the [Ballerina performance test results](https://github.com/ballerina-platform/ballerina-lang/blob/master/performance/benchmarks/summary.md).

