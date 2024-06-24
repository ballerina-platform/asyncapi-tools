import xlibb/pipe;
import ballerina/url;

# Stream generator class for NextMessage|CompleteMessage|ErrorMessage return type
public client isolated class NextMessageCompleteMessageErrorMessageStreamGenerator {
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

    #  Next method to return next stream message
    #
    public isolated function next() returns record {|NextMessage|CompleteMessage|ErrorMessage value;|}|error {
        while true {
            anydata|error? message = self.pipes.getPipe(self.pipeId).consume(self.timeout);
            if message is error? {
                continue;
            }
            NextMessage|CompleteMessage|ErrorMessage response = check message.cloneWithType();
            return {value: response};
        }
    }

    # Close method to close used pipe
    #
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

# Get Encoded URI for a given value.
#
# + value - Value to be encoded
# + return - Encoded string
public isolated function getEncodedUri(anydata value) returns string {
    string|error encoded = url:encode(value.toString(), "UTF8");
    if (encoded is string) {
        return encoded;
    } else {
        return value.toString();
    }
}
