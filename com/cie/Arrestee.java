package com.cie;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Arrestee {
    public String firstName;
    public String middleName;
    public String lastName;
    public Date dob;
    List<OffenseCode> charges;

    public Arrestee() {
        this.charges = new ArrayList<OffenseCode>();
    }
}
