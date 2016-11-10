package com.cie;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class DbUtils {

    private static final Logger log = Logger.getLogger(DbUtils.class.getName());

    private static final String DB_DRIVER = "org.postgresql.Driver";
    private static final String DB_CONNECTION = "jdbc:postgresql://cie-postgressql.crgoe3rbytwy.us-west-2.rds.amazonaws.com:5432/ciesddb";
    //private static final String DB_CONNECTION = "jdbc:postgresql://cie-postgressql.crgoe3rbytwy.us-west-2.rds.amazonaws.com:5432/cietestdb";
    private static final String DB_USER = "developer";
    private static final String DB_PASSWORD = "developer";

    public static Connection getDBConnection() {
        log.info("Establish DB Connection...");
 
        try { 
            Class.forName(DB_DRIVER);
        }
        catch (ClassNotFoundException e) {
            log.error("PostgreSQL JDBC driver not found!");
            e.printStackTrace();
            return null; 
        }
 
        log.info("PostgreSQL JDBC driver registered!"); 
        Connection connection = null;
 
        try { 
            connection = DriverManager.getConnection(DB_CONNECTION,
                                                     DB_USER, DB_PASSWORD); 
        }
        catch (SQLException e) {
            log.error("SQLException while trying to establish DB connection!");
            e.printStackTrace();
            return null; 
        }
 
        return connection;
    }

    public static void closeConnection(Connection conn) {
        log.info("Closing DB connection...");

        if (conn == null) {
            return;
        }

        try {
            conn.close();
        }
        catch (SQLException sqle) {
            log.error("Failed to close DB connection!");
        }
    }
}
