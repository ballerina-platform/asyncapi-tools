import ballerina/lang.runtime;
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

    # Use to write messages to the websocket.
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
                    log:printError("[writeMessage]PipeError: " + message.message());
                    self.attemptToCloseConnection();
                    return;
                }
                websocket:Error? wsErr = self.clientEp->writeMessage(message);
                if wsErr is websocket:Error {
                    log:printError("[writeMessage]WsError: " + wsErr.message());
                    self.attemptToCloseConnection();
                    return;
                }
                runtime:sleep(0.01);
            }
        }
    }

    # Use to read messages from the websocket.
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
                    log:printError("[readMessage]WsError: " + message.message());
                    self.attemptToCloseConnection();
                    return;
                }
                pipe:Pipe pipe = self.pipes.getPipe(message.event);
                pipe:Error? pipeErr = pipe.produce(message, 5);
                if pipeErr is pipe:Error {
                    log:printError("[readMessage]PipeError: " + pipeErr.message());
                    self.attemptToCloseConnection();
                    return;
                }
                runtime:sleep(0.01);
            }
        }
    }

    # Ping server to determine whether connection is alive
    #
    remote isolated function doPing(Ping ping, decimal timeout) returns Pong|error {
        lock {
            if !self.isActive {
                return error("[doPing]ConnectionError: Connection has been closed");
            }
        }
        Message|error message = ping.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("[doPing]DataBindingError: Error in cloning message");
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("[doPing]PipeError: Error in producing message");
        }
        Message|pipe:Error responseMessage = self.pipes.getPipe("ping").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("[doPing]PipeError: Error in consuming message");
        }
        Pong|error pong = responseMessage.cloneWithType();
        if pong is error {
            self.attemptToCloseConnection();
            return error("[doPing]DataBindingError: Error in cloning message");
        }
        return pong;
    }

    # Subscribe to a topic on a single or multiple currency pairs.
    #
    remote isolated function doSubscribe(Subscribe subscribe, decimal timeout) returns SubscriptionStatus|error {
        lock {
            if !self.isActive {
                return error("[doSubscribe]ConnectionError: Connection has been closed");
            }
        }
        Message|error message = subscribe.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("[doSubscribe]DataBindingError: Error in cloning message");
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("[doSubscribe]PipeError: Error in producing message");
        }
        Message|pipe:Error responseMessage = self.pipes.getPipe("subscribe").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("[doSubscribe]PipeError: Error in consuming message");
        }
        SubscriptionStatus|error subscriptionStatus = responseMessage.cloneWithType();
        if subscriptionStatus is error {
            self.attemptToCloseConnection();
            return error("[doSubscribe]DataBindingError: Error in cloning message");
        }
        return subscriptionStatus;
    }

    # Unsubscribe, can specify a channelID or multiple currency pairs.
    #
    remote isolated function doUnsubscribe(Unsubscribe unsubscribe, decimal timeout) returns SubscriptionStatus|error {
        lock {
            if !self.isActive {
                return error("[doUnsubscribe]ConnectionError: Connection has been closed");
            }
        }
        Message|error message = unsubscribe.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("[doUnsubscribe]DataBindingError: Error in cloning message");
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("[doUnsubscribe]PipeError: Error in producing message");
        }
        Message|pipe:Error responseMessage = self.pipes.getPipe("unsubscribe").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("[doUnsubscribe]PipeError: Error in consuming message");
        }
        SubscriptionStatus|error subscriptionStatus = responseMessage.cloneWithType();
        if subscriptionStatus is error {
            self.attemptToCloseConnection();
            return error("[doUnsubscribe]DataBindingError: Error in cloning message");
        }
        return subscriptionStatus;
    }

    #
    remote isolated function doHeartbeat(decimal timeout) returns Heartbeat|error {
        Message|pipe:Error responseMessage = self.pipes.getPipe("heartbeat").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("[doHeartbeat]PipeError: Error in consuming message");
        }
        Heartbeat|error heartbeat = responseMessage.cloneWithType();
        if heartbeat is error {
            self.attemptToCloseConnection();
            return error("[doHeartbeat]DataBindingError: Error in cloning message");
        }
        return heartbeat;
    }

    #
    remote isolated function doSystemStatus(decimal timeout) returns SystemStatus|error {
        Message|pipe:Error responseMessage = self.pipes.getPipe("systemStatus").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("[doSystemStatus]PipeError: Error in consuming message");
        }
        SystemStatus|error systemStatus = responseMessage.cloneWithType();
        if systemStatus is error {
            self.attemptToCloseConnection();
            return error("[doSystemStatus]DataBindingError: Error in cloning message");
        }
        return systemStatus;
    }

    isolated function attemptToCloseConnection() {
        error? connectionClose = self->connectionClose();
        if connectionClose is error {
            log:printError("ConnectionError: " + connectionClose.message());
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
