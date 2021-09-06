@display {label: "Connection Config"}
public type ListenerConfiguration record {
    @display {label: "Listener Port"}
    int port;
    @display {label: "Verification Token"}
    string verificationToken;
};
