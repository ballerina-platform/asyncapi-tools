package io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.apicurio.datamodels.models.union.BooleanUnionValueImpl;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import static io.ballerina.asyncapi.core.GeneratorUtils.convertAsyncAPITypeToBallerina;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.GT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MAP_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MAP_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_KEYWORD;

public class MapTypeGenerator extends TypeGenerator {
    public MapTypeGenerator(AsyncApi25SchemaImpl schema, String typeName) {
        super(schema, typeName);
    }

    @Override
    public TypeDescriptorNode generateTypeDescriptorNode() throws BallerinaAsyncApiException {

        if ( (schema.getAdditionalProperties() instanceof BooleanUnionValueImpl &&schema.getAdditionalProperties().asBoolean().equals(true))
                ||((AsyncApi25SchemaImpl) schema.getAdditionalProperties()).getType() == null) {

//            paramType = "map<" + convertAsyncAPITypeToBallerina("{}") + ">";
//            convertAsyncAPITypeToBallerina("{}")

            return NodeFactory.createMapTypeDescriptorNode(createToken(MAP_KEYWORD),NodeFactory.createTypeParameterNode(
                    createToken(LT_TOKEN),NodeFactory.createBuiltinSimpleNameReferenceNode(
                            null,createIdentifierToken( convertAsyncAPITypeToBallerina("{}"))),createToken(GT_TOKEN)));
//            return NodeFactory.createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
//                    metadataBuilder.isOpenRecord() ? createToken(OPEN_BRACE_TOKEN) : createToken(OPEN_BRACE_PIPE_TOKEN),
//                    createNodeList(recordFields), metadataBuilder.getRestDescriptorNode(),
//                    metadataBuilder.isOpenRecord() ? createToken(CLOSE_BRACE_TOKEN) :
//                            createToken(CLOSE_BRACE_PIPE_TOKEN));

        } else if (((AsyncApi25SchemaImpl) schema.getAdditionalProperties()).getType() != null) {


            return NodeFactory.createMapTypeDescriptorNode(createToken(MAP_KEYWORD),NodeFactory.createTypeParameterNode(
                    createToken(LT_TOKEN),NodeFactory.createBuiltinSimpleNameReferenceNode(null,createIdentifierToken(convertAsyncAPITypeToBallerina(((AsyncApi25SchemaImpl) schema.
                            getAdditionalProperties()).getType())))
                    ,createToken(GT_TOKEN)));


        }
        return null;
    }





}
