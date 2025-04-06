import ballerina/lang.regexp;
import ballerina/log;
import ballerina/websocket;

import xlibb/pipe;

# WebSockets API offers real-time market data updates. WebSockets is a bidirectional protocol offering fastest real-time data, helping you build real-time applications. The public message types presented below do not require authentication. Private-data messages can be subscribed on a separate authenticated endpoint.
#
# ### General Considerations
#
# - TLS with SNI (Server Name Indication) is required in order to establish a Kraken WebSockets API connection. See Cloudflare's [What is SNI?](https://www.cloudflare.com/learning/ssl/what-is-sni/) guide for more details.
# - All messages sent and received via WebSockets are encoded in JSON format
# - All decimal fields (including timestamps) are quoted to preserve precision.
# - Timestamps should not be considered unique and not be considered as aliases for transaction IDs. Also, the granularity of timestamps is not representative of transaction rates.
# - At least one private message should be subscribed to keep the authenticated client connection open.
# - Please use REST API endpoint [AssetPairs](https://www.kraken.com/features/api#get-tradable-pairs) to fetch the list of pairs which can be subscribed via WebSockets API. For example, field 'wsname' gives the supported pairs name which can be used to subscribe.
# - Cloudflare imposes a connection/re-connection rate limit (per IP address) of approximately 150 attempts per rolling 10 minutes. If this is exceeded, the IP is banned for 10 minutes.
# - Recommended reconnection behaviour is to (1) attempt reconnection instantly up to a handful of times if the websocket is dropped randomly during normal operation but (2) after maintenance or extended downtime, attempt to reconnect no more quickly than once every 5 seconds. There is no advantage to reconnecting more rapidly after maintenance during cancel_only mode.
public client isolated class KrakenWebsocketsAPIClient {
    private final websocket:Client clientEp;
    private final pipe:Pipe writeMessageQueue;
    private final PipesMap pipes;
    private boolean isActive;
    private final readonly & map<string> responseMap = {
        "SystemStatus": "systemStatus",
        "SubscriptionStatus": "unsubscribe",
        "Heartbeat": "heartbeat",
        "Pong": "ping"
    };

    # Gets invoked to initialize the `connector`.
    #
    # + config - The configurations to be used when initializing the `connector`
    # + serviceUrl - URL of the target service
    # + return - An error if connector initialization failed
    public isolated function init(websocket:ClientConfiguration clientConfig =  {}, string serviceUrl = "ws.kraken.com") returns error? {
        self.pipes = new ();
        self.writeMessageQueue = new (1000);
        websocket:Client websocketEp = check new (serviceUrl, clientConfig);
        self.clientEp = websocketEp;
        self.isActive = true;
        self.startMessageWriting();
        self.startMessageReading();
        return;
    }

    private isolated function getRecordName(string dispatchingValue) returns string {
        string[] words = regexp:split(re `[\W_]+`, dispatchingValue);
        string result = "";
        foreach string word in words {
            result += word.substring(0, 1).toUpperAscii() + word.substring(1).toLowerAscii();
        }
        return result;
    }

    private isolated function getPipeName(string responseType) returns string {
        string responseRecordType = self.getRecordName(responseType);
        if self.responseMap.hasKey(responseRecordType) {
            return self.responseMap.get(responseRecordType);
        }
        return responseType;
    }

    # Used to write messages to the websocket.
    #
    private isolated function startMessageWriting() {
        worker writeMessage {
            while true {
                lock {
                    if !self.isActive {
                        break;
                    }
                }
                Message|pipe:Error message = self.writeMessageQueue.consume(5);
                if message is pipe:Error {
                    if message.message() == "Operation has timed out" {
                        continue;
                    }
                    log:printError("PipeError: Failed to consume message from the pipe", message);
                    self.attemptToCloseConnection();
                    return;
                }
                websocket:Error? wsErr = self.clientEp->writeMessage(message);
                if wsErr is websocket:Error {
                    log:printError("WsError: Failed to write message to the client", wsErr);
                    self.attemptToCloseConnection();
                    return;
                }
            }
        }
    }

    # Used to read messages from the websocket.
    #
    private isolated function startMessageReading() {
        worker readMessage {
            while true {
                lock {
                    if !self.isActive {
                        break;
                    }
                }
                Message|websocket:Error message = self.clientEp->readMessage(Message);
                if message is websocket:Error {
                    log:printError("WsError: Failed to read message from the client", message);
                    self.attemptToCloseConnection();
                    return;
                }
                string pipeName = self.getPipeName(message.event);
                pipe:Pipe pipe = self.pipes.getPipe(pipeName);
                pipe:Error? pipeErr = pipe.produce(message, 5);
                if pipeErr is pipe:Error {
                    log:printError("PipeError: Failed to produce message to the pipe", pipeErr);
                    self.attemptToCloseConnection();
                    return;
                }
            }
        }
    }

    # Ping server to determine whether connection is alive
    #
    remote isolated function doPing(Ping ping, decimal timeout) returns Pong|error {
        lock {
            if !self.isActive {
                return error("ConnectionError: Connection has been closed");
            }
        }
        Message|error message = ping.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", message);
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in producing message", pipeErr);
        }
        Message|pipe:Error responseMessage = self.pipes.getPipe("ping").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in consuming message", responseMessage);
        }
        Pong|error pong = responseMessage.cloneWithType();
        if pong is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", pong);
        }
        return pong;
    }

    # Subscribe to a topic on a single or multiple currency pairs.
    #
    remote isolated function doSubscribe(Subscribe subscribe, decimal timeout) returns SubscriptionStatus|error {
        lock {
            if !self.isActive {
                return error("ConnectionError: Connection has been closed");
            }
        }
        Message|error message = subscribe.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", message);
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in producing message", pipeErr);
        }
        Message|pipe:Error responseMessage = self.pipes.getPipe("subscribe").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in consuming message", responseMessage);
        }
        SubscriptionStatus|error subscriptionStatus = responseMessage.cloneWithType();
        if subscriptionStatus is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", subscriptionStatus);
        }
        return subscriptionStatus;
    }

    # Unsubscribe, can specify a channelID or multiple currency pairs.
    #
    remote isolated function doUnsubscribe(Unsubscribe unsubscribe, decimal timeout) returns SubscriptionStatus|error {
        lock {
            if !self.isActive {
                return error("ConnectionError: Connection has been closed");
            }
        }
        Message|error message = unsubscribe.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", message);
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in producing message", pipeErr);
        }
        Message|pipe:Error responseMessage = self.pipes.getPipe("unsubscribe").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in consuming message", responseMessage);
        }
        SubscriptionStatus|error subscriptionStatus = responseMessage.cloneWithType();
        if subscriptionStatus is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", subscriptionStatus);
        }
        return subscriptionStatus;
    }

    remote isolated function doHeartbeat(decimal timeout) returns Heartbeat|error {
        Message|pipe:Error responseMessage = self.pipes.getPipe("heartbeat").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in consuming message", responseMessage);
        }
        Heartbeat|error heartbeat = responseMessage.cloneWithType();
        if heartbeat is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", heartbeat);
        }
        return heartbeat;
    }

    remote isolated function doSystemStatus(decimal timeout) returns SystemStatus|error {
        Message|pipe:Error responseMessage = self.pipes.getPipe("systemStatus").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in consuming message", responseMessage);
        }
        SystemStatus|error systemStatus = responseMessage.cloneWithType();
        if systemStatus is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", systemStatus);
        }
        return systemStatus;
    }

    isolated function attemptToCloseConnection() {
        error? connectionClose = self->connectionClose();
        if connectionClose is error {
            log:printError("ConnectionError", connectionClose);
        }
    }

    remote isolated function connectionClose() returns error? {
        lock {
            self.isActive = false;
            check self.writeMessageQueue.immediateClose();
            check self.pipes.removePipes();
            check self.clientEp->close();
        }
    };
}
