package io.ballerina.asyncapi.core.generators.client;

import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.GeneratorConstants;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;

import java.util.List;

import static io.ballerina.asyncapi.core.GeneratorConstants.X_DISPATCHER_KEY;
import static io.ballerina.asyncapi.core.GeneratorUtils.extractReferenceType;

/**
 * Common function utils for request and response.
 */
public class CommonFunctionUtils {


    private AsyncApi25DocumentImpl asyncAPI;

    public CommonFunctionUtils(AsyncApi25DocumentImpl asyncAPI) {
        this.asyncAPI = asyncAPI;
    }


    /**
     * Get return data type by traversing AsyncAPI schemas.
     */
    public boolean isDispatcherPresent(String schemaName, AsyncApi25SchemaImpl schema,
                                              String dispatcherVal, boolean isParent)
            throws BallerinaAsyncApiException {

        if (schema != null) {
            if (schema.getProperties() != null) {
//                type = getValidName(schemaName, true);
                if (schema.getProperties().containsKey(dispatcherVal)) {
                    if (!schema.getProperties().get(dispatcherVal).getType().equals("string")) {
                        throw new BallerinaAsyncApiException("Both dispatcherKey and " +
                                "dispatcherStreamId type must be string");
                    }
                    if (schema.getRequired() == null || (!schema.getRequired().contains(dispatcherVal))) {
                        throw new BallerinaAsyncApiException("" +
                                "Both dispatcherKey and dispatcherStreamId type must be inside required property");
                    }
                    return true;
                }

            } else if (schema.getOneOf() != null) {
                List<AsyncApiSchema> oneOfSchemas = schema.getOneOf();
                for (AsyncApiSchema oneOfSchema : oneOfSchemas) {
                    AsyncApi25SchemaImpl oneOf25Schema = (AsyncApi25SchemaImpl) oneOfSchema;
                    boolean oneOfContainProperties;
                    if (oneOf25Schema.get$ref() != null) {
                        String refSchemaName = extractReferenceType(oneOf25Schema.get$ref());
                        AsyncApi25SchemaImpl refSchema = (AsyncApi25SchemaImpl) asyncAPI.getComponents().
                                getSchemas().get(
                                        refSchemaName);
                        oneOfContainProperties = isDispatcherPresent(refSchemaName, refSchema, dispatcherVal,
                                false);
                    } else {
                        oneOfContainProperties = isDispatcherPresent("", oneOf25Schema,
                                dispatcherVal, false);
                    }
                    if (!oneOfContainProperties && isParent) {
                        TextNode textNode = (TextNode) asyncAPI.getExtensions().get(X_DISPATCHER_KEY);
                        String dispatcherKey = textNode.asText();
                        if (dispatcherVal.equals(dispatcherKey)) {
                            throw new BallerinaAsyncApiException(String.format(
                                    "%s schema must be a record, and it must have properties to contain " +
                                            "dispatcherKey as a field", schemaName));
                        }
                    }

                }
                return true;

//            if(schema.getProperties()==null){
//                throw new BallerinaAsyncApiException(String.format(
//                        "Response type must be a record, %s schema must contain properies field",schemaName));
//            }


            } else if (schema.getAllOf() != null) {
                List<Schema> allOfSchemas = schema.getAllOf();
                boolean allOfContainProperties;
                for (Schema allOfSchema : allOfSchemas) {
                    AsyncApi25SchemaImpl allOf25Schema = (AsyncApi25SchemaImpl) allOfSchema;
                    if (allOf25Schema.get$ref() != null) {
                        String refSchemaName = extractReferenceType(allOf25Schema.get$ref());
                        AsyncApi25SchemaImpl refSchema = (AsyncApi25SchemaImpl) asyncAPI.getComponents().
                                getSchemas().get(
                                        schemaName);
                        allOfContainProperties = isDispatcherPresent(refSchemaName, refSchema, dispatcherVal,
                                false);
                    } else {
                        allOfContainProperties = isDispatcherPresent("", allOf25Schema,
                                dispatcherVal, false);
                    }
                    if (allOfContainProperties) {
                        return true;

                    }

                }
                TextNode textNode = (TextNode) asyncAPI.getExtensions().get(X_DISPATCHER_KEY);
                String dispatcherKey = textNode.asText();
                if (dispatcherVal.equals(dispatcherKey)) {
                    throw new BallerinaAsyncApiException(String.format(
                            "%s schema must be a record, and it must have properties to contain dispatcherKey " +
                                    "as a field", schemaName));
                }


            } else if (!schema.getType().equals(GeneratorConstants.OBJECT)) {
                throw new BallerinaAsyncApiException(String.format(
                        "Response type must be a record, invalid response type %s in %s schema, schema must contain " +
                                "properties field to contain dispatcherKey",
                        schema.getType(), schemaName));
            } else {
                return false;

            }
        } else {
//            if (schema == null) {
//                throw new BallerinaAsyncApiException("Response type must be a record, invalid response schema");
//
//            } else {
            throw new BallerinaAsyncApiException("Response type must be a record, invalid response schema");
//            }
        }
        return false;
    }
}
