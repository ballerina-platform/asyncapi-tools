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

import ballerina/http;
import ballerinax/asyncapi.native.handler;
import ballerina/log;

service class DispatcherService {
    *http:Service;
    private map<GenericServiceType> services = {};
    private handler:NativeHandler nativeHandler = new ();
    private ListenerConfig listenerConfig;

    public function init(ListenerConfig listenerConfig) {
      self.listenerConfig = listenerConfig;
    }

    isolated function addServiceRef(string serviceType, GenericServiceType genericService) returns error? {
        if (self.services.hasKey(serviceType)) {
            return error("Service of type " + serviceType + " has already been attached");
        }
        self.services[serviceType] = genericService;
    }

    isolated function removeServiceRef(string serviceType) returns error? {
        if (!self.services.hasKey(serviceType)) {
            return error("Cannot detach the service of type " + serviceType + ". Service has not been attached to the listener before");
        }
        _ = self.services.remove(serviceType);
    }

    // We are not using the (@http:payload GenericEventWrapperEvent g) notation because of a bug in Ballerina.
    // Issue: https://github.com/ballerina-platform/ballerina-lang/issues/32859
    resource function post .(http:Caller caller, http:Request request) returns error? {
        json payload = check request.getJsonPayload();
        // Intent verification Handling
        if (payload.spreadsheetId !== self.listenerConfig.spreadsheetId) {
            return error("Spread sheet ID mismatch");
        } else {
            log:printInfo("Intent verification success...");
        }
        GenericDataType genericDataType = check payload.cloneWithType(GenericDataType);
        check self.matchRemoteFunc(genericDataType);
        http:Response ackRes = new;
        ackRes.statusCode = http:STATUS_OK;
        check caller->respond(ackRes);
    }

    private function matchRemoteFunc(GenericDataType genericDataType) returns error? {
        match genericDataType?.eventType {
            "appendRow" => {
                check self.executeRemoteFunc(genericDataType, "appendRow", "SheetRowService", "onAppendRow");
            }
            "updateRow" => {
                check self.executeRemoteFunc(genericDataType, "updateRow", "SheetRowService", "onUpdateRow");
            }
        }
    }

    private function executeRemoteFunc(GenericDataType genericEvent, string eventName, string serviceTypeStr, string eventFunction) returns error? {
        GenericServiceType? genericService = self.services[serviceTypeStr];
        if genericService is GenericServiceType {
            check self.nativeHandler.invokeRemoteFunction(genericEvent, eventName, eventFunction, genericService);
        }
    }
}
