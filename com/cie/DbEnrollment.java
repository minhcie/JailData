package com.cie;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

public class DbEnrollment {
    private static final Logger log = Logger.getLogger(DbEnrollment.class.getName());

    public long id;
    public long clientId;
    public long programId;
    public Date startDate;
    public Date endDate;
    public long dismissalReasonId;
    public String dismissalReasonOther;
    public long etoProgramId;

    public static List<DbEnrollment> findOpenEnrollment(Connection conn, long clientId) {
        List<DbEnrollment> results = new ArrayList<DbEnrollment>();
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT e.id, e.clientId, e.programId, e.dismissalReasonId, ");
            sb.append("e.dismissalReasonOther, e.startDate, e.endDate, p.etoProgramId ");
            sb.append("FROM enrollment e ");
            sb.append("INNER JOIN program p ON p.id = e.programId ");
            sb.append("WHERE e.clientId = " + clientId);
            sb.append("  AND e.endDate IS NULL ");
            sb.append("ORDER BY p.etoProgramId ");

            // @debug.
            log.info("DbEnrollment.findOpenEnrollment() - " + sb.toString());
            
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            while (rs.next()) {
                DbEnrollment enroll = new DbEnrollment();
                enroll.id = rs.getLong("id");
                enroll.clientId = rs.getLong("clientId");
                enroll.programId = rs.getLong("programId");
                enroll.startDate = rs.getDate("startDate");
                enroll.endDate = rs.getDate("endDate");
                enroll.dismissalReasonId = rs.getLong("dismissalReasonId");
                enroll.dismissalReasonOther = rs.getString("dismissalReasonOther");
                enroll.etoProgramId = rs.getLong("etoProgramId");
                results.add(enroll);
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbEnrollment.findOpenEnrollment(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbEnrollment.findOpenEnrollment(): " + e);
        }
        return results;
    }
}
