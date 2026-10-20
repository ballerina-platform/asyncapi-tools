# Trigger template

Files that every trigger repo gets alongside the code `bal asyncapi` generates. The trigger scaffold copies them in.

## Compiler plugin

`bal build --export-endpoints` lists the ports and paths a program listens on in `target/artifact/endpoints.yaml`.
The `ballerina/http` compiler plugin only reports services attached to an `http:Listener`, so each trigger ships its
own compiler plugin to report the services attached to its `Listener`, with the same rules and defaults as HTTP
services (`type: "REST"`, an empty `schemaPath`).

As `ballerina/http` does with its plugin, the trigger builds the plugin from source in its own repo and its
`CompilerPlugin.toml` points at the built jar, so nothing is published separately. The plugin finds its trigger from
where its jar is loaded, so the code is the same for every trigger.

The scaffold wires it into a trigger repo like this:

| Template | Trigger repo |
|---|---|
| `compiler-plugin/` | `compiler-plugin/`, included in `settings.gradle` as `:<package>-compiler-plugin` |
| `CompilerPlugin.toml` | `build-config/resources/CompilerPlugin.toml`, with `@package.name@` set to the package name |

and in `ballerina/build.gradle`, as for `Ballerina.toml`:

- `updateTomlFiles` writes `CompilerPlugin.toml` from the template, with `@project.version@` set to the version.
- `build` depends on `:<package>-compiler-plugin:build`, so the jar exists when the package is packed.

The plugin is built and tested here too (`:trigger-template-compiler-plugin`), so a change is checked before it is
copied into the triggers.
