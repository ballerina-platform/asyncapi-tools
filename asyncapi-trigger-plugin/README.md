# Trigger compiler plugin

`bal build --export-endpoints` lists the ports and paths a program listens on in `target/artifact/endpoints.yaml`.
The `ballerina/http` compiler plugin only reports services attached to an `http:Listener`, so each trigger carries
this plugin to report the services attached to its `Listener`, with the same rules and defaults as HTTP services
(`type: "REST"`, an empty `schemaPath`).

Ballerina runs a plugin only for apps that import its package directly. Apps import the trigger, never
`asyncapi.native.handler`, so the plugin has to be inside each trigger package. It finds its trigger from where its
jar is loaded, so the same jar works for every trigger.

## How it gets into a trigger

This is the only copy of the source. `asyncapi-cli` bundles the built jar inside the asyncapi tool, and generating a
trigger writes it to the trigger's `lib/` folder, where its `CompilerPlugin.toml` points. `bal pack` then puts it in
the bala.

The plugin's version is the tool's. A fix here reaches a trigger when it is generated again with the tool release
that has the fix. The jar is built without timestamps, so the same tool version always writes the same bytes.
