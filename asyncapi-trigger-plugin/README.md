# Trigger compiler plugin

`bal build --export-endpoints` lists the ports and paths a program listens on in `target/artifact/endpoints.yaml`.
The `ballerina/http` compiler plugin only reports services attached to an `http:Listener`, so each trigger carries
this plugin to report the services attached to its `Listener`, with the same rules and defaults as HTTP services
(`type: "REST"`, an empty `schemaPath`).

Ballerina runs a plugin only for apps that import its package directly. Apps import the trigger, never
`asyncapi.native.handler`, so the plugin has to be inside each trigger package. It finds its trigger from where its
jar is loaded, so the same jar works for every trigger.

## How it gets into a trigger

This is the only copy of the source. `asyncapi-cli` bundles the built jar inside the asyncapi tool. A generated
trigger has a `[[tool.asyncapi]]` entry in its `Ballerina.toml`, so `bal build` and `bal pack` run the tool as a
build tool, which copies the jar to `target/tool-cache/trigger-plugin/`, where the trigger's `CompilerPlugin.toml`
points. `bal pack` then puts it in the bala. The trigger's source never holds the jar.

The plugin's version is the tool's. A fix here reaches a trigger when its `[[tool.asyncapi]]` entry moves to the tool
release with the fix (generating with that tool does it). The jar is built without timestamps, so the same tool
version always copies the same bytes.
