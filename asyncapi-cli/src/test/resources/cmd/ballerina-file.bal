import ballerina/websocket;
import ballerina/log;

listener websocket:Listener ep0 = new (80);

service /hello on ep0 {
    resource function get hi() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer(username)
    }
}

service class ChatServer {
    *websocket:Service;

    remote function onOpen(websocket:Caller caller) returns error? {
        string welcomeMsg = "Hi " + self.username + "! You have successfully connected to the chat";
        check caller->writeMessage(welcomeMsg);
        string msg = self.username + " connected to chat";
        check broadcast(msg);
        caller.setAttribute(USERNAME, self.username);
        lock {
            connectionsMap[caller.getConnectionId()] = caller;
        }
    }

    remote function onMessage(websocket:Caller caller, string text) returns error? {
        string msg = check getUsername(caller, USERNAME) + ": " + text;
        io:println(msg);
        @strand {
            thread:"any"
        }
        worker broadcast returns error? {
            check broadcast(msg);
        }
    }

    remote function onClose(websocket:Caller caller, int statusCode, string reason) returns error? {
        lock {
            _ = connectionsMap.remove(caller.getConnectionId());
        }
        string msg = check getUsername(caller, USERNAME) + " left the chat";
        check broadcast(msg);
    }
}

