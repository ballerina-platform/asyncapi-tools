# Specification: Ballerina AsyncAPI for WebSockets

_Authors_: @thushalya @hasathcharu  
_Reviewers_: @bhashinee @shafreenAnfar  
_Created_: 2024/06/27   
_Updated_: 2026/10/13  
_Edition_: Swan Lake  

## Introduction

This is the specification for the AsyncApi Tools of [Ballerina language](https://ballerina.io/), which supports generation of client API from an AsyncAPI specification for WebSocket protocols. This also allows the generation of an AsyncAPI specification for a given Ballerina service running on WebSockets.

The AsyncAPI Tools specification has evolved and may continue to evolve in the future. The released versions of the specification can be found under the relevant GitHub tag.

If you have any feedback or suggestions about the tool, start a discussion via a [GitHub issue](https://github.com/ballerina-platform/ballerina-library/issues) or in the [Discord server](https://discord.gg/ballerinalang). Based on the outcome of the discussion, the specification and implementation can be updated. Community feedback is always welcome. Any accepted proposal, which affects the specification is stored under `/docs/proposals`. Proposals under discussion can be found with the label `type/proposal` in GitHub.

The conforming implementation of the specification is released and included in the distribution. Any deviation from the specification is considered a bug.

## Contents

1. [Overview](#1-overview)
2. [Generating the specification](#2-generating-the-specification)
3. [Generating the Ballerina client](#3-generating-the-ballerina-client)

## 1. Overview
This specification elaborates on the `AsyncAPI CLI Tool` commands.

## 2. Generating the specification

This tool enables the generation of an AsyncAPI specification for a given service running on WebSockets. It can be done by running the following command.

```bash
bal asyncapi <package_directory | bal_file> --protocol ws [-o <output_directory>]
```

| Command Parameter |                                    Description                                    | Mandatory |    Default Value    |
|:-----------------:|:---------------------------------------------------------------------------------:|:---------:|:-------------------:|
|     `<input>`     |          the Ballerina package directory, or a standalone `.bal` file           |    Yes    |          -          |
|    --protocol     |                         the protocol, `ws` for WebSockets                         |    Yes    |          -          |
|   -o, --output    |                             the output directory                                |    No     | <current_directory> |
|     --module      |                    the module of the package to read services from                    |    No     |  <default_module>   |
|     --service     |         the base path of the service to generate the specification for, e.g. `/chat`         |    No     |   <all_services>    |
|     --format      |                       the output format, `yaml` or `json`                       |    No     |        yaml         |
|      --force      |                       overwrite specifications that already exist                       |    No     |        false        |
|     --dry-run     |              show the files that would be written, without writing them              |    No     |        false        |


The command generates an AsyncAPI specification for the services running on WebSockets in the given input. The generated specification will be saved in the specified output directory. If the output directory is not specified, the specification will be saved in the current directory.

Behaviour of the command,
- The input is a Ballerina package directory, or a standalone `.bal` file that is not part of a package. For a file inside a package, pass the package directory instead.
- For a package, the services in every file of the module are read. The default module is used unless `--module` names another one.
- Users should provide input which does not contain any compilation errors.
- If there are multiple services, the tool will generate a separate specification for each service, by equating each service to a channel. `--service` limits this to the services with the given base path.
- A specification that already exists with different content is skipped with a warning unless `--force` is given. A run that skips any file exits with a non-zero code.
- The generated specification is of version 3.0.0.

### How the AsyncAPI specification is generated

#### Info Object

```yaml
info:
  title: <service_name> 
  version: <package_version>
```

The `service_name` is derived from the name of the service. If the name of the service is `/`, the `service_name` will be derived from the file name. Otherwise, the `service_name` will be the name of the service.

The `package_version` is derived from the version defined in the `Ballerina.toml` file, if available. If the version is not defined in the `Ballerina.toml` file, the `package_version` will be `0.0.0`.

#### Server Object

```yaml
servers:
  development:
    host: "{server}:{port}"
    protocol: ws
    protocolVersion: "13"
    pathname: /
    variables:
      server:
        default: ws://localhost
      port:
        default: "9092"
```

The server name will be `development` by default, which can be changed by the user once the specification is generated. 
The `server` URL will be `ws://localhost`, and if the `websocket:Listener` has SSL configurations, the `server` URL will be `wss://localhost`. The `port` will be derived from the port number that the `websocket:Listener` is attached to. The `pathname` is the base path of the service. The protocol will be `ws`, and the WebSocket protocol version used will be `13`.

#### Channel and Operation Objects

```yaml
channels:
  root:
    address: /
    messages:
      Hello:
        $ref: "#/components/messages/Hello"
      Response:
        $ref: "#/components/messages/Response"
    description: An echo service that echoes the messages sent by the client.
operations:
  sendHello:
    action: send
    channel:
      $ref: "#/channels/root"
    messages:
    - $ref: "#/channels/root/messages/Hello"
  receiveResponse:
    action: receive
    channel:
      $ref: "#/channels/root"
    messages:
    - $ref: "#/channels/root/messages/Response"
```

Currently, this tool supports only one channel per specification. If there are multiple services in the project, the tool will generate a separate specification for each service, by equating each service to a channel.
The equivalent Ballerina service class is given below. The `description` will be the doc comment provided on the `resource` function of the service.

```Ballerina
@websocket:ServiceConfig{dispatcherKey: "event"}
service / on websocketListener {
    # An echo service that echoes the messages sent by the client.
    # + return - User status
    resource function get .() returns websocket:Service|websocket:UpgradeError {
        return new WsService();
    }
}
```

When there are path parameters in the `resource` function, the `parameters` object will be added to the `channel` object with the relevant path parameters.

When there are query parameters in the `resource` function, the `bindings` object will be added to the `channel` object with the relevant query parameters under the `query` object.

When there are headers in the `resource` function, the `bindings` object will be added to the `channel` object with the relevant headers under the `headers` object.

Here, the `bindingsVersion` will be same as the `version` in the `info` object.

The returned `WsService` class is as follows.

```Ballerina
service class WsService {
    *websocket:Service;

    remote function onHello(Hello clientData) returns Response? {
        return {message:"You sent: " + clientData.message};
    }

}
```

The channel's messages and the `send` and `receive` operations will be derived from the `remote` functions available in the service class. Each message a `remote` function accepts becomes a `send` operation (sent by the client), and each response it returns becomes a `receive` operation.

A `remote` function may take a `websocket:Caller` before its message parameter; the caller is not a message and is not added to the specification.

#### Components Object

The `components` object will contain the `messages` and `schemas` objects. `messages` refer to what can be sent and received in the channel. `schemas` refer to the data types used in `messages`.

A message sent by the client is the type of the parameter its `remote` function accepts, for example `Hello` for `onHello(Hello clientData)`. The function's name does not need to match the type's name.
Messages received by the client can be of type `any`.

Refer to the following Ballerina records.

```Ballerina
public type Hello record {|
    string message;
    string event;
|};

# Representation of a response
# 
# + event - dispatcher key
# + message - message to be sent
public type Response record {|
    string event = "just a message";
    string message;
|};
```

These are the records that are used in the `WsService` class. These records will be mapped to the `components` object in the AsyncAPI specification as follows. If the fields are properly documented with comments, they will be added to the `description` field in the AsyncAPI specification.

```yaml
components:
  schemas:
    Hello:
      type: object
      required:
      - message
      - event
      properties:
        message:
          type: string
        event:
          type: string
          const: Hello
    Response:
      type: object
      required:
      - event
      - message
      properties:
        event:
          type: string
          description: dispatcher key
        message:
          type: string
          description: message to be sent
      description: Representation of a response
  messages:
    Response:
      payload:
        $ref: '#/components/schemas/Response'
    Hello:
      payload:
        $ref: '#/components/schemas/Hello'
      x-response:
        $ref: '#/components/messages/Response'
        x-required: false
      x-response-type: simple-rpc
```

Here, for optional fields, the `x-required` property will be set to `false`. The `x-response-type` property will be set to `simple-rpc` if the `remote` function returns a single response. If the `remote` function returns a `stream` of responses, the `x-response-type` property will be set to `server-streaming`.

#### Extended Properties

The `x-dispathcherKey` property is added if there is a `@websocket:ServiceConfig` annotation with a `dispatcherKey` value in the service class.

The `x-dispatcherStreamId` property is added if there is a `@websocket:ServiceConfig` annotation with a `dipatcherStreamId` value in the service class.

## 3. Generating the Ballerina client

This tool enables the generation of a WebSocket client based on a given AsyncAPI specification. It can be done by running the following command.

```bash
bal asyncapi <specification_file> --protocol ws [-o <output_directory>]
```

| Command Parameter |                           Description                            | Mandatory |    Default Value    |
|:-----------------:|:----------------------------------------------------------------:|:---------:|:-------------------:|
|     `<input>`     |      the AsyncAPI specification (`.yaml`, `.yml` or `.json`)      |    Yes    |          -          |
|    --protocol     |                the protocol, `ws` for WebSockets                 |    Yes    |          -          |
|   -o, --output    |                       the output directory                       |    No     | <current_directory> |
|     --module      |          the module of the package to write the client into          |    No     |  <default_module>   |
|   --with-tests    |          specifies whether to generate a test skeleton           |    No     |        false        |
|     --license     | specifies the path for a text file containing the license header |    No     |          -          |
|      --force      |                overwrite files that already exist                |    No     |        false        |
|     --dry-run     |    show the files that would be written, without writing them    |    No     |        false        |


The command generates a WebSocket client for the given AsyncAPI specification. The client will be generated in the specified output directory, which must be inside a Ballerina package. If the output directory is not specified, the client will be generated in the current directory.

Behaviour of the command,
- The tool will only generate a client if the provided input file is a valid AsyncAPI specification. A client is currently generated from AsyncAPI 2.x specifications; the 3.0.0 specifications generated by this tool are not yet supported as input.
- A file that already exists with different content is skipped with a warning unless `--force` is given. A run that skips any file exits with a non-zero code.
- The command will generate a `client.bal` file, a `types.bal` file, and a `utils.bal` file. If the `--with-tests` flag is provided, the tool will generate a `tests/tests.bal` file as well.
- If the `--license` flag is provided, the tool will add the license header to the generated files.

### The `client.bal` file

The generated client contains two workers, one to handle the write operations and the other to handle the read operations to the WebSocket server. 

The client will also use a `writeMessageQueue` to queue the messages to be sent to the server.

The `x-dispatcherKey` property is used to determine the dispatcher key of the message, in which a pipe is created to handle the response of the message for a specific event.

If the `x-dispatcherStreamId` property is available, the client will instead use the `dispatcherStreamId` to create pipes to produce the messages. 

These pipes are consumed by the respective `remote` functions.

For each `publish` message type in the `channels` section, the tool will generate a `remote` function to send the message to the server. If the server responds with a single response, i.e., if the `x-response-type` property is set to `simple-rpc`, the generated function will return the response. If the server responds with a stream of responses, i.e., if the `x-response-type` property is set to `server-streaming`, the generated function will return a `stream` of responses.

### The `utils.bal` file

This file will contain a `PipesMap` class to manage the pipes created for each event.

If a `stream` of responses are expected to be returned by one or more of the `remote` functions, then a `StreamGenerator` class will also be generated.

### The `types.bal` file

This file will contain the types used in the `client.bal` file. The types will be derived from the `components` object in the AsyncAPI specification.