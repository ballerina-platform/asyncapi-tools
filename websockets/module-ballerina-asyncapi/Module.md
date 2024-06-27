## Module Overview

This module provides the Ballerina AsyncAPI tooling, which will currently creates AsyncAPI definitions according to given Ballerina service file

The AsyncAPI tools currently provide the following capabilities. 

  1. Export the AsyncAPI definition of a Ballerina service.

The `asyncapi` command in Ballerina is used for Ballerina to AsyncAPI code generations.

### Ballerina to AsyncAPI
#### Service to AsyncAPI Export
```bash
    bal asyncapi -i <ballerina-file-path> 
               [(-o|--output) output asyncapi file path]
               [--protocol <protocol>]
```
Export the Ballerina service to an  AsyncAPI Specification 2.5.0 definition. For the export to work properly, 
the input Ballerina service should be defined using the basic service level @websocket:ServiceConfig annotation with the dispatcherKey.

If you need to document an AsyncAPI contract for only one given service, then use this command.
```bash
    bal asyncapi -i <ballerina-file-path> --service <service-name>
               [--protocol <protocol>]
```

### Samples for AsyncAPI Commands

#### Generate an AsyncAPI Contract from a Service

 ```bash
    bal asyncapi -i modules/helloworld/helloService.bal --protocol ws
  ```
This will generate the AsyncAPI contracts for the Ballerina services, which are in the `hello.bal` Ballerina file.
 ```bash 
    bal asyncapi -i modules/helloworld/helloService.bal --service helloworld --protocol ws
  ```
This command will generate the `helloworld-asyncapi.yaml` file that is related to the `helloworld` service inside the
 `helloService.bal` file.
 ```bash
    bal asyncapi -i modules/helloworld/helloService.bal --json --protocol ws
  ```
This `--json` option can be used with the Ballerina to AsyncAPI command to generate the `helloworld-asyncapi.json` file 
instead of generating the YAML file.
