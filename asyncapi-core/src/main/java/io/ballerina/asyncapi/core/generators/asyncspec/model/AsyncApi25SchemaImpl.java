package io.ballerina.asyncapi.core.generators.asyncspec.model;


//In Apicurio data model they are using JsonNodes for bindings
// therefore we have to convert Java object to a Jackson ObjectNode we have to use new ObjectMapper()
public class AsyncApi25SchemaImpl extends io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl {

    @Override
    public boolean isEntity(){
        return false;
    }
}
