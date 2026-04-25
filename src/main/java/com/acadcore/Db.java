package com.acadcore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {
    // Set these using env vars or JVM properties to avoid hardcoding secrets in code:
    // ACADCORE_DB_URL / db.url
    // ACADCORE_DB_USER / db.user
    // ACADCORE_DB_PASS / db.pass
    private static final String URL = getConfig(
            "ACADCORE_DB_URL",
            "db.url",
            "jdbc:mysql://sql12.freesqldatabase.com:3306/sql12824045?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String USER = getConfig("ACADCORE_DB_USER", "db.user", "sql12824045");
    private static final String PASS = getConfig("ACADCORE_DB_PASS", "db.pass", "JHd2nUlMXV");

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found in classpath", e);
        }

        if (!URL.startsWith("jdbc:mysql://")) {
            throw new SQLException("Invalid MySQL JDBC URL: " + URL + ". Example: jdbc:mysql://host:3306/database");
        }

        return DriverManager.getConnection(URL, USER, PASS);
    }

    private static String getConfig(String envKey, String propKey, String defaultValue) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }

        String propVal = System.getProperty(propKey);
        if (propVal != null && !propVal.isBlank()) {
            return propVal.trim();
        }

        return defaultValue;
    }
}
      