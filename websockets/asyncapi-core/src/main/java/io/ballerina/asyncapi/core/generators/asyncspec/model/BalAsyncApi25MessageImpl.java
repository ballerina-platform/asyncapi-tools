package io.ballerina.asyncapi.core.generators.asyncspec.model;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageImpl;

/**
 * Because JsonNodes are used for bindings in the Apicurio data model, we must use Jackson ObjectMapper to
 * transform a Java object to a Jackson ObjectNode. But by default true values will not be excluded when try to convert
 * Java objects to Json nodes, therefore this approach has overridden the original Apicurio isEntity function
 * and changed it to false,thus it will not be included in the output asyncAPI specification.
 *
 */
public class BalAsyncApi25MessageImpl extends AsyncApi25MessageImpl {
    @Override
    public boolean isEntity() {
        return false;
    }

}
