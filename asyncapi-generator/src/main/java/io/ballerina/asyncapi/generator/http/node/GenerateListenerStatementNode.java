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

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the body of {@code getServiceTypeStr} in
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
    public List<StatementNode> generate() throws GeneratorException {
        if (serviceTypeNames.isEmpty()) {
            throw new GeneratorException(
                    "No service types found, probably there are no channels defined in the async api spec");
        }
        return buildStatements(serviceTypeNames);
    }

    /**
     * Builds one {@code if serviceRef is XService} check per known service type, collecting every type the
     * service matches. Exactly one match returns that type. No match returns an {@code error} so an
     * unrecognized {@code serviceRef} fails loudly. More than one match (a service object implementing the
     * remote functions of two service types) is an {@code error} too: with one type picked, the other type's
     * events would never reach the service.
     *
     * <p>Returns an {@code error}, not a {@code panic}: a {@code panic} would crash the entire
     * running listener process over one bad {@code attach} call, whereas returning an error lets
     * the caller handle it as an ordinary failed operation.
     */
    private List<StatementNode> buildStatements(List<String> serviceTypes) {
        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement("string[] matched = [];"));
        for (String serviceType : serviceTypes) {
            String typeName = CodegenUtils.getServiceTypeNameByServiceName(serviceType);
            statements.add(NodeParser.parseStatement(String.format(
                    "if serviceRef is %1$s { matched.push(\"%1$s\"); }", typeName)));
        }
        statements.add(NodeParser.parseStatement("if matched.length() == 1 { return matched[0]; }"));
        statements.add(NodeParser.parseStatement("if matched.length() == 0 {"
                + " return error(\"Unrecognized service type attached to the listener\"); }"));
        statements.add(NodeParser.parseStatement("return error(string `The service matches more than one service"
                + " type (${string:'join(\", \", ...matched)}). Implement one service type per service`);"));
        return statements;
    }
}
