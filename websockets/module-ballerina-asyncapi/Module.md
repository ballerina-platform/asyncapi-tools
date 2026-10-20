## Module Overview

This module provides the Ballerina AsyncAPI tooling, which creates AsyncAPI definitions from Ballerina WebSocket services.

The AsyncAPI tools currently provide the following capabilities. 

  1. Export the AsyncAPI definition of a Ballerina service.

The `asyncapi` command in Ballerina is used for Ballerina to AsyncAPI code generations.

### Ballerina to AsyncAPI
#### Service to AsyncAPI Export
```bash
    bal asyncapi <package-directory | ballerina-file-path> --protocol ws
               [(-o|--output) <output-directory>]
               [--module <module-name>]
               [--format <yaml|json>]
```
Export the Ballerina services to AsyncAPI Specification 3.0.0 definitions. The input is a Ballerina package directory,
or a standalone `.bal` file that is not part of a package. For the export to work properly, the input Ballerina service
should be defined using the basic service level @websocket:ServiceConfig annotation with the dispatcherKey.

If you need to document an AsyncAPI contract for only one given service, then use this command.
```bash
    bal asyncapi <package-directory | ballerina-file-path> --protocol ws --service <base-path>
```

### Samples for AsyncAPI Commands

#### Generate an AsyncAPI Contract from a Service

 ```bash
    bal asyncapi . --protocol ws --module helloworld
  ```
This will generate the AsyncAPI contracts for the Ballerina services in the `helloworld` module of the package in the
current directory.
 ```bash
    bal asyncapi . --protocol ws --module helloworld --service /helloworld
  ```
This command will generate the `helloworld_asyncapi.yaml` file for the service with the `/helloworld` base path.
 ```bash
    bal asyncapi . --protocol ws --module helloworld --format json
  ```
This `--format json` option can be used with the Ballerina to AsyncAPI command to generate JSON files instead of
YAML files.
