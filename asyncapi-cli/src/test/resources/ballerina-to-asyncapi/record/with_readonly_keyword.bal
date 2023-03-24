import ballerina/websocket;

configurable int port = 8080;

type Album readonly & record {|
    string id;
    readonly & string title;
    string artist;
    decimal price;
    string event;
|};

table<Album> key(id) albums = table [
        {event: "Album",id: "1", title: "Blue Train", artist: "John Coltrane", price: 56.99},
        {event:"Album",id: "2", title: "Jeru", artist: "Gerry Mulligan", price: 17.99},
        {event:"Album",id: "3", title: "Sarah Vaughan and Clifford Brown", artist: "Sarah Vaughan", price: 39.99}
    ];

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on new websocket:Listener(port) {
    resource function get albums/[string id]() returns websocket:Service| websocket:UpgradeError{
        return new ChatServer();

    }

}

service class ChatServer{
    *websocket:Service;


    remote function onAlbum(Album message, websocket:Caller caller) returns string[] {
        return ["sg","hello"];
    }


}



