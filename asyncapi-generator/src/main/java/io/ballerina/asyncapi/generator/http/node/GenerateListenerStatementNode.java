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
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.StatementNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates the {@code match} statement body for {@code getServiceTypeStr} in
 * {@code listener.bal}, mapping each attached service instance to its type name string.
 */
public class GenerateListenerStatementNode implements Generator {

    private final List<String> serviceTypeNames;

    /**
     * Creates a generator for the {@code getServiceTypeStr} function body.
     *
     * @param serviceTypeNames the raw service type names to generate branches for
     */
    public GenerateListenerStatementNode(List<String> serviceTypeNames) {
        this.serviceTypeNames = serviceTypeNames;
    }

    @Override
    public StatementNode generate() throws GeneratorException {
        if (serviceTypeNames.isEmpty()) {
            throw new GeneratorException(
                    "No service types found, probably there are no channels defined in the async api spec");
        }
        return buildMatchStatement(serviceTypeNames);
    }

    /**
     * Builds a {@code match serviceRef { XService _ => { return "XService"; } ... }} statement,
     * one type-binding-pattern clause per known service type, ending in a {@code var _} clause
     * that returns an {@code error(...)} so an unrecognized {@code serviceRef} fails loudly (the
     * caller, {@code attach}/{@code detach}, propagates it via {@code check}) instead of being
     * silently mislabeled as whichever type happened to be tested last.
     *
     * <p>Returns an {@code error}, not a {@code panic}: a {@code panic} would crash the entire
     * running listener process over one bad {@code attach} call, whereas returning an error lets
     * the caller handle it as an ordinary failed operation.
     */
    private StatementNode buildMatchStatement(List<String> serviceTypes) {
        String clauses = serviceTypes.stream()
                .map(CodegenUtils::getServiceTypeNameByServiceName)
                .map(typeName -> String.format("%s _ => { return \"%s\"; }", typeName, typeName))
                .collect(Collectors.joining(" "));
        String matchStatement = String.format(
                "match serviceRef { %s var _ => { return error(\"Unrecognized service type attached to the listener\"); } }",
                clauses);
        return NodeParser.parseStatement(matchStatement);
    }
}
