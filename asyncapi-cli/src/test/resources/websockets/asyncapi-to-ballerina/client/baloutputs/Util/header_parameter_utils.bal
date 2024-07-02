import xlibb/pipe;

type SimpleBasicType string|boolean|int|float|decimal;

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
