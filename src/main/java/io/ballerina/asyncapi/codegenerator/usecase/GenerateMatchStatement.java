package io.ballerina.asyncapi.codegenerator.usecase;

import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.usecase.utils.CodegenUtils;
import io.ballerina.asyncapi.codegenerator.usecase.utils.VisitorUtils;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.MatchClauseNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;

public class GenerateMatchStatement implements UseCase {
    private final CodegenUtils codegenUtils = new CodegenUtils();
    private AaiDocument document;
    public GenerateMatchStatement(AaiDocument document) {
        this.document = document;
    }

    @Override
    public MatchStatementNode execute() throws BallerinaAsyncApiException {
        VisitorUtils vu = new VisitorUtils();
        UseCase extractServiceTypes = new ExtractServiceTypesFromSpec(this.document);
        Map<String, List<String>> serviceTypes = extractServiceTypes.execute();
        List<MatchClauseNode> matchClauseNodes = new ArrayList<>();

        for (Map.Entry<String, List<String>> service : serviceTypes.entrySet()) {
            String serviceName = service.getKey();
            for (String eventName : service.getValue()) {
                String formattedEventName = "on" + codegenUtils.getValidName(eventName, true);
                MatchClauseNode matchClause = generateMatchClause(serviceName, eventName, formattedEventName);
                matchClauseNodes.add(matchClause);
            }
        }

        String eventPath = vu.getEventNamePathComponents(this.document);

        MatchStatementNode msn = NodeFactory.createMatchStatementNode(
                NodeFactory.createToken(SyntaxKind.MATCH_KEYWORD),
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(eventPath)),
                NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN),
                NodeFactory.createNodeList(matchClauseNodes),
                NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN), null);
        return msn;
    }

    private MatchClauseNode generateMatchClause(String serviceTypeName, String eventName, String formattedEventName) {

        CheckExpressionNode lineNode = NodeFactory.createCheckExpressionNode(SyntaxKind.CHECK_EXPRESSION,
                NodeFactory.createToken(SyntaxKind.CHECK_KEYWORD, codegenUtils.createMinutiae(""), codegenUtils.createMinutiae(" ")),
                NodeFactory.createMethodCallExpressionNode(
                        NodeFactory.createSimpleNameReferenceNode(
                                NodeFactory.createIdentifierToken("self")),
                        createToken(SyntaxKind.DOT_TOKEN),
                        NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken("executeRemoteFunc")),
                        createToken(SyntaxKind.OPEN_PAREN_TOKEN), NodeFactory.createSeparatedNodeList(
                                NodeFactory.createPositionalArgumentNode(NodeFactory.createSimpleNameReferenceNode(
                                        NodeFactory.createIdentifierToken("genericEvent"))),
                                createToken(SyntaxKind.COMMA_TOKEN),
                                NodeFactory.createPositionalArgumentNode(NodeFactory.createSimpleNameReferenceNode(
                                        NodeFactory.createIdentifierToken("\"" + eventName + "\""))),
                                createToken(SyntaxKind.COMMA_TOKEN),
                                NodeFactory.createPositionalArgumentNode(NodeFactory.createSimpleNameReferenceNode(
                                        NodeFactory.createIdentifierToken("\"" + serviceTypeName + "\""))),
                                createToken(SyntaxKind.COMMA_TOKEN),
                                NodeFactory.createPositionalArgumentNode(NodeFactory.createSimpleNameReferenceNode(
                                        NodeFactory.createIdentifierToken("\"" + formattedEventName + "\"")))),
                        NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN)));

        BlockStatementNode blockStatement = NodeFactory.createBlockStatementNode(
                NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN, codegenUtils.createMinutiae(" "), codegenUtils.createMinutiae("\n")),
                NodeFactory.createNodeList(NodeFactory.createExpressionStatementNode(SyntaxKind.CALL_STATEMENT,
                        lineNode,
                        NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN))),
                NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN, codegenUtils.createMinutiae(""), codegenUtils.createMinutiae("\n")));

        MatchClauseNode matchClause = NodeFactory.createMatchClauseNode(NodeFactory.createSeparatedNodeList(
                NodeFactory.createBasicLiteralNode(SyntaxKind.STRING_LITERAL,
                        NodeFactory.createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN, "\"" + eventName + "\"",
                                codegenUtils.createMinutiae(" "),
                                codegenUtils.createMinutiae(" ")))), null,
                NodeFactory.createLiteralValueToken(SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN, "=>", codegenUtils.createMinutiae(""), codegenUtils.createMinutiae(" ")),
                blockStatement);
        return matchClause;
    }
}
