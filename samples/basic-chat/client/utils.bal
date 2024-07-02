// Copyright (c) 2024 WSO2 LLC. (http://www.wso2.com).
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

import xlibb/pipe;

# Stream generator class for Response return type
public client isolated class ResponseStreamGenerator {
    *Generator;
    private final PipesMap pipes;
    private final string pipeId;
    private final decimal timeout;

    # StreamGenerator
    #
    # + pipe - Pipe to hold stream messages 
    # + timeout - Waiting time 
    public isolated function init(PipesMap pipes, string pipeId, decimal timeout) {
        self.pipes = pipes;
        self.pipeId = pipeId;
        self.timeout = timeout;
    }

    public isolated function next() returns record {|Response value;|}|error {
        while true {
            anydata|error? message = self.pipes.getPipe(self.pipeId).consume(self.timeout);
            if message is error? {
                continue;
            }
            Response response = check message.cloneWithType();
            return {value: response};
        }
    }

    public isolated function close() returns error? {
        check self.pipes.removePipe(self.pipeId);
    }
}

# PipesMap class to handle generated pipes
public isolated class PipesMap {
    private final map<pipe:Pipe> pipes;

    public isolated function init() {
        self.pipes = {};
    }

    public isolated function addPipe(string id, pipe:Pipe pipe) {
        lock {
            self.pipes[id] = pipe;
        }
    }

    public isolated function getPipe(string id) returns pipe:Pipe {
        lock {
            if (self.pipes.hasKey(id)) {
                return self.pipes.get(id);
            }
            pipe:Pipe pipe = new (100);
            self.addPipe(id, pipe);
            return pipe;
        }
    }

    public isolated function removePipe(string id) returns error? {
        lock {
            _ = check self.getPipe(id).gracefulClose();
            _ = self.pipes.remove(id);
        }
    }

    public isolated function removePipes() returns error? {
        lock {
            foreach pipe:Pipe pipe in self.pipes {
                check pipe.gracefulClose();
            }
            self.pipes.removeAll();
        }
    }
}

# StreamGeneratorsMap class to handle generated stream generators
public isolated class StreamGeneratorsMap {
    private final Generator[] streamGenerators;

    public isolated function init() {
        self.streamGenerators = [];
    }

    public isolated function addStreamGenerator(Generator streamGenerator) {
        lock {
            self.streamGenerators.push(streamGenerator);
        }
    }

    public isolated function removeStreamGenerators() returns error? {
        lock {
            foreach Generator streamGenerator in self.streamGenerators {
                check streamGenerator.close();
            }
        }
    }
}

# Generator object type for type inclusion
public type Generator isolated object {
    public isolated function next() returns record {|anydata value;|}|error;
    public isolated function close() returns error?;
};
