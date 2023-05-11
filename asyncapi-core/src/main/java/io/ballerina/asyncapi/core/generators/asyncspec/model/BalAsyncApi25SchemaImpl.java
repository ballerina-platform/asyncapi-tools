package io.ballerina.asyncapi.core.generators.asyncspec.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.apicurio.datamodels.models.union.BooleanSchemaUnion;
import io.apicurio.datamodels.models.union.BooleanUnionValueImpl;
import io.apicurio.datamodels.models.union.SchemaListUnionValueImpl;
import io.apicurio.datamodels.models.union.SchemaSchemaListUnion;

@JsonDeserialize(as = BalAsyncApi25SchemaImpl.class)
interface MockSchema extends Schema {


}

//@JsonTypeInfo( use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "class")
//@JsonSubTypes({
//        @JsonSubTypes.Type(value = AsyncApi25SchemaImpl.class)
//})

/**
 * In Apicurio data model they are using JsonNodes for bindings
 * therefore we have to convert Java object to a Jackson ObjectNode we have to use new ObjectMapper().
 * This {@code BalAsyncApi25SchemaImpl} contains details related to BalAsyncApi25SchemaImpl. It has overrided
 * the original Apicurio isEntity method and set it into false, then it will not be printed. Reason because this
 * is in the original, entity attribute has become true by default and it is also printing in the asyncapi definition
 * when using jackson's objectmapper
 *
 * @since 2.0.0
 */

public class BalAsyncApi25SchemaImpl extends io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl {

//    public BalAsyncApi25SchemaImpl() {
//        super();
//    }

    @JsonDeserialize(as = BalAsyncApi25SchemaImpl.class)
    private SchemaSchemaListUnion items;

    @JsonDeserialize(as = BalAsyncApi25SchemaImpl.class)
    private BooleanSchemaUnion additionalProperties;

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
