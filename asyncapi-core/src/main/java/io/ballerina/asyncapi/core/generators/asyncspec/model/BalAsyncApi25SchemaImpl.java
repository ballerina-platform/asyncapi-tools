package io.ballerina.asyncapi.core.generators.asyncspec.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.apicurio.datamodels.models.union.BooleanSchemaUnion;
import io.apicurio.datamodels.models.union.SchemaSchemaListUnion;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;

/**
 * Because JsonNodes are used for bindings in the Apicurio data model, we must use Jackson ObjectMapper to
 * transform a Java object to a Jackson ObjectNode. But by default true values will not be excluded when try to convert
 * Java objects to Json nodes, therefore this approach has overridden the original Apicurio isEntity function
 * and changed it to false,thus it will not be included in the output asyncAPI specification.
 *
 */

public class BalAsyncApi25SchemaImpl extends AsyncApi25SchemaImpl {

    // @JsonDeserialize is using to identify the correct class when deserializing, here it is SchemaSchemaListUnion
    @JsonDeserialize(as = BalAsyncApi25SchemaImpl.class)
    private SchemaSchemaListUnion items;

    @JsonDeserialize(as = BalAsyncApi25SchemaImpl.class)
    private BooleanSchemaUnion additionalProperties;


    // @JsonIgnore is using to ignore the attributes when serializing
    @JsonIgnore
    @Override
    public boolean isEntity() {
        return true;
    }


    @JsonIgnore
    @Override
    public boolean isSchema() {
        return true;
    }


}


