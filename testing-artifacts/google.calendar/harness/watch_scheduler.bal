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

import ballerina/lang.runtime;
import ballerina/log;
import ballerina/task;
import ballerina/time;

class Job {
    *task:Job;
    private Listener 'listener;
    private int retryCount = 1;
    private int retryScheduleCount = 1;

    isolated function init(Listener 'listener) {
        self.'listener = 'listener;
    }

    public isolated function execute() {
        error? err = self.'listener.registerWatchChannel();
        if (err is error) {
            log:printWarn(WARN_CHANNEL_REGISTRATION, 'error = err);
            if (self.retryCount <= 10) {
                log:printInfo(INFO_RETRY_CHANNEL_REGISTRATION + self.retryCount.toString());
                runtime:sleep(5);
                self.retryCount += 1;
                self.execute();
            } else {
                panic error(ERR_CHANNEL_REGISTRATION);
            }
        } else {
            self.scheduleNextChannel();
        }
    }

    isolated function scheduleNextChannel() {
        error? err = self.scheduleNextChannelRenewal();
        if (err is error) {
            log:printWarn(WARN_CHANNEL_REGISTRATION, 'error = err);
            if (self.retryScheduleCount <= 10) {
                log:printInfo(INFO_RETRY_SCHEDULE + self.retryScheduleCount.toString());
                runtime:sleep(5);
                self.retryScheduleCount += 1;
                self.scheduleNextChannel();
            } else {
                panic error(ERR_SCHEDULE);
            }
        }
    }

    isolated function scheduleNextChannelRenewal() returns error? {
        time:Utc currentUtc = time:utcNow();
        decimal timeDifference = (self.'listener.expirationTime/1000) - (<decimal>currentUtc[0]) - 60;
        time:Utc scheduledUtcTime = time:utcAddSeconds(currentUtc, timeDifference);
        time:Civil scheduledTime = time:utcToCivil(scheduledUtcTime);
        _ = check task:scheduleOneTimeJob(self, scheduledTime);
        return;
    }
}
