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
package io.ballerina.asyncapi.generator.ws.asyncspec.mapper;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.values.ConstantValue;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.NoSuchElementException;
import java.util.Optional;

import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.BALLERINA;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.UNSUPPORTED_ANNOTATION_VALUE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.WEBSOCKET;

/**
 * Reads {@code ballerina/websocket} annotations ({@code @websocket:ServiceConfig}, {@code @websocket:DispatcherConfig})
 * the way websocket itself sees them: the annotation is found by its symbol wherever it sits among the others, and a
 * field's value is the string it stands for, not its source text.
 */
final class WebSocketAnnotations {

    private WebSocketAnnotations() {
    }

    /**
     * The {@code @websocket:<name>} annotation among the given ones, if any.
     */
    static Optional<AnnotationNode> find(NodeList<AnnotationNode> annotations, String name,
                                         SemanticModel semanticModel) {
        if (annotations == null) {
            return Optional.empty();
        }
        for (AnnotationNode annotation : annotations) {
            Optional<Symbol> symbol = semanticModel.symbol(annotation);
            if (symbol.isPresent() && name.equals(symbol.get().getName().orElse(""))
                    && symbol.get().getModule().map(WebSocketAnnotations::isWebsocketModule).orElse(false)) {
                return Optional.of(annotation);
            }
        }
        return Optional.empty();
    }

    /**
     * The value expression of the annotation's {@code fieldName} field, if it is set.
     */
    static Optional<ExpressionNode> field(AnnotationNode annotation, String fieldName) {
        if (annotation.annotValue().isEmpty()) {
            return Optional.empty();
        }
        for (MappingFieldNode field : annotation.annotValue().get().fields()) {
            if (field instanceof SpecificFieldNode specificField
                    && fieldName.equals(specificField.fieldName().toString().strip())
                    && specificField.valueExpr().isPresent()) {
                return specificField.valueExpr();
            }
        }
        return Optional.empty();
    }

    /**
     * The string a field's value stands for: a string literal with its escapes decoded, or a string constant's value.
     * The compiler accepts only those here, so anything else is reported rather than read as its source text.
     */
    static String stringValue(ExpressionNode value, String fieldName, SemanticModel semanticModel) {
        if (value.kind() == SyntaxKind.STRING_LITERAL) {
            // The token's own text, without a trailing comment
            String literal = ((BasicLiteralNode) value).literalToken().text();
            return unescapeStringLiteral(literal.substring(1, literal.length() - 1));
        }
        Optional<Symbol> symbol = semanticModel.symbol(value);
        if (symbol.isPresent() && symbol.get() instanceof ConstantSymbol constant) {
            Object constValue = constant.constValue();
            if (constValue instanceof ConstantValue wrapped) {
                constValue = wrapped.value();
            }
            if (constValue instanceof String text) {
                return text;
            }
        }
        throw new NoSuchElementException(String.format(UNSUPPORTED_ANNOTATION_VALUE, fieldName,
                value.toString().strip()));
    }

    /**
     * The value of a Ballerina string literal's text, with its escapes ({@code \t}, {@code \n}, {@code \r},
     * {@code \\}, {@code \"} and {@code \\u{hex}}) decoded, as websocket sees the annotation value.
     */
    static String unescapeStringLiteral(String text) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\' || i + 1 == text.length()) {
                value.append(c);
                continue;
            }
            char escaped = text.charAt(++i);
            switch (escaped) {
                case 't' -> value.append('\t');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 'u' -> {
                    int close = text.indexOf('}', i);
                    value.appendCodePoint(Integer.parseInt(text.substring(i + 2, close), 16));
                    i = close;
                }
                default -> value.append(escaped);
            }
        }
        return value.toString();
    }

    private static boolean isWebsocketModule(ModuleSymbol module) {
        return WEBSOCKET.equals(module.getName().orElse("")) && BALLERINA.equals(module.id().orgName());
    }
}
