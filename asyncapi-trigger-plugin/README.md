# Trigger compiler plugin

`bal build --export-endpoints` lists the ports and paths a program listens on in `target/artifact/endpoints.yaml`.
The `ballerina/http` compiler plugin only reports services attached to an `http:Listener`, so each trigger carries
this plugin to report the services attached to its `Listener`, with the same rules and defaults as HTTP services
(`type: "REST"`, an empty `schemaPath`).

Ballerina runs a plugin only for apps that import its package directly. Apps import the trigger, never
`asyncapi.native.handler`, so the plugin has to be inside each trigger package. It finds its trigger from where its
jar is loaded, so the same jar works for every trigger.

## How it gets into a trigger

This is the only copy of the source. `asyncapi-cli` bundles the built jar, and `bal asyncapi --protocol http`
writes it into every trigger it generates at a package root, with the `CompilerPlugin.toml` that packs it:

```
CompilerPlugin.toml
compiler-plugin/asyncapi-trigger-compiler-plugin.jar
```

A fix here reaches the triggers with the next tool release, when they are regenerated. The jar is built without
timestamps, so regenerating with the same tool leaves it unchanged.
