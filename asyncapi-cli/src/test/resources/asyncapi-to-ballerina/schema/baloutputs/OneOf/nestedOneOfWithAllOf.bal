public type UnSubscribe record {
    # Street No
    string streetNo?;
    # House number
    string houseNo?;
    # Street Name
    string streatName?;
    # Country Name
    string country?;
    string event;
}|record {
    # Zip code
    int zipCode?;
    string event;
};

public type Subscribe Time|Activity;

public type Time record {
    int minute;
    string second?;
    string hour?;
    string day?;
    string event;
};

public type Activity record {
    # Unique identifier for the activity
    string uuid?;
    string event;
};