/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.websocketscore.exception;

/**
 * Exception type definition for AsyncApi to Ballerina parsing related errors.
 *
 */
public class BallerinaAsyncApiExceptionWs extends Exception {
    public BallerinaAsyncApiExceptionWs(String message, Throwable e) {
        super(message, e);
    }

    public BallerinaAsyncApiExceptionWs(String message) {
        super(message);
    }
}
