package com.cie;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.lang.NumberFormatException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import org.apache.log4j.Logger;

public class JailData {

    private static final Logger log = Logger.getLogger(JailData.class.getName());

    static void usage() {
        System.err.println("usage: java -jar JailData.jar <data.pdf>");
        System.err.println("");
        System.exit(-1);
    }

    public static void main(String[] args) {
        if (args.length == 0 || args.length < 1) {
            usage();
        }

        String fileName = args[0];
        Connection conn = null;
        try {
            conn = DbUtils.getDBConnection();
            if (conn == null) {
                System.exit(-1);
            }

            log.info("Reading arrest data file name: " + fileName + "...\n");
            File f = new File(fileName);
            if (!f.exists()) {
                log.error("File not found!");
                System.exit(-1);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
            SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MMM-yy");

            PDDocument document = PDDocument.load(f);
            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.setSortByPosition(true);

            // Set extract regions.
            Rectangle header = new Rectangle(0, 1, 1224, 71);
            Rectangle content = new Rectangle(0, 72, 1224, 1590);
            stripper.addRegion("header", header);
            stripper.addRegion("content", content);

            int pages = document.getNumberOfPages();
            //for (int n = 1; n < pages; n++) { // Ignore first page.
            for (int n = 24; n < 25; n++) { // Test data.
                log.info("\n");
                log.info("Page " + n + "\n");
                PDPage page = document.getPage(n);
                stripper.extractRegions(page);

                // Parse arrest data.
                ArrestData data = new ArrestData();
                Arrestee arrestee = null;
                OffenseCode charge = null;

                // Parse page header.
                String text = stripper.getTextForRegion("header");
                String[] lines = text.split("[\r\n]+");
                //log.info("Text in the header area: " + header + "\n");
                for (int i = 0; i < lines.length; i++) {
                    String s = lines[i];

                    // Ignore blank line.
                    if (s == null || s.length() <= 0) {
                        continue;
                    }

                    // @debug.
                    //log.info("@debug: " + s);

                    //int k = s.indexOf("Effective Date:");
                    int k = s.indexOf("TO");
                    if (k >= 0) {
                        //String temp = s.substring(k+15).trim();
                        String temp = s.substring(k+2).trim();
                        if (temp != null && temp.length() > 0) {
                            data.effectiveDate = sdf.parse(temp);
                            log.info("Effective Date: " + temp);
                        }
                    }
                    if (i == 5) {
                        data.arrestingAgency = s.trim();
                        log.info("Arresting Agency: " + data.arrestingAgency);
                    }
                }
                log.info("\n");

                // Parse page content.
                text = stripper.getTextForRegion("content");
                lines = text.split("[\r\n]+");
                //log.info("Text in the content area: " + content);
                for (int i = 0; i < lines.length; i++) {
                    String s = lines[i].trim();

                    // Ignore blank line.
                    if (s == null || s.length() <= 0) {
                        continue;
                    }

                    // @debug.
                    //log.info("@debug: " + s);

                    // Parse names.
                    int lastNameIndex = s.indexOf("Last Name:");
                    int firstNameIndex = s.indexOf("First Name:");
                    int middleNameIndex = s.indexOf("Middle Name:");
                    int dobIndex = s.indexOf("DOB:");
                    if (lastNameIndex >= 0 || firstNameIndex >= 0) {
                        if (arrestee != null && charge != null) {
                            // Add charge info.
                            //log.info(charge.arrestNum + " " + charge.chargeNum + " " + charge.code);
                            arrestee.charges.add(charge);
                            charge = null;
                        }
                        if (data != null && arrestee != null) {
                            // Add arrest info.
                            data.arrestees.add(arrestee);
                        }

                        // New arrestee.
                        arrestee = new Arrestee();
                        if (middleNameIndex > -1) {
                            arrestee.firstName = s.substring(firstNameIndex+11, middleNameIndex).trim();
                            arrestee.middleName = s.substring(middleNameIndex+12, dobIndex).trim();
                        }
                        else {
                            // Parse first name.
                            String temp[] = s.substring(firstNameIndex+11).trim().split(" ");
                            arrestee.firstName = temp[0];
                            arrestee.middleName = temp[temp.length-2];
                        }
                        arrestee.lastName = s.substring(lastNameIndex+10, firstNameIndex).trim();
                        log.info("\n");
                        log.info("First Name: " + arrestee.firstName);
                        log.info("Middle Name: " + arrestee.middleName);
                        log.info("Last Name: " + arrestee.lastName);
                        String temp = s.substring(dobIndex+4).trim();
                        if (temp != null && temp.length() > 0) {
                            log.info("DOB: " + temp);
                            arrestee.dob = sdf2.parse(temp);
                        }
                    }

                    // Parse charge info.
                    if (!s.contains("San Diego County") &&
                        !s.contains("Arresting Agency") &&
                        !s.contains("Last Name") &&
                        !s.contains("Arrnum Chgnum") &&
                        !s.contains("Code Section") &&
                        !s.contains("250000FA_MEDIA") &&
                        !s.contains("RJuInM")) { // Shield logo.
                        if (arrestee != null && charge != null) {
                            // Add charge info.
                            //log.info(charge.arrestNum + " " + charge.chargeNum + " " + charge.code);
                            arrestee.charges.add(charge);
                        }
                        charge = new OffenseCode();
                        String[] codes = s.split(" ");
                        for (int k = 0; k < codes.length; k++) {
                            switch (k) {
                                case 0:
                                    try {
                                        charge.arrestNum = Integer.parseInt(codes[k].trim());
                                    }
                                    catch (NumberFormatException nfe) {
                                        // Do nothing, continue to next line.
                                        k = codes.length;
                                        continue;
                                    }
                                    break;
                                case 1:
                                    try {
                                        charge.chargeNum = Integer.parseInt(codes[k].trim());
                                    }
                                    catch (NumberFormatException nfe) {
                                        // Do nothing, continue to next line.
                                        k = codes.length;
                                        continue;
                                    }
                                    break;
                                case 2:
                                    String tmp = codes[k].trim();
                                    charge.code = tmp;
                                    DbOffenseCode oc = null;

                                    // Fetch charge description.
                                    if (tmp.contains("(A)(B)")) { // Multiple codes.
                                        String code1 = tmp.replace("(B)", "");
                                        oc = DbOffenseCode.findByCode(conn, code1);
                                        if (oc != null) {
                                            log.info("Charge code: " + code1 + " - " + oc.description);
                                        }
                                        else {
                                            log.info("Charge code: " + code1);
                                        }

                                        String code2 = tmp.replace("(A)", "");
                                        oc = DbOffenseCode.findByCode(conn, code2);
                                        if (oc != null) {
                                            log.info("Charge code: " + code2 + " - " + oc.description);
                                        }
                                        else {
                                            log.info("Charge code: " + code2);
                                        }
                                    }
                                    else {
                                        oc = DbOffenseCode.findByCode(conn, charge.code);
                                        if (oc != null) {
                                            log.info("Charge code: " + charge.code + " - " + oc.description);
                                        }
                                        else {
                                            log.info("Charge code: " + charge.code);
                                        }
                                    }
                                    break;
                                case 3:
                                    charge.codeType = codes[k].trim();
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }

                // Add charge info.
                if (arrestee != null && charge != null) {
                    //log.info(charge.arrestNum + " " + charge.chargeNum + " " + charge.code);
                    arrestee.charges.add(charge);
                }

                // Add arrest info
                if (data != null && arrestee != null) {
                    data.arrestees.add(arrestee);

                    // Loop thru all arrestees and find matching opted-in client.
                    log.info("\n");
                    log.info("*** Checking for any matching client...");
                    for (int i = 0; i < data.arrestees.size(); i++) {
                        Arrestee arrestedInfo = data.arrestees.get(i);

                        boolean found = false;
                        int confidence = 0;
                        List<DbClient> results = DbClient.findByNameDob(conn,
                                                                        arrestedInfo.firstName,
                                                                        arrestedInfo.lastName,
                                                                        arrestedInfo.dob);
                        if (results != null && results.size() > 0) {
                            log.info("*** Opted-in client found, confidence level: 90%");
                            found = true;
                            confidence = 90;
                        }
                        else {
                            results = DbClient.findByNameDob(conn, null,
                                                             arrestedInfo.lastName,
                                                             arrestedInfo.dob);
                            if (results != null && results.size() > 0) {
                                log.info("*** Opted-in client found, confidence level: 70%");
                                found = true;
                                confidence = 70;
                            }
                            else {
                                results = DbClient.findByNameDob(conn, arrestedInfo.firstName,
                                                                 null, arrestedInfo.dob);
                                if (results != null && results.size() > 0) {
                                    log.info("*** Opted-in client found, confidence level: 60%");
                                    found = true;
                                    confidence = 60;
                                }
                                else {
                                    results = DbClient.findByNameDob(conn,
                                                                     arrestedInfo.firstName,
                                                                     arrestedInfo.lastName,
                                                                     null);
                                    if (results != null && results.size() > 0) {
                                        log.info("*** Opted-in client found, confidence level: 50%");
                                        found = true;
                                        confidence = 50;
                                    }
                                    else {
                                        log.info("*** Client not found!");
                                    }
                                }
                            }
                        }

                        // Notify the client's case manager(s) if a match is found.
                        if (found && results != null && results.size() > 0) {
                            log.info("*** " + results.size() + " matching clients found...");
                            long prevOrdId = 0;
                            String prevFirstName = "";
                            String prevLastName = "";
                            long prevDob = 0;
                            for (int ii = 0; ii < results.size(); ii++) {
                                DbClient c = results.get(ii);

                                // If duplicate client, do not send notification again.
                                if (prevOrdId == c.organizationId) {
                                    continue;
                                }
                                else {
                                    prevOrdId = c.organizationId;
                                }

                                // Insert arrest record for each charge code.
                                for (int k = 0; k < arrestedInfo.charges.size(); k++) {
                                    OffenseCode oc = arrestedInfo.charges.get(k);
                                    DbArrest rec = new DbArrest();
                                    rec.clientId = c.id;
                                    rec.arrestDate = data.effectiveDate;
                                    rec.arrestingAgency = data.arrestingAgency;
                                    rec.arrestNumber = oc.arrestNum;
                                    rec.chargeNumber = oc.chargeNum;
                                    rec.offenseCode = oc.code + " " + oc.codeType;
                                    rec.matchConfidence = confidence;

                                    // Save arrest info.
                                    log.info("Save arrest info for client id: " + c.id);
                                    rec.insert(conn);
                                }

                                // Look up client's open enrollments.
                                List<DbEnrollment> enrolls = DbEnrollment.findOpenEnrollment(conn, c.id);
                                if (enrolls != null && enrolls.size() > 0) {
                                    // Send notification email to the case manager with
                                    // matching program.
                                    for (int m = 0; m < enrolls.size(); m++) {
                                        DbEnrollment e = enrolls.get(m);
                                        DbGenericCaseManager mgr = DbGenericCaseManager.findByOrganizationProgram(conn,
                                                                                                                  c.organizationId,
                                                                                                                  e.etoProgramId);
                                        if (mgr != null) {
                                            sendEmail(conn, mgr.email, data.effectiveDate,
                                                      data.arrestingAgency, arrestedInfo,
                                                      confidence);
                                        }
                                    }
                                }

                                // Also send notification email to the case manager
                                // for all programs.
                                DbGenericCaseManager mgr = DbGenericCaseManager.findByOrganization(conn,
                                                                                                   c.organizationId);
                                if (mgr != null) {
                                    sendEmail(conn, mgr.email, data.effectiveDate,
                                              data.arrestingAgency, arrestedInfo,
                                              confidence);
                                }

                                // Populate ETO touchpoint.  One per client regardless
                                // of which organization.
                                boolean addTouchPoint = false;
                                switch (confidence) {
                                    case 90:
                                        if (!c.firstName.equalsIgnoreCase(prevFirstName) ||
                                            !c.lastName.equalsIgnoreCase(prevLastName) ||
                                            c.dob.getTime() != prevDob) {
                                            addTouchPoint = true;
                                            prevFirstName = c.firstName;
                                            prevLastName = c.lastName;
                                            prevDob = c.dob.getTime();
                                        }
                                        break;
                                    case 70:
                                        if (!c.lastName.equalsIgnoreCase(prevLastName) ||
                                            c.dob.getTime() != prevDob) {
                                            addTouchPoint = true;
                                            prevFirstName = "";
                                            prevLastName = c.lastName;
                                            prevDob = c.dob.getTime();
                                        }
                                        break;
                                    case 60:
                                        if (!c.firstName.equalsIgnoreCase(prevFirstName) ||
                                            c.dob.getTime() != prevDob) {
                                            addTouchPoint = true;
                                            prevFirstName = c.firstName;
                                            prevLastName = "";
                                            prevDob = c.dob.getTime();
                                        }
                                        break;
                                    case 50:
                                        if (!c.firstName.equalsIgnoreCase(prevFirstName) ||
                                            !c.lastName.equalsIgnoreCase(prevLastName)) {
                                            addTouchPoint = true;
                                            prevFirstName = c.firstName;
                                            prevLastName = c.lastName;
                                            prevDob = 0;
                                        }
                                        break;
                                    default:
                                        break;
                                }
                                if (addTouchPoint && c.etoSubjectId != 0) {
                                    TouchPointUtils.addArrestData(conn, c.organizationId,
                                                                  c.etoSubjectId,
                                                                  data.effectiveDate,
                                                                  data.arrestingAgency,
                                                                  arrestedInfo, confidence);
                                }
                            }
                        }
                    }
                }
            }

            // Close document.
            document.close();
        }
        catch (IOException ioe) {
            log.error(ioe.getMessage());
            ioe.printStackTrace();
        } 
        catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        finally {
            DbUtils.closeConnection(conn);
        }
    }

    private static void sendEmail(Connection conn, String mgrEmailAddr,
                                  Date arrestedDate, String arrestedAgency,
                                  Arrestee arrestedInfo, int confidence) throws Exception {
        // Format email message.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        SimpleDateFormat sdf2 = new SimpleDateFormat("MM/dd/yyyy");
        StringBuffer msg = new StringBuffer();
        msg.append("\n");
        msg.append("***** Client match confidence " + confidence + "% *****");
        msg.append("\n");
        msg.append("CIE auto-notification for client: ");
        msg.append(arrestedInfo.firstName + " " + arrestedInfo.lastName);
        msg.append(" (XX/XX/" + sdf.format(arrestedInfo.dob) + ")");
        msg.append("\n");
        msg.append("Client was arrested on " + sdf2.format(arrestedDate));
        msg.append(" by " + arrestedAgency);
        msg.append(" with the following charge(s):");
        msg.append("\n");
        for (int i = 0; i < arrestedInfo.charges.size(); i++) {
            OffenseCode oc = arrestedInfo.charges.get(i);

            // Retrieve charge description.
            DbOffenseCode dboc = null;
            String tmp = oc.code;
            if (tmp.contains("(A)(B)")) { // Multiple codes.
                String code1 = tmp.replace("(B)", "");
                dboc = DbOffenseCode.findByCode(conn, code1);
                if (dboc != null) {
                    msg.append(code1 + " - " + dboc.description);
                }
                else {
                    msg.append(code1);
                }

                String code2 = tmp.replace("(A)", "");
                dboc = DbOffenseCode.findByCode(conn, code2);
                if (dboc != null) {
                    msg.append(code2 + " - " + dboc.description);
                }
                else {
                    msg.append(code2);
                }
            }
            else {
                dboc = DbOffenseCode.findByCode(conn, tmp);
                if (dboc != null) {
                    msg.append(tmp + " - " + dboc.description);
                }
                else {
                    msg.append(tmp);
                }
            }
            msg.append("\n");
        }
        msg.append("\n");
        msg.append("In reviewing the arrest data, please be aware that the initial booking charges may be ");
        msg.append("modified, dropped or expanded as the case progresses through the criminal justice ");
        msg.append("system. To further check inmate status, you may access the San Diego County Sheriff’s ");
        msg.append("Department Who Is In Jail application (http://apps.sdsheriff.net/wij/wij.aspx) and ");
        msg.append("also use its direct link to track release date through the Victim Identification and ");
        msg.append("Notification system (www.vinelink.com).");
        msg.append("\n");

        // Config email properties.
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.auth", "true");
        props.put("mail.debug", "true"); 
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");

        Session mailSession = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("minh@ciesandiego.org", "m1nh@cie");
            }
        });

        // Enable the debug mode.
        mailSession.setDebug(true);

        Message mail = new MimeMessage(mailSession);

        // Set the FROM, TO, DATE and SUBJECT fields.
        mail.setFrom(new InternetAddress("minh@ciesandiego.org"));
        mail.setRecipients(Message.RecipientType.TO,
                          InternetAddress.parse(mgrEmailAddr));
        mail.setSentDate(new Date());
        mail.setSubject("CIE San Diego - Arrest Notification");

        // Email body.
        mail.setText(msg.toString());

        // Send email message. 
        log.info("Send notification email to: " + mgrEmailAddr);
        Transport.send(mail);
    }
}
