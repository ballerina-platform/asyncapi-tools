public type Subscribe record {
    anydata pet_type;
    string event;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
};

public type Indicator record {
    string id?;
    string value?;
};

public type Country record {
    string id?;
    string value?;
};

public type AccessToElectricity record {
    Indicator indicator?;
    Country country?;
    string date?;
    int value?;
    int 'decimal?;
};

public type YouthLiteracyRate record {
    Indicator indicator?;
    Country country?;
    string date?;
    int value?;
    int 'decimal?;
};

public type GrossDomesticProduct record {
    Indicator indicator?;
    Country country?;
    string date?;
    int value?;
    int 'decimal?;
};

public type PrimaryEducationExpenditure record {
    Indicator indicator?;
    Country country?;
    string date?;
    int value?;
    int 'decimal?;
};

public type CountryPoplutation record {
    Indicator indicator?;
    Country country?;
    string date?;
    int value?;
    int 'decimal?;
};
