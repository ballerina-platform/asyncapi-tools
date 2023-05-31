/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.asyncapi.core.generators.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.schema.BallerinaTypesGenerator;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.core.GeneratorConstants.DEFAULT_RETURN;
import static io.ballerina.asyncapi.core.GeneratorUtils.extractReferenceType;
import static io.ballerina.asyncapi.core.GeneratorUtils.getValidName;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PIPE_TOKEN;

/**
 * This util class for maintain the operation response with ballerina return type.
 *
 * @since 1.3.0
 */
public class RemoteFunctionReturnTypeGenerator {
    private AsyncApi25DocumentImpl asyncAPI;
    private List<TypeDefinitionNode> typeDefinitionNodeList = new LinkedList<>();

    public RemoteFunctionReturnTypeGenerator() {

    }

    public RemoteFunctionReturnTypeGenerator(AsyncApi25DocumentImpl asyncAPI,
                                             List<TypeDefinitionNode> typeDefinitionNodeList) {

        this.asyncAPI = asyncAPI;

        this.typeDefinitionNodeList = typeDefinitionNodeList;
    }

    /**
     * Get return type of the remote function.
     * <p>
     * //     * @param operation swagger operation.
     *
     * @return string with return type.
     * @throws BallerinaAsyncApiException - throws exception if creating return type fails.
     */
    public String getReturnType(JsonNode x_response,JsonNode x_response_type) throws BallerinaAsyncApiException {
        //TODO: Handle multiple media-type
        ArrayList<String> returnTypes = new ArrayList<>();
        String type = null;

        if (x_response.get("oneOf") != null) {  //Handle Union references
            if(x_response.get("oneOf") instanceof ArrayNode){
                ArrayNode test= (ArrayNode) x_response.get("oneOf");
                if(x_response_type!=null) {
                    for (Iterator<JsonNode> it = test.iterator(); it.hasNext(); ) {
                        JsonNode jsonNode = it.next();
                        if (jsonNode.get("$ref") != null) {
                            String reference = jsonNode.get("$ref").asText();
                            String schemaName = getValidName(extractReferenceType(reference), true);
                            AsyncApi25SchemaImpl refSchema = (AsyncApi25SchemaImpl) asyncAPI.getComponents().getSchemas().get(
                                    schemaName);
                            type = getDataType(schemaName, refSchema);
                            returnTypes.add(type);


                        } else if (jsonNode.get("payload") != null) {
                            throw new BallerinaAsyncApiException("Ballerina service file cannot be generate to the " +
                                    "given AsyncAPI specification, Response type must be a Record");
                        }

                        System.out.println("test");
                    }
                }else{
                    throw new BallerinaAsyncApiException("x-response-type must be included ex:-" +
                            " x-response-type: streaming || x-response-type: simple-rpc");
                }
            }

        } else if (x_response.get("payload")!=null && x_response.get("payload")
                .get("type") != new TextNode("object")) { //Handle payload references
            throw new BallerinaAsyncApiException("Ballerina service file cannot be generate to the " +
                    "given AsyncAPI specification, Response type must be a Record");



        } else if (x_response.get("$ref") != null) { //Handle reference responses
            String reference = x_response.get("$ref").asText();
            String schemaName = getValidName(extractReferenceType(reference), true);
            AsyncApi25SchemaImpl refSchema = (AsyncApi25SchemaImpl) asyncAPI.getComponents().getSchemas().get(
                    schemaName);
            type = getDataType(schemaName, refSchema);
            returnTypes.add(type);

        }
//        return type;

        //Add |error to the response
        if (returnTypes.size() > 0) {
            String finalReturnType = String.join(PIPE_TOKEN.stringValue(), returnTypes) ;


            return finalReturnType;


        } else {
            return DEFAULT_RETURN;
        }
    }


    /**
     * Get return data type by traversing AsyncAPI schemas.
     */
    private String getDataType(String schemaName,AsyncApi25SchemaImpl schema)
            throws BallerinaAsyncApiException {
        String type = null;

        if(schema!=null && schema.getType()!=null && schema.getType().equals("object")){
            type = handleInLineRecordInResponse(schemaName, schema);

        }else{
            throw new BallerinaAsyncApiException("Response type must be a record");
        }
        return type;
    }

    /**
     * Handle inline record by generating record with name for response in AsyncAPI type ObjectSchema.
     */
    private String handleInLineRecordInResponse(String schemaName, AsyncApi25SchemaImpl objectSchema)
            throws BallerinaAsyncApiException {

        Map<String, Schema> properties = objectSchema.getProperties();
        String ref = objectSchema.get$ref();
//        String type = getValidName(schemaName, true) + "Response";
        String type = getValidName(schemaName, true) ;


        if (ref != null) {
            type = extractReferenceType(ref.trim());
        }

        return type;


    }

}
