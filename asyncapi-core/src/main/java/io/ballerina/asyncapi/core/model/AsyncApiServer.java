package io.ballerina.asyncapi.core.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Represents a Server Object in an AsyncAPI document.
 * Defines a message broker or server.
 *
 * @param host            The server host name.
 * @param protocol        The protocol this server supports (e.g., "kafka", "ws", "http").
 * @param protocolVersion The version of the protocol used.
 * @param pathname        The path to a resource on the host.
 * @param description     A description of the host.
 * @param title           A human-friendly title for the server.
 * @param summary         A short summary of the server.
 * @param variables       A map of variable substitutions for the server URL template.
 * @param security        A list of security mechanisms supported by the server.
 * @param tags            A list of tags for API documentation control.
 * @param externalDocs    Additional external documentation for the server.
 * @param bindings        Protocol-specific server bindings.
 * @param extensions      Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiServer(
        String host,
        String protocol,
        String protocolVersion,
        String pathname,
        String description,
        String title,
        String summary,
        Map<String, AsyncApiServerVariable> variables,
        List<AsyncApiSecurityRequirement> security,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiServerBindings bindings,
        Map<String, JsonNode> extensions
) {

    /**
     * Represents a Server Variable Object for URL template substitution.
     *
     * @param name         The name of the variable.
     * @param description  A description of the variable.
     * @param defaultValue The default value to use for substitution.
     * @param enumValues   An enumeration of allowed string values.
     * @param examples     Example values for the variable.
     * @param extensions   Specification extensions (fields prefixed with "x-").
     */
    public record AsyncApiServerVariable(
            String name,
            String description,
            String defaultValue,
            List<String> enumValues,
            List<String> examples,
            Map<String, JsonNode> extensions
    ) {
    }

    /**
     * Represents protocol-specific server binding definitions.
     * This structure is designed to be extensible to accommodate future
     * protocol implementations and additional binding properties.
     *
     * @param httpServerBindings HTTP-specific server binding properties.
     * @param wsServerBindings   WebSocket-specific server binding properties.
     */
    public record AsyncApiServerBindings(
            HttpServerBindings httpServerBindings,
            WsServerBindings wsServerBindings
    ) {
    }

    /** HTTP Server Binding Object. Reserved for future use; has no defined properties. */
    public record HttpServerBindings() {
    }

    /** WebSocket Server Binding Object. Reserved for future use; has no defined properties. */
    public record WsServerBindings() {
    }

}
