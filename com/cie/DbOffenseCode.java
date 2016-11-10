package com.cie;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class DbOffenseCode {
    private static final Logger log = Logger.getLogger(DbOffenseCode.class.getName());

    public long id;
    public String code;
    public String codeType;
    public String description;

    public static DbOffenseCode findByCode(Connection conn, String code) {
        DbOffenseCode oc = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, code, codeType, description ");
            sb.append("FROM offense_code ");
            sb.append("WHERE code = '" + code + "'");

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                oc = new DbOffenseCode();
                oc.id = rs.getLong("id");
                oc.code = rs.getString("code");
                oc.codeType = rs.getString("codeType");
                oc.description = rs.getString("description");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
            log.error("SQLException in DbOffenseCode.findByCode(): " + sqle);
        }
        catch (Exception e) {
            log.error("Exception in DbOffenseCode.findByCode(): " + e);
        }
        return oc;
    }
}
