package com.cie;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.apache.log4j.Logger;

public class DbArrest {
    private static final Logger log = Logger.getLogger(DbArrest.class.getName());

    public long id;
    public long clientId;
    public Date arrestDate;
    public String arrestingAgency;
    public int arrestNumber;
    public int chargeNumber;
    public String offenseCode;

    public void insert(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("INSERT INTO arrest (clientId, arrestDate, arrestingAgency, ");
            sb.append("arrestNumber, chargeNumber, offenseCode ");
            sb.append("VALUES (?, ?, ?, ?, ?, ?)");

            PreparedStatement ps = conn.prepareStatement(sb.toString(),
                                                         Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, this.clientId);
            java.sql.Date sqlDate = new java.sql.Date(this.arrestDate.getTime());
            ps.setDate(2, sqlDate);
            ps.setString(3, SqlString.encode(this.arrestingAgency));
            ps.setInt(4, this.arrestNumber);
            ps.setInt(5, this.chargeNumber);
            ps.setString(6, SqlString.encode(this.offenseCode));

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to insert arrest record!");
            }
            else {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getLong(1);
                }
            }
        }
        catch (SQLException sqle) {
            log.error("SQLException DbArrest.insert(): " + sqle);
        }
        catch (Exception e) {
            log.error("Exception DbArrest.insert(): " + e);
        }
    }
}
