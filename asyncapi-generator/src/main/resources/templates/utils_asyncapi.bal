import ballerina/url;
import xlibb/pipe;

# PipesMap class to handle generated pipes
public isolated class PipesMap {
    private final map<pipe:Pipe> pipes;
    private final map<pipe:Pipe> turns;
    public isolated function init() {
        self.pipes = {};
        self.turns = {};
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
            // Turns close first, so calls waiting for one end even if a reply pipe fails to close
            foreach pipe:Pipe turn in self.turns {
                pipe:Error? turnErr = turn.immediateClose();
                if turnErr is pipe:Error {
                    continue;
                }
            }
            self.turns.removeAll();
            foreach pipe:Pipe pipe in self.pipes {
                check pipe.gracefulClose();
            }
            self.pipes.removeAll();
        }
    }

    # Waits until no other call is waiting for a reply on the given pipe.
    #
    # + id - The reply pipe
    # + timeout - Seconds to wait for the turn
    # + return - A `pipe:Error` if the wait times out or the connection closes
    public isolated function takeTurn(string id, decimal timeout) returns pipe:Error? {
        pipe:Pipe turn;
        lock {
            if !self.turns.hasKey(id) {
                pipe:Pipe newTurn = new (1);
                check newTurn.produce(true, timeout);
                self.turns[id] = newTurn;
            }
            turn = self.turns.get(id);
        }
        boolean _ = check turn.consume(timeout);
    }

    # Lets the next call waiting on the given pipe send its request. A closed turn means the connection is closing,
    # so there is no one to hand it to, and the caller keeps the reply it already has.
    #
    # + id - The reply pipe
    public isolated function giveTurn(string id) {
        pipe:Pipe? turn;
        lock {
            turn = self.turns[id];
        }
        if turn is pipe:Pipe {
            pipe:Error? produceErr = turn.produce(true, 5);
            if produceErr is pipe:Error {
                return;
            }
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



public type SimpleBasicType string|boolean|int|float|decimal;

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

# Generate query path with query parameter.
#
# + queryParam - Query parameter map
# + return - Returns generated Path or error at failure of client initialization
public isolated function getPathForQueryParam(map<anydata> queryParam) returns string|error {
    string[] param = [];
    if (queryParam.length() > 0) {
        param.push("?");
        foreach var [key, value] in queryParam.entries() {
            if value is () {
                _ = queryParam.remove(key);
                continue;
            }
            if (value is SimpleBasicType) {
                param.push(key, "=", getEncodedUri(value.toString()));

            } else {
                param.push(key, "=", value.toString());
            }
            param.push("&");
        }
        _ = param.pop();
    }
    string restOfPath = string:'join("", ...param);
    return restOfPath;
}


# Combine custom headers and param headers
#
# + customHeaders - Custom headers map
# + paramHeaders - Headers generated using spec
# + return - Return combine custom and spec generated headers
public isolated function getCombineHeaders(map<string> customHeaders, map<string> paramHeaders) returns map<string> {
    foreach [string, string] [k, v] in paramHeaders.entries() {
        customHeaders[k] = v;
    }
    return customHeaders;
}
