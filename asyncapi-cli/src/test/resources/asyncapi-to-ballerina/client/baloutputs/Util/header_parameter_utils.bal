import nuvindu/pipe;

type SimpleBasicType string|boolean|int|float|decimal;

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

# Combine custom headers and param headers
#
# + customHeaders - Custom headers map
# + paramHeaders - Headers generated using spec
# + return - Return combine custom and spec generated headers
isolated function getCombineHeaders(map<string> customHeaders, map<string> paramHeaders) returns map<string> {
    foreach [string, string] [k, v] in paramHeaders.entries() {
        customHeaders[k] = v;
    }
    return customHeaders;

}
