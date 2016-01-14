package com.cie;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class DbGenericCaseManager {
    private static final Logger log = Logger.getLogger(DbGenericCaseManager.class.getName());

    public long id;
    public long organizationId;
    public long etoProgramId;
    public String etoProgramName;
    public String firstName;
    public String lastName;
    public String email;
    public String phone;

    public static DbGenericCaseManager findByOrganizationProgram(Connection conn, long orgId, long programId) {
        DbGenericCaseManager caseMgr = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, organizationId, etoProgramId, etoProgramName, ");
            sb.append("firstName, lastName, email, phone ");
            sb.append("FROM generic_case_manager ");
            sb.append("WHERE organizationId = " + orgId);

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            while (rs.next()) {
                long etoPrgmId = rs.getLong("etoProgramId");
                if (etoPrgmId == programId) {
                    // Found case manager with matching program.
                    caseMgr = caseManagerInfo(rs);
                    break;
                }
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
            log.error("SQLException in DbGenericCaseManager.findByOrganizationProgram(): " + sqle);
        }
        catch (Exception e) {
            log.error("Exception in DbGenericCaseManager.findByOrganizationProgram(): " + e);
        }
        return caseMgr;
    }

    public static DbGenericCaseManager findByOrganization(Connection conn, long orgId) {
        DbGenericCaseManager caseMgr = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, organizationId, etoProgramId, etoProgramName, ");
            sb.append("firstName, lastName, email, phone ");
            sb.append("FROM generic_case_manager ");
            sb.append("WHERE organizationId = " + orgId);

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            while (rs.next()) {
                long etoPrgmId = rs.getLong("etoProgramId");
                if (etoPrgmId == 0) {
                    // Case manager for all programs.
                    caseMgr = caseManagerInfo(rs);
                    break;
                }
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
            log.error("SQLException in DbGenericCaseManager.findByOrganization(): " + sqle);
        }
        catch (Exception e) {
            log.error("Exception in DbGenericCaseManager.findByOrganization(): " + e);
        }
        return caseMgr;
    }

    private static DbGenericCaseManager caseManagerInfo(ResultSet rs) throws SQLException {
        DbGenericCaseManager caseMgr = new DbGenericCaseManager();
        caseMgr.id = rs.getLong("id");
        caseMgr.organizationId = rs.getLong("organizationId");
        caseMgr.etoProgramId = rs.getLong("etoProgramId");
        caseMgr.etoProgramName = rs.getString("etoProgramName");
        caseMgr.firstName = rs.getString("firstName");
        caseMgr.lastName = rs.getString("lastName");
        caseMgr.email = rs.getString("email");
        caseMgr.phone = rs.getString("phone");
        return caseMgr;
    }
}
