package io.ballerina.asyncapi.websocketscore.generators.asyncspec.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.apicurio.datamodels.models.union.BooleanUnionValueImpl;

/**
 * This {@code BalBooleanSchema} contains details related to asyncAPI Boolean schema.
 * In Apicurio data model ,they are using BooleanSchemaUnion but boolean: true and value: true fields need to be ignored
 * therefore those defined as @JsonIgnore
 */
public class BalBooleanSchema extends BooleanUnionValueImpl {

    public BalBooleanSchema(Boolean value) {
        super(value);
    }

    @JsonIgnore
    @Override
    public boolean isBoolean() {
        return true;
    }

    @JsonIgnore
    @Override
    public Boolean getValue() {
        return true;
    }


}
