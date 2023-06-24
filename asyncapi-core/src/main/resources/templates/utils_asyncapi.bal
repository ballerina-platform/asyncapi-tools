import ballerina/url;
import xlibb/pipe;

# PipesMap class to handle generated pipes
public isolated class PipesMap{
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
            pipe:Pipe pipe = new (1);
            self.addPipe(id, pipe);
            return pipe;
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
public isolated class StreamGeneratorsMap{
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
public type Generator isolated object{

    public isolated function next() returns record {|anydata value;|}|error?;

    public isolated function close() returns error?;
};



public type SimpleBasicType string|boolean|int|float|decimal;

# Get Encoded URI for a given value.
#
# + value - Value to be encoded
# + return - Encoded string
public isolated function getEncodedUri(anydata value) returns string{
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
public isolated function getPathForQueryParam(map<anydata> queryParam) returns string|error{
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
public isolated function getCombineHeaders(map<string> customHeaders, map<string> paramHeaders) returns map<string>{
    foreach [string, string] [k, v] in paramHeaders.entries(){
        customHeaders[k] = v;
    }
    return customHeaders;

}


