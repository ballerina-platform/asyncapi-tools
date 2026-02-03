package io.ballerina.asyncapi.core.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public interface AsyncApiSpec {

    String getAsyncApiVersion();

    String getAsyncApiId();

    AsyncApiInfo getAsyncApiInfo();

    Map<String, AsyncApiServer> getAsyncApiServers();

    String getAsyncApiContentType();

    List<AsyncApiChannel> getAsyncApiChannels();

    List<AsyncApiOperation> getAsyncApiOperations();

    AsyncApiComponent getAsyncApiComponents();

    Map<String, JsonNode> getAsyncApiExtensions();
}
