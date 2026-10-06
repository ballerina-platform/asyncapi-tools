// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package io.ballerinax.event;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;

import static io.ballerina.runtime.api.utils.StringUtils.fromString;

/**
 * This class contains the native functions.
 * These are being called from Ballerina (dispatcher_service.bal) through interop
 */
public class NativeHttpToEventAdaptor {

    private static final Module JSONDATA_MODULE = new Module("ballerina", "data.jsondata", "1");
    private static final String OPTIONS_RECORD = "Options";
    private static final String ALLOW_DATA_PROJECTION = "allowDataProjection";
    private static final String NIL_AS_OPTIONAL_FIELD = "nilAsOptionalField";
    private static final String ABSENT_AS_NILABLE_TYPE = "absentAsNilableType";

    public static Object invokeRemoteFunction(Environment env, BObject adaptor, BMap<BString, Object> message,
                                              BString eventName, BString eventFunction, BObject serviceObj) {
        Object[] args = new Object[]{message, true};
        return env.yieldAndRun(() -> {
            try {
                return env.getRuntime().callMethod(serviceObj, eventFunction.getValue(), null, args);
            } catch (BError error) {
                BString errorMessage = fromString("service method invocation failed: " + error.getErrorMessage());
                BError invocationError = ErrorCreator.createError(errorMessage, error);
                return invocationError;
            }
        });
    }

    /**
     * Binds a raw payload to the record type declared on the first parameter of the given remote
     * function of the attached service, discovered by reflection. The binding itself is delegated to
     * {@code jsondata:parseAsType} so {@code @jsondata:Name} renames and data projection behave as they
     * would for a statically typed call.
     */
    public static Object bindEventPayload(Environment env, BObject serviceObj, BString eventFunction,
                                          Object payload) {
        Type targetType;
        try {
            targetType = resolvePayloadType(serviceObj, eventFunction.getValue());
        } catch (IllegalStateException e) {
            return ErrorCreator.createError(fromString(e.getMessage()));
        }
        BTypedesc targetTypedesc = ValueCreator.createTypedescValue(targetType);
        // Calling a module function via Runtime.callFunction takes plain argument values, unlike
        // callMethod above, which expects a trailing "argument given" flag per parameter.
        Object[] args = new Object[]{payload, projectionOptions(), targetTypedesc};
        return env.yieldAndRun(() -> {
            try {
                return env.getRuntime().callFunction(JSONDATA_MODULE, "parseAsType", null, args);
            } catch (BError error) {
                BString errorMessage = fromString("payload binding failed: " + error.getErrorMessage());
                return ErrorCreator.createError(errorMessage, error);
            }
        });
    }

    private static Type resolvePayloadType(BObject serviceObj, String functionName) {
        ObjectType serviceType = serviceObj.getType();
        for (MethodType method : serviceType.getMethods()) {
            if (method.getName().equals(functionName)) {
                Parameter[] parameters = method.getParameters();
                if (parameters.length == 0) {
                    throw new IllegalStateException(
                            "remote function '" + functionName + "' declares no payload parameter");
                }
                return parameters[0].type;
            }
        }
        throw new IllegalStateException("remote function '" + functionName + "' not found on the attached service");
    }

    // Providers send "field": null for unset optional fields, which parseAsType rejects unless projection is on.
    private static BMap<BString, Object> projectionOptions() {
        BMap<BString, Object> options = ValueCreator.createRecordValue(JSONDATA_MODULE, OPTIONS_RECORD);
        RecordType optionsType = (RecordType) options.getType();
        Field projectionField = optionsType.getFields().get(ALLOW_DATA_PROJECTION);
        // The field's inherent type is an anonymous record (or false); a plain map would be rejected.
        Type projectionType = projectionField.getFieldType();
        if (projectionType instanceof UnionType unionType) {
            for (Type member : unionType.getMemberTypes()) {
                if (member instanceof RecordType) {
                    projectionType = member;
                    break;
                }
            }
        }
        BMap<BString, Object> projection = ValueCreator.createMapValue(projectionType);
        projection.put(fromString(NIL_AS_OPTIONAL_FIELD), true);
        projection.put(fromString(ABSENT_AS_NILABLE_TYPE), true);
        options.put(fromString(ALLOW_DATA_PROJECTION), projection);
        return options;
    }
}
