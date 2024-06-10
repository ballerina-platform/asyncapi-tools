## Module overview

The AsyncAPI tools provide the following capabilities.
 
 1. Generate the Ballerina client code for a given AsyncAPI definition. 
 2. Export the AsyncAPI definition of a Ballerina service.
    
The `asyncapi1` command in Ballerina is used for AsyncAPI to Ballerina and Ballerina to AsyncAPI code generations. 
Code generation from AsyncAPI to Ballerina can produce `ballerina client stubs`.
   

### AsyncAPI to Ballerina
#### Generate Client Stub from an AsyncAPI Contract

```bash
bal asyncapi1 -i <asyncapi-contract-path> 
               [--service-name: generated files name]
               [(-o|--output): output file path]
```
Generates Ballerina client stub for a given AsyncAPI file.

This `-i <asyncapi-contract-path>` parameter of the command is mandatory. It will get the path to the
 AsyncAPI contract file (i.e., `my-api.yaml` or `my-api.json`) as an input.

The `--service-name`  is an optional parameter, which allows you to change the generated service name.

The `(-o|--output)` is an optional parameter. You can use this to give the output path of the generated files.
If not, it will take the execution path as the output path.


If you want to generate client , you can set the mode as  `client` in the AsyncAPI tool. 
This client can be used in client applications to call the service defined in the AsyncAPI file.

```bash
bal asyncapi1 -i <asyncapi-contract-path> 
               [(-o|--output) output file path]
```

### Ballerina to AsyncAPI
#### Service to AsyncAPI Export
```bash
bal asyncapi1 -i <ballerina-file-path> 
               [(-o|--output) output asyncapi file path]
```
Export the Ballerina service to an  AsyncAPI Specification 2.5.0 definition. For the export to work properly, 
the input Ballerina service should be defined using the basic service and resource-level websocket annotation
@ServiceConfig 

If you need to document an AsyncAPI contract for only one given service, then use this command.
```bash
    bal asyncapi1 -i <ballerina-file-path> (-s | --service) <service-name>
```

### Samples for AsyncAPI Commands
#### Generate Client Stub from AsyncAPI
```bash
    bal asyncapi1 -i hello.yaml
```

This will generate a Ballerina client stub for the `hello.yaml` AsyncAPI client named `hello-client`.
The above command can be run from within anywhere on the execution path. 
It is not mandatory to run it from inside the Ballerina project.

Output:
```bash
The service generation process is complete. The following files were created.
-- hello-client.bal
-- types.bal
-- utils.bal
```
#### Generate an AsyncAPI Contract from a Service

 ```bash
    bal asyncapi1 -i modules/helloworld/helloService.bal
  ```
This will generate the AsyncAPI contracts for the Ballerina services, which are in the `hello.bal` Ballerina file.
 ```bash 
    bal asyncapi1 -i modules/helloworld/helloService.bal (-s | --service) helloworld
  ```
This command will generate the `helloworld-asyncapi1.yaml` file that is related to the `helloworld` service inside the
 `helloService.bal` file.
