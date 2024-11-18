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
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import static io.ballerina.runtime.api.utils.StringUtils.fromString;

/**
 * This class contains the native functions.
 * These are being called from Ballerina (dispatcher_service.bal) through interop
 */
public class NativeHttpToEventAdaptor {
    public static Object invokeRemoteFunction(Environment env, BObject adaptor, BMap<BString, Object> message,
                                              BString eventName, BString eventFunction, BObject serviceObj) {
        Object[] args = new Object[]{message, true};
        return env.yieldAndRun(() -> {
            try {
                return env.getRuntime().callMethod(serviceObj, eventFunction.getValue(),
                        new StrandMetadata(true, null), args);
            } catch (BError error) {
                BString errorMessage = fromString("service method invocation failed: " + error.getErrorMessage());
                BError invocationError = ErrorCreator.createError(errorMessage, error);
                return invocationError;
            }
        });
    }
}
