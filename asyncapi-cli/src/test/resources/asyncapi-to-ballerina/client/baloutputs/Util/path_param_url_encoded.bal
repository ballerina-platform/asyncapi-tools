import nuvindu/pipe;
import ballerina/url;
import nuvindu/pipe;

# Stream generator class
isolated class StreamGenerator {
    private final pipe:Pipe pipe;
    private final decimal timeout;

    # Description
    #
    # + pipe - Pipe to hold stream messages
    # + timeout - Waiting time
    isolated function init(pipe:Pipe pipe, decimal timeout) returns error? {
        self.pipe = pipe;
        self.timeout = timeout;
    }

    # Next method to return next stream message
    public isolated function next() returns record {|NextMessage|CompleteMessage|ErrorMessage value;|}|error? {
        anydata responseMessage = check self.pipe.consume(self.timeout);
        NextMessage|CompleteMessage|ErrorMessage response = check responseMessage.cloneWithType();
        return {value: response};
    }
}

# Pipesmap class
isolated class PipesMap {
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
            return self.pipes.get(id);
        }
    }
}

# Get Encoded URI for a given value.
#
# + value - Value to be encoded
# + return - Encoded string
isolated function getEncodedUri(anydata value) returns string {
    string|error encoded = url:encode(value.toString(), "UTF8");
    if (encoded is string) {
        return encoded;
    } else {
        return value.toString();
    }
}
