package com.cie;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
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

    public static void main(String[] args) {
        Connection conn = null;

        try {
            conn = DbUtils.getDBConnection();
            if (conn == null) {
                System.exit(-1);
            }

            log.info("Reading document...\n");
            String fileName = "../Arrests.pdf";
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
            Rectangle header = new Rectangle(1, 1, 1224, 71);
            Rectangle content = new Rectangle(0, 72, 1224, 1590);
            stripper.addRegion("header", header);
            stripper.addRegion("content", content);

            int pages = document.getNumberOfPages();
            for (int n = 1; n < pages; n++) { // Ignore first page.
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
                    //log.info(s);

                    int k = s.indexOf("Effective Date:");
                    if (k >= 0) {
                        String temp = s.substring(k+15).trim();
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
                    log.info("@debug: " + s);

                    // Parse names.
                    int lastNameIndex = s.indexOf("Last Name:");
                    int firstNameIndex = s.indexOf("First Name:");
                    int middleNameIndex = s.indexOf("Middle Name:");
                    int dobIndex = s.indexOf("DOB:");
                    if (lastNameIndex >= 0 || firstNameIndex >= 0) {
                        if (arrestee != null && charge != null) {
                            // Add charge info.
                            log.info(charge.arrestNum + " " + charge.chargeNum + " " + charge.code);
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
                            log.info(charge.arrestNum + " " + charge.chargeNum + " " + charge.code);
                            arrestee.charges.add(charge);
                        }
                        charge = new OffenseCode();
                        String[] codes = s.split(" ");
                        for (int k = 0; k < codes.length; k++) {
                            switch (k) {
                                case 0:
                                    charge.arrestNum = Integer.parseInt(codes[k].trim());
                                    break;
                                case 1:
                                    charge.chargeNum = Integer.parseInt(codes[k].trim());
                                    break;
                                default:
                                    if (charge.code != null && charge.code.length() > 0) {
                                        charge.code += " ";
                                        charge.code += codes[k].trim();
                                    }
                                    else {
                                        charge.code = codes[k].trim();
                                    }
                                    break;
                            }
                        }
                    }
                }

                // Add charge info.
                if (arrestee != null && charge != null) {
                    log.info(charge.arrestNum + " " + charge.chargeNum + " " + charge.code);
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
                        DbClient c = DbClient.findByNameDob(conn, arrestedInfo.firstName,
                                                            arrestedInfo.lastName, arrestedInfo.dob);
                        if (c != null) {
                            log.info("*** Opted-in client found, confidence level: 90%");
                            found = true;
                            confidence = 90;
                        }
                        else {
                            c = DbClient.findByNameDob(conn, null, arrestedInfo.lastName,
                                                       arrestedInfo.dob);
                            if (c != null) {
                                log.info("*** Opted-in client found, confidence level: 70%");
                                found = true;
                                confidence = 70;
                            }
                            else {
                                c = DbClient.findByNameDob(conn, arrestedInfo.firstName,
                                                           null, arrestedInfo.dob);
                                if (c != null) {
                                    log.info("*** Opted-in client found, confidence level: 60%");
                                    found = true;
                                    confidence = 60;
                                }
                                else {
                                    c = DbClient.findByNameDob(conn, arrestedInfo.firstName,
                                                               arrestedInfo.lastName, null);
                                    if (c != null) {
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

                        // Notify the client's case manager if a match is found.
                        if (found && c != null) {
                            log.info("First Name: " + c.firstName);
                            log.info("Middle Name: " + c.lastName);
                            log.info("\n");
                            sendEmail(data.effectiveDate, arrestedInfo);
                        }
                    }
                }
            }

            // test email
            sendEmail(null, null);

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

    private static void sendEmail(Date arrestedDate, Arrestee arrestedInfo) throws Exception {
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

        Message msg = new MimeMessage(mailSession);

        // Set the FROM, TO, DATE and SUBJECT fields.
        msg.setFrom(new InternetAddress("minh@ciesandiego.org"));
        msg.setRecipients(Message.RecipientType.TO,InternetAddress.parse("sddolphins@gmail.com"));
        msg.setSentDate(new Date());
        msg.setSubject("Test email send from CIE");

        // Email body.
        msg.setText("Hello from my first e-mail sent with JavaMail");

        // Send email message. 
        Transport.send( msg );
    }
}
