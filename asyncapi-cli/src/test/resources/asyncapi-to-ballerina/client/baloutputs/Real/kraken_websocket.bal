import ballerina/websocket;
import xlibb/pipe;
import ballerina/lang.runtime;

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
    private final pipe:Pipe readMessageQueue;
    private final PipesMap pipes;
    private boolean isMessageWriting;
    private boolean isMessageReading;
    private boolean isPipeTriggering;
    private pipe:Pipe? pingPipe;
    private pipe:Pipe? subscribePipe;
    private pipe:Pipe? unsubscribePipe;
    private pipe:Pipe? heartbeatPipe;
    private pipe:Pipe? systemStatusPipe;
    # Gets invoked to initialize the `connector`.
    #
    # + config - The configurations to be used when initializing the `connector`
    # + serviceUrl - URL of the target service
    # + return - An error if connector initialization failed
    public isolated function init(websocket:ClientConfiguration clientConfig =  {}, string serviceUrl = "ws.kraken.com") returns error? {
        self.pipes = new ();
        self.writeMessageQueue = new (1000);
        self.readMessageQueue = new (1000);
        websocket:Client websocketEp = check new (serviceUrl, clientConfig);
        self.clientEp = websocketEp;
        self.pingPipe = ();
        self.subscribePipe = ();
        self.unsubscribePipe = ();
        self.heartbeatPipe = ();
        self.systemStatusPipe = ();
        self.isMessageWriting = true;
        self.isMessageReading = true;
        self.isPipeTriggering = true;
        self.startMessageWriting();
        self.startMessageReading();
        self.startPipeTriggering();
        return;
    }
    # Use to write messages to the websocket.
    #
    private isolated function startMessageWriting() {
        worker writeMessage returns error? {
            while self.isMessageWriting {
                anydata requestMessage = check self.writeMessageQueue.consume(5);
                check self.clientEp->writeMessage(requestMessage);
                runtime:sleep(0.01);
            }
        }
    }
    # Use to read messages from the websocket.
    #
    private isolated function startMessageReading() {
        worker readMessage returns error? {
            while self.isMessageReading {
                Message message = check self.clientEp->readMessage();
                check self.readMessageQueue.produce(message, 5);
                runtime:sleep(0.01);
            }
        }
    }
    # Use to map received message responses into relevant requests.
    #
    private isolated function startPipeTriggering() {
        worker pipeTrigger returns error? {
            while self.isPipeTriggering {
                Message message = check self.readMessageQueue.consume(5);
                string event = message.event;
                match (event) {
                    "Pong" => {
                        pipe:Pipe pingPipe = self.pipes.getPipe("ping");
                        check pingPipe.produce(message, 5);
                    }
                    "SubscriptionStatus" => {
                        pipe:Pipe subscribePipe = self.pipes.getPipe("subscribe");
                        check subscribePipe.produce(message, 5);
                    }
                    "SubscriptionStatus" => {
                        pipe:Pipe unsubscribePipe = self.pipes.getPipe("unsubscribe");
                        check unsubscribePipe.produce(message, 5);
                    }
                    "Heartbeat" => {
                        pipe:Pipe heartbeatPipe = self.pipes.getPipe("heartbeat");
                        check heartbeatPipe.produce(message, 5);
                    }
                    "SystemStatus" => {
                        pipe:Pipe systemStatusPipe = self.pipes.getPipe("systemStatus");
                        check systemStatusPipe.produce(message, 5);
                    }
                }
            }
        }
    }
    # Ping server to determine whether connection is alive
    #
    remote isolated function doPing(Ping ping, decimal timeout) returns Pong|error {
        if self.writeMessageQueue.isClosed() {
            return error("connection closed");
        }
        pipe:Pipe pingPipe;
        lock {
            self.pingPipe = self.pipes.getPipe("ping");
        }
        Message message = check ping.cloneWithType();
        check self.writeMessageQueue.produce(message, timeout);
        lock {
            pingPipe = check self.pingPipe.ensureType();
        }
        anydata responseMessage = check pingPipe.consume(timeout);
        Pong pong = check responseMessage.cloneWithType();
        return pong;
    }
    # Subscribe to a topic on a single or multiple currency pairs.
    #
    remote isolated function doSubscribe(Subscribe subscribe, decimal timeout) returns SubscriptionStatus|error {
        if self.writeMessageQueue.isClosed() {
            return error("connection closed");
        }
        pipe:Pipe subscribePipe;
        lock {
            self.subscribePipe = self.pipes.getPipe("subscribe");
        }
        Message message = check subscribe.cloneWithType();
        check self.writeMessageQueue.produce(message, timeout);
        lock {
            subscribePipe = check self.subscribePipe.ensureType();
        }
        anydata responseMessage = check subscribePipe.consume(timeout);
        SubscriptionStatus subscriptionStatus = check responseMessage.cloneWithType();
        return subscriptionStatus;
    }
    # Unsubscribe, can specify a channelID or multiple currency pairs.
    #
    remote isolated function doUnsubscribe(Unsubscribe unsubscribe, decimal timeout) returns SubscriptionStatus|error {
        if self.writeMessageQueue.isClosed() {
            return error("connection closed");
        }
        pipe:Pipe unsubscribePipe;
        lock {
            self.unsubscribePipe = self.pipes.getPipe("unsubscribe");
        }
        Message message = check unsubscribe.cloneWithType();
        check self.writeMessageQueue.produce(message, timeout);
        lock {
            unsubscribePipe = check self.unsubscribePipe.ensureType();
        }
        anydata responseMessage = check unsubscribePipe.consume(timeout);
        SubscriptionStatus subscriptionStatus = check responseMessage.cloneWithType();
        return subscriptionStatus;
    }
    #
    remote isolated function doHeartbeat(decimal timeout) returns Heartbeat|error {
        pipe:Pipe heartbeatPipe;
        lock {
            self.heartbeatPipe = self.pipes.getPipe("heartbeat");
        }
        lock {
            heartbeatPipe = check self.heartbeatPipe.ensureType();
        }
        anydata responseMessage = check heartbeatPipe.consume(timeout);
        Heartbeat heartbeat = check responseMessage.cloneWithType();
        return heartbeat;
    }
    #
    remote isolated function doSystemStatus(decimal timeout) returns SystemStatus|error {
        pipe:Pipe systemStatusPipe;
        lock {
            self.systemStatusPipe = self.pipes.getPipe("systemStatus");
        }
        lock {
            systemStatusPipe = check self.systemStatusPipe.ensureType();
        }
        anydata responseMessage = check systemStatusPipe.consume(timeout);
        SystemStatus systemStatus = check responseMessage.cloneWithType();
        return systemStatus;
    }
    remote isolated function closePingPipe() returns error? {
        lock {
            if self.pingPipe !is() {
                pipe:Pipe pingPipe = check self.pingPipe.ensureType();
                check pingPipe.gracefulClose();
            }
        }
    };
    remote isolated function closeSubscribePipe() returns error? {
        lock {
            if self.subscribePipe !is() {
                pipe:Pipe subscribePipe = check self.subscribePipe.ensureType();
                check subscribePipe.gracefulClose();
            }
        }
    };
    remote isolated function closeUnsubscribePipe() returns error? {
        lock {
            if self.unsubscribePipe !is() {
                pipe:Pipe unsubscribePipe = check self.unsubscribePipe.ensureType();
                check unsubscribePipe.gracefulClose();
            }
        }
    };
    remote isolated function closeHeartbeatPipe() returns error? {
        lock {
            if self.heartbeatPipe !is() {
                pipe:Pipe heartbeatPipe = check self.heartbeatPipe.ensureType();
                check heartbeatPipe.gracefulClose();
            }
        }
    };
    remote isolated function closeSystemStatusPipe() returns error? {
        lock {
            if self.systemStatusPipe !is() {
                pipe:Pipe systemStatusPipe = check self.systemStatusPipe.ensureType();
                check systemStatusPipe.gracefulClose();
            }
        }
    };
    remote isolated function connectionClose() returns error? {
        lock {
            self.isMessageReading = false;
            self.isMessageWriting = false;
            self.isPipeTriggering = false;
            check self.writeMessageQueue.immediateClose();
            check self.readMessageQueue.immediateClose();
            check self.pipes.removePipes();
            check self.clientEp->close();
        }
    };
}
