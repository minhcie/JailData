package com.cie;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.log4j.Logger;

public class DbClient {
    private static final Logger log = Logger.getLogger(DbClient.class.getName());

    public long id = 0;
    public long organizationId;
    public long genderId;
    public String firstName;
    public String middleName = "";
    public String lastName;
    public String suffix = "";
    public Date dob;
    public String ssn;
    public String caseNumber;
    public String address1;
    public String address2;
    public String city;
    public String state;
    public String postalCode;
    public String homePhone;
    public String cellPhone;
    public String email;
    public int veteranStatus;
    public int ethnicity;
    public boolean active = true;
    public UUID etoEnterpriseId;
    public long etoParticipantSiteId;
    public long etoSubjectId;

    public void insert(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("INSERT INTO client (organizationId, genderId, firstName, middleName, ");
            sb.append("lastName, suffix, dob, ssn, caseNumber, address1, address2, city, state, ");
            sb.append("postalCode, homePhone, cellPhone, email, veteranStatus, ethnicity, ");
            sb.append("active, etoEnterpriseId, etoParticipantSiteId, etoSubjectId) ");
            sb.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::uuid, ?, ?)");

            PreparedStatement ps = conn.prepareStatement(sb.toString(),
                                                         Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, this.organizationId);
            ps.setLong(2, this.genderId);
            ps.setString(3, SqlString.encode(this.firstName));
            ps.setString(4, SqlString.encode(this.middleName));
            ps.setString(5, SqlString.encode(this.lastName));
            ps.setString(6, SqlString.encode(this.suffix));
            java.sql.Date sqlDate = new java.sql.Date(this.dob.getTime());
            ps.setDate(7, sqlDate);
            ps.setString(8, SqlString.encode(this.ssn));
            ps.setString(9, SqlString.encode(this.caseNumber));
            ps.setString(10, SqlString.encode(this.address1));
            ps.setString(11, SqlString.encode(this.address2));
            ps.setString(12, SqlString.encode(this.city));
            ps.setString(13, SqlString.encode(this.state));
            ps.setString(14, SqlString.encode(this.postalCode));
            ps.setString(15, SqlString.encode(this.homePhone));
            ps.setString(16, SqlString.encode(this.cellPhone));
            ps.setString(17, SqlString.encode(this.email));
            ps.setInt(18, this.veteranStatus);
            ps.setInt(19, this.ethnicity);
            ps.setBoolean(20, this.active);
            ps.setObject(21, this.etoEnterpriseId);
            ps.setLong(22, this.etoParticipantSiteId);
            ps.setLong(23, this.etoSubjectId);

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to insert client record!");
            }
            else {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getLong(1);
                }
            }
        }
        catch (SQLException sqle) {
            log.error("SQLException DbClient.insert(): " + sqle);
        }
        catch (Exception e) {
            log.error("Exception DbClient.insert(): " + e);
        }
    }

    public void update(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("UPDATE client SET organizationId = ?, genderId = ?, firstName = ?, ");
            sb.append("middleName = ?, lastName = ?, suffix = ?, dob = ?, ssn = ?, ");
            sb.append("caseNumber = ?, address1 = ?, address2 = ?, city = ?, state = ?, ");
            sb.append("postalCode = ?, homePhone = ?, cellPhone = ?, email = ?, ");
            sb.append("veteranStatus = ?, ethnicity = ?, active = ?, ");
            sb.append("etoEnterpriseId = ?, etoParticipantSiteId = ?, etoSubjectId = ? ");
            sb.append("WHERE id = " + this.id);

            PreparedStatement ps = conn.prepareStatement(sb.toString());
            ps.setLong(1, this.organizationId);
            ps.setLong(2, this.genderId);
            ps.setString(3, SqlString.encode(this.firstName));
            ps.setString(4, SqlString.encode(this.middleName));
            ps.setString(5, SqlString.encode(this.lastName));
            ps.setString(6, SqlString.encode(this.suffix));
            java.sql.Date sqlDate = new java.sql.Date(this.dob.getTime());
            ps.setDate(7, sqlDate);
            ps.setString(8, SqlString.encode(this.ssn));
            ps.setString(9, SqlString.encode(this.caseNumber));
            ps.setString(10, SqlString.encode(this.address1));
            ps.setString(11, SqlString.encode(this.address2));
            ps.setString(12, SqlString.encode(this.city));
            ps.setString(13, SqlString.encode(this.state));
            ps.setString(14, SqlString.encode(this.postalCode));
            ps.setString(15, SqlString.encode(this.homePhone));
            ps.setString(16, SqlString.encode(this.cellPhone));
            ps.setString(17, SqlString.encode(this.email));
            ps.setInt(18, this.veteranStatus);
            ps.setInt(19, this.ethnicity);
            ps.setBoolean(20, this.active);
            ps.setObject(21, this.etoEnterpriseId);
            ps.setLong(22, this.etoParticipantSiteId);
            ps.setLong(23, this.etoSubjectId);

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to update client record!");
            }
        }
        catch (SQLException sqle) {
            log.error("SQLException in DbClient.update(): " + sqle);
        }
        catch (Exception e) {
            log.error("Exception in DbClient.update(): " + e);
        }
    }

    public static DbClient findByNameDob(Connection conn, String firstName,
                                         String lastName, Date dob) {
        DbClient client = null;
        try {
            if (dob != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                StringBuffer sb = new StringBuffer();
                sb.append("SELECT id, organizationId, genderId, firstName, middleName, ");
                sb.append("lastName, suffix, dob, ssn, caseNumber, etoEnterpriseId, ");
                sb.append("etoParticipantSiteId, etoSubjectId ");
                sb.append("FROM client ");
                if (firstName != null && lastName != null && dob != null) {
                    sb.append("WHERE firstName ~* '" + SqlString.encode(firstName) + "' ");
                    sb.append("  AND lastName ~* '" + SqlString.encode(lastName) + "' ");
                    sb.append("  AND dob = '" + sdf.format(dob) + "'");
                    log.info("DbClient.findByNameDob(): name = " + firstName + " " + lastName + ", dob = " + sdf.format(dob));
                }
                else if (lastName != null && dob != null) {
                    sb.append("WHERE lastName ~* '" + SqlString.encode(lastName) + "' ");
                    sb.append("  AND dob = '" + sdf.format(dob) + "'");
                    log.info("DbClient.findByNameDob(): last name = " + lastName + ", dob = " + sdf.format(dob));
                }
                else if (firstName != null && dob != null) {
                    sb.append("WHERE firstName ~* '" + SqlString.encode(firstName) + "' ");
                    sb.append("  AND dob = '" + sdf.format(dob) + "'");
                    log.info("DbClient.findByNameDob(): first name = " + firstName + ", dob = " + sdf.format(dob));
                }
                else if (firstName != null && lastName != null) {
                    sb.append("WHERE firstName ~* '" + SqlString.encode(firstName) + "' ");
                    sb.append("  AND lastName ~* '" + SqlString.encode(lastName) + "' ");
                    log.info("DbClient.findByNameDob(): name = " + firstName + " " + lastName);
                }
                else {
                    log.info("DbClient.findByNameDob() - Trying to find client with null names and dob.");
                    return null;
                }

                Statement statement = conn.createStatement();
                ResultSet rs = statement.executeQuery(sb.toString());
                if (rs.next()) {
                    client = new DbClient();
                    client.id = rs.getLong("id");
                    client.organizationId = rs.getLong("organizationId");
                    client.genderId = rs.getLong("genderId");
                    client.firstName = rs.getString("firstName");
                    client.middleName = rs.getString("middleName");
                    client.lastName = rs.getString("lastName");
                    client.suffix = rs.getString("suffix");
                    client.dob = rs.getDate("dob");
                    client.ssn = rs.getString("ssn");
                    client.caseNumber = rs.getString("caseNumber");
                    client.etoEnterpriseId = (UUID)rs.getObject("etoEnterpriseId");
                    client.etoParticipantSiteId = rs.getLong("etoParticipantSiteId");
                    client.etoSubjectId = rs.getLong("etoSubjectId");
                }

                rs.close();
                statement.close();
            }
        }
        catch (SQLException sqle) {
            log.error("SQLException in DbClient.findByNameDob(): " + sqle);
        }
        catch (Exception e) {
            log.error("Exception in DbClient.findByNameDob(): " + e);
        }
        return client;
    }
}
