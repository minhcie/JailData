package com.cie;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
                System.out.println("\nPage " + n + "\n");
                PDPage page = document.getPage(n);
                stripper.extractRegions(page);

                // Parse arrest data.
                ArrestData data = new ArrestData();
                Arrestee arrestee = null;
                OffenseCode charge = null;

                String text = stripper.getTextForRegion("header");
                String[] lines = text.split("[\r\n]+");
                //System.out.println("Text in the header area: " + header + "\n");
                for (int i = 0; i < lines.length; i++) {
                    String s = lines[i];

                    // Ignore blank line.
                    if (s == null || s.length() <= 0) {
                        continue;
                    }

                    // @debug.
                    //System.out.println(s);

                    int k = s.indexOf("Effective Date:");
                    if (k >= 0) {
                        String temp = s.substring(k+15).trim();
                        if (temp != null && temp.length() > 0) {
                            data.effectiveDate = sdf.parse(temp);
                            System.out.println("Effective Date: " + temp);
                        }
                    }
                    if (i == 5) {
                        data.arrestingAgency = s.trim();
                        System.out.println("Arresting Agency: " + data.arrestingAgency);
                    }
                }
                System.out.println();

                text = stripper.getTextForRegion("content");
                lines = text.split("[\r\n]+");
                //System.out.println("Text in the content area: " + content);
                for (int i = 0; i < lines.length; i++) {
                    String s = lines[i].trim();

                    // Ignore blank line.
                    if (s == null || s.length() <= 0) {
                        continue;
                    }

                    // @debug.
                    //System.out.println(s);

                    // Parse names.
                    int lastNameIndex = s.indexOf("Last Name:");
                    int firstNameIndex = s.indexOf("First Name:");
                    int middleNameIndex = s.indexOf("Middle Name:");
                    int dobIndex = s.indexOf("DOB:");
                    if (lastNameIndex >= 0 || firstNameIndex >= 0) {
                        if (arrestee != null && charge != null) {
                            System.out.println(charge.arrestNum + " " + charge.chargeNum + " " + charge.code);
                            arrestee.charges.add(charge);
                            charge = null;
                        }
                        if (data != null && arrestee != null) {
                            data.arrestees.add(arrestee);
                        }
                        arrestee = new Arrestee();
                        arrestee.firstName = s.substring(firstNameIndex+11, middleNameIndex).trim();
                        arrestee.middleName = s.substring(middleNameIndex+12, dobIndex).trim();
                        arrestee.lastName = s.substring(lastNameIndex+10, firstNameIndex).trim();
                        System.out.println("\nFirst Name: " + arrestee.firstName);
                        System.out.println("Middle Name: " + arrestee.middleName);
                        System.out.println("Last Name: " + arrestee.lastName);
                        String temp = s.substring(dobIndex+4).trim();
                        if (temp != null && temp.length() > 0) {
                            System.out.println("DOB: " + temp);
                            arrestee.dob = sdf2.parse(temp);
                        }
                    }

                    // Parse charge info.
                    if (!s.contains("San Diego County") &&
                        !s.contains("Arresting Agency") &&
                        !s.contains("Last Name") &&
                        !s.contains("Arrnum Chgnum Code Section") &&
                        !s.contains("RJuInM")) { // Shield logo.
                        if (arrestee != null && charge != null) {
                            System.out.println(charge.arrestNum + " " + charge.chargeNum + " " + charge.code);
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

                if (arrestee != null && charge != null) {
                    System.out.println(charge.arrestNum + " " + charge.chargeNum + " " + charge.code);
                    arrestee.charges.add(charge);
                }
                if (data != null && arrestee != null) {
                    data.arrestees.add(arrestee);
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
}
