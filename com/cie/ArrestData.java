package com.cie;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ArrestData {
    public Date effectiveDate;
    public String arrestingAgency;
    List<Arrestee> arrestees;

    public ArrestData() {
        this.arrestees = new ArrayList<Arrestee>();
    }
}
