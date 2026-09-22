/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.asyncapi.generator.http.node;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.extractor.EventIdentifierExtractor;
import io.ballerina.asyncapi.generator.http.generator.DataTypesGenerator;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NodeParser;

/**
 * Generates the {@code isolated function dispatchBatchedEvents(json[] eventsArray, string eventType)
 * returns error?} method node for the {@code DispatcherService} class in {@code dispatcher_service.bal}.
 *
 * <p>Holds the per-element dispatch loop for a batched delivery, factored out of the {@code post}
 * resource function so it can be run on a separate strand via {@code start} after the batch is
 * acknowledged, instead of holding the HTTP worker thread for the whole loop's duration.
 */
public class GenerateDispatchBatchFuncNode implements Generator {

    public static final String DISPATCH_BATCH_FUNC = "dispatchBatchedEvents";

    private final EventIdentifierConfig identifierConfig;

    public GenerateDispatchBatchFuncNode(EventIdentifierConfig identifierConfig) {
        this.identifierConfig = identifierConfig;
    }

    @Override
    public FunctionDefinitionNode generate() throws GeneratorException {
        String type = identifierConfig.type();
        String cloneVar = GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME;
        StringBuilder loopBody = new StringBuilder();

        if (EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_BODY.equals(type)) {
            loopBody.append(String.format("json|error eventTypeField = event.%s;", identifierConfig.path()));
            loopBody.append(" if eventTypeField is error {"
                    + " log:printError(\"DISPATCH_FAILED\", eventTypeField); continue; }");
            loopBody.append(" string elementEventType = eventTypeField.toString();");
        } else if (EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_COMPOSITE.equals(type)) {
            loopBody.append(String.format("json|error actionField = event.%s;", identifierConfig.path()));
            loopBody.append(" string eventIdentifier = eventType;");
            loopBody.append(" if actionField is json && actionField != () {"
                    + " eventIdentifier = eventType + \"_\" + actionField.toString(); }");
        } else {
            loopBody.append(" string elementEventType = eventType;");
        }

        loopBody.append(String.format(" %s|error %sResult = event.cloneWithType(%s);",
                DataTypesGenerator.GENERIC_DATA_TYPE, cloneVar, DataTypesGenerator.GENERIC_DATA_TYPE));
        loopBody.append(String.format(
                " if %sResult is error { log:printError(\"DISPATCH_FAILED\", %sResult); continue; }",
                cloneVar, cloneVar));

        if (EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_COMPOSITE.equals(type)) {
            loopBody.append(String.format(
                    " error? dispatchResult = self.%s(%sResult, eventIdentifier, eventType);",
                    GenerateMatchRemoteFuncNode.DISPATCHER_MATCH_REMOTE_FUNC, cloneVar));
        } else {
            loopBody.append(String.format(
                    " error? dispatchResult = self.%s(%sResult, elementEventType);",
                    GenerateMatchRemoteFuncNode.DISPATCHER_MATCH_REMOTE_FUNC, cloneVar));
        }
        loopBody.append(" if dispatchResult is error { log:printError(\"DISPATCH_FAILED\", dispatchResult); }");

        String functionText = String.format(
                "isolated function %s(json[] eventsArray, string eventType) returns error? {"
                        + " foreach json event in eventsArray { %s }"
                        + " }",
                DISPATCH_BATCH_FUNC, loopBody);

        return (FunctionDefinitionNode) NodeParser.parseObjectMember(functionText);
    }
}
