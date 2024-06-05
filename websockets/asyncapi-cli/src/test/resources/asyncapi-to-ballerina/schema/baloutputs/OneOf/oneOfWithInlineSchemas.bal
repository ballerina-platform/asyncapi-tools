public type CountryDetails record {
    string iso_code?;
    string name?;
    string event;
};

public type Subscribe record {
    int zipCode?;
    string event;
};

public type UnSubscribe Time|Activity|record {
    # Street Number
    string streetNo?;
    # House Number
    string houseNo?;
    string event;
}|record {
    # Street Name
    string streatName?;
    string event;
    # Country Name
    string country?;
}|record {
    # Zipcode
    int zipCode?;
    string event;
}|CountryDetails;

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
