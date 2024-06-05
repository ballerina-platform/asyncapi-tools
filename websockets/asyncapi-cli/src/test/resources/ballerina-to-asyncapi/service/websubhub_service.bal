// Copyright (c) 2023 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
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
import ballerina/websubhub;

isolated string[] locations = [];
isolated map<websubhub:VerifiedSubscription> newsReceiversCache = {};

service /query on new websubhub:Listener(9000) {

    // Topic registration is not supported by this `hub`
    remote function onRegisterTopic(websubhub:TopicRegistration msg)
        returns websubhub:TopicRegistrationError {
        return error websubhub:TopicRegistrationError(
            "Topic registration not supported", statusCode = http:STATUS_NOT_IMPLEMENTED);
    }

    // Topic deregistration is not supported by this `hub`
    remote function onDeregisterTopic(websubhub:TopicDeregistration msg) returns websubhub:TopicDeregistrationError {
        return error websubhub:TopicDeregistrationError(
            "Topic deregistration not supported", statusCode = http:STATUS_NOT_IMPLEMENTED);
    }

    // Content update is not supported by this `hub`
    remote function onUpdateMessage(websubhub:UpdateMessage msg) returns websubhub:UpdateMessageError {
        return error websubhub:UpdateMessageError(
            "Content update not supported", statusCode = http:STATUS_NOT_IMPLEMENTED);
    }

    remote function onSubscriptionValidation(readonly & websubhub:Subscription subscription) returns websubhub:SubscriptionDeniedError? {
        string newsReceiverId = string `${subscription.hubTopic}-${subscription.hubCallback}`;
        boolean newsReceiverAvailable = false;
        lock {
            newsReceiverAvailable = newsReceiversCache.hasKey(newsReceiverId);
        }
        if newsReceiverAvailable {
            return error websubhub:SubscriptionDeniedError(
                    string `News receiver for location ${subscription.hubTopic} and endpoint ${subscription.hubCallback} already available`,
                    statusCode = http:STATUS_NOT_ACCEPTABLE
                );
        }
    }

    remote function onSubscriptionIntentVerified(readonly & websubhub:VerifiedSubscription subscription) returns error? {
        lock {
            if locations.indexOf(subscription.hubTopic) is () {
                locations.push(subscription.hubTopic);
            }
        }
        string newsReceiverId = string `${subscription.hubTopic}-${subscription.hubCallback}`;
        lock {
            newsReceiversCache[newsReceiverId] = subscription;
        }
    }

    remote function onUnsubscriptionValidation(readonly & websubhub:Unsubscription unsubscription) returns websubhub:UnsubscriptionDeniedError? {
        string newsReceiverId = string `${unsubscription.hubTopic}-${unsubscription.hubCallback}`;
        boolean newsReceiverNotAvailable = false;
        lock {
            newsReceiverNotAvailable = !newsReceiversCache.hasKey(newsReceiverId);
        }
        if newsReceiverNotAvailable {
            return error websubhub:UnsubscriptionDeniedError(
                    string `News receiver for location ${unsubscription.hubTopic} and endpoint ${unsubscription.hubCallback} not available`,
                    statusCode = http:STATUS_NOT_ACCEPTABLE
                );
        }
    }

    remote function onUnsubscriptionIntentVerified(readonly & websubhub:VerifiedUnsubscription unsubscription) returns error? {
        string newsReceiverId = string `${unsubscription.hubTopic}-${unsubscription.hubCallback}`;
        lock {
            _ = newsReceiversCache.removeIfHasKey(newsReceiverId);
        }
    }
}