package io.ballerina.asyncapi.core.generators.asyncspec.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.apicurio.datamodels.models.union.BooleanUnionValueImpl;

/**
 * In Apicurio data model ,they are using BooleanSchemaUnion but If use this as they given then we can't get the output.
 *
 *
 * @since 2.0.0
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
