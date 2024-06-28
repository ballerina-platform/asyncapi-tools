# Specification: Ballerina AsyncAPI for Websockets

_Authors_: @thushalya @hasathcharu  
_Reviewers_: @bhashinee @shafreenAnfar  
_Created_: 2024/06/27   
_Updated_: 2024/06/28  
_Edition_: Swan Lake  

## Introduction

This is the specification for the AsyncApi Tools of [Ballerina language](https://ballerina.io/), which supports generation of client API from an AsyncAPI specification for Websocket protocols. This also allows the generation of an AsyncAPI specification for a given Ballerina service running on websockets.

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

This tool enables the generation of an AsyncAPI specification for a given service running on websockets. It can be done by running the following command.

```bash
bal asyncapi --protocol ws -i <input_file> --o <output_directory>
```

| Command Parameter |                           Description                            | Mandatory |    Default Value    |
|:-----------------:|:----------------------------------------------------------------:|:---------:|:-------------------:|
|    --protocol     |                   indicates the protocol used                    |    No     |        http         |
|   <-i, --input>   |             specifies the entry file of the service              |    Yes    |          -          |
|  <-o, --output>   |                  specifies the output directory                  |    No     | <current_directory> |
|     --service     | specifies the specific service to generate the specification for |    No     |   <all_services>    |
|      --json       |          specifies whether to generate in `json` format          |    No     |        false        |


The command generates an AsyncAPI specification for the given service running on websockets. The generated specification will be saved in the specified output directory. If the output directory is not specified, the specification will be saved in the current directory.

Behaviour of the command,
- The tool generates a specification only if the provided input file is a Ballerina file.
- Users should provide an input file which does not contain any compilation errors.
- If executed within a Ballerina project, the tool will generate the specification for all the services in the project, including all the types defined within modules.
- If there are multiple services in the project, the tool will generate a separate specification for each service, by equating each service to a channel.
- The generated specification is of version 2.5.0.

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
  "development":
    url: "{server}:{port}/"
    protocol: ws
    protocolVersion: "13"
    variables:
      server:
        default: ws://localhost
      port:
        default: "9092"
```

The server name will be `development` by default, which can be changed by the user once the specification is generated. 
The `server` URL will be `ws://localhost`, and if the `websocket:Listener` has SSL configurations, the `server` URL will be `wss://localhost`. The `port` will be derived from the port number that the `websocket:Listener` is attached to. The protocol version will be `13` by default. The `protocol` will be `ws`.

#### Channel Object

```yaml
channels:
  /:
    description: An echo service that echoes the messages sent by the client.
    subscribe:
      message:
        $ref: '#/components/messages/Response'
    publish:
      message:
        $ref: '#/components/messages/Hello'
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

The `subscribe` and `publish` objects will be derived from the `remote` functions available in the service class.
When there are multiple `subscribe` and `publish` payload types, the tool will add them all with the `oneOf` property.

#### Components Object

The `components` object will contain the `messages` and `schemas` objects. `messages` refer to what you can `subscribe` to and `publish` in the channel. `schemas` refer to the data types used in `messages`.

`publish` messages must always be a `record` type of the name of the `remote` function without the `on` word. For example, if the `remote` function is `onHello`, the `subscribe` message type shall be `Hello`.
`subscribe` messages can be of type `any`.

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

This tool enables the generation of a websocket client based on a given AsyncAPI specification. It can be done by running the following command.

```bash
bal asyncapi --protocol ws -i <input_file> --o <output_directory>
```

| Command Parameter |                           Description                            | Mandatory |    Default Value    |
|:-----------------:|:----------------------------------------------------------------:|:---------:|:-------------------:|
|    --protocol     |                   indicates the protocol used.                   |    No     |        http         |
|   <-i, --input>   |         specifies the file containing the specification          |    Yes    |          -          |
|  <-o, --output>   |                  specifies the output directory                  |    No     | <current_directory> |
|   --with-tests    |          specifies whether to generate a test skeleton           |    No     |        false        |
|     --license     | specifies the path for a text file containing the license header |    No     |          -          |


The command generates an AsyncAPI specification for the given service running on websockets. The generated client will be generated in the specified output directory. If the output directory is not specified, the client will be generated in the current directory.

Behaviour of the command,
- The tool will only generate a client if the provided input file is a valid AsyncAPI specification.
- The command will generate a `client.bal` file, a `types.bal` file, and a `utils.bal` file. If the `--with-tests` flag is provided, the tool will generate a `tests/tests.bal` file as well.
- If the `--license` flag is provided, the tool will add the license header to the generated files.

### The `client.bal` file

The generated client contains two workers, one to handle the write operations and the other to handle the read operations to the websocket server. 

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