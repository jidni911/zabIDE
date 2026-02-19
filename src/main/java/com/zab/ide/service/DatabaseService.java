package com.zab.ide.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DatabaseService {

    private Connection connection = null;
    private String currentServer = "localhost";
    private String currentDatabase = "ZABDB";
    private String currentUser = "sa";
    private String currentPassword = "sql@s3rv3r";

    /**
     * Connect to database
     * 
     * @throws ClassNotFoundException
     */
    public Connection connectToDatabase(String serverName, String username, String password, String dbName)
            throws SQLException, ClassNotFoundException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        // Close existing connection if any
        if (connection != null && !connection.isClosed()) {
            System.out.println("Closing existing connection");
            connection.close();
        }
        if (serverName == null || serverName.equals("")) {
            serverName = currentServer;
        }
        if (username == null || username.equals("")) {
            username = currentUser;
        }
        if (password == null || password.equals("")) {
            password = currentPassword;
        }
        if (dbName == null || dbName.equals("")) {
            dbName = currentDatabase;
        }

        String url = "jdbc:sqlserver://" + serverName + ";databaseName=" + dbName
                + ";encrypt=false;trustServerCertificate=true;";
        System.out.println("Connecting to: " + url);
        System.out.println("Using driver: " + DriverManager.getDriver(url));

        connection = DriverManager.getConnection(url, username, password);
        currentServer = serverName;
        currentDatabase = dbName;
        currentUser = username;
        currentPassword = password;
        return connection;
    }

    /**
     * Execute query and return results with metadata
     */
    public Map<String, Object> executeQuery(String query) throws SQLException {
        ensureConnection();

        Map<String, Object> result = new HashMap<String, Object>();

        try (Statement stmt = connection.createStatement()) {
            String trimmedQuery = query.trim().toLowerCase();

            if (trimmedQuery.startsWith("select") || trimmedQuery.startsWith("show") ||
                    trimmedQuery.startsWith("describe") || trimmedQuery.startsWith("explain")) {
                // SELECT query - return result set
                ResultSet rs = stmt.executeQuery(query);
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Get column names
                List<String> columns = new ArrayList<String>();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(metaData.getColumnName(i));
                }

                // Get rows
                List<List<Object>> rows = new ArrayList<List<Object>>();
                while (rs.next()) {
                    List<Object> row = new ArrayList<Object>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    rows.add(row);
                }

                result.put("type", "select");
                result.put("columns", columns);
                result.put("rows", rows);
                result.put("rowCount", rows.size());
                result.put("message", rows.size() + " row(s) returned");

            } else {
                // DML/DDL query - return affected rows
                int rowsAffected = stmt.executeUpdate(query);
                result.put("type", "update");
                result.put("rowsAffected", rowsAffected);
                result.put("message", "Query executed successfully. " + rowsAffected + " row(s) affected");
            }
        }

        return result;
    }

    /**
     * Get list of databases
     */
    public List<String> getDatabases() throws SQLException {
        ensureConnection();

        String query = "SELECT name FROM sys.databases ORDER BY name";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            List<String> databases = new ArrayList<String>();
            while (rs.next()) {
                databases.add(rs.getString(1));
            }
            return databases;
        }
    }

    /**
     * Get tables for a database
     */
    public List<String> getTables(String dbName) throws SQLException {
        ensureConnection();

        String query = "SELECT table_name FROM " + dbName + ".information_schema.tables " +
                "WHERE table_type = 'BASE TABLE' ORDER BY table_name";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            List<String> tables = new ArrayList<String>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            return tables;
        }
    }

    /**
     * Get columns for a table
     */
    public List<Map<String, Object>> getTableColumns(String dbName, String tableName) throws SQLException {
        ensureConnection();

        String query = "SELECT column_name, data_type, character_maximum_length, is_nullable " +
                "FROM " + dbName + ".information_schema.columns " +
                "WHERE table_name = ? ORDER BY ordinal_position";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, tableName);
            ResultSet rs = stmt.executeQuery();

            List<Map<String, Object>> columns = new ArrayList<Map<String, Object>>();
            while (rs.next()) {
                Map<String, Object> column = new HashMap<String, Object>();
                column.put("name", rs.getString("column_name"));
                column.put("type", rs.getString("data_type"));
                column.put("length", rs.getObject("character_maximum_length"));
                column.put("nullable", rs.getString("is_nullable"));
                columns.add(column);
            }
            return columns;
        }
    }

    /**
     * Get current connection info
     */
    public Map<String, Object> getConnectionInfo() throws SQLException {
        Map<String, Object> info = new HashMap<String, Object>();

        if (connection == null || connection.isClosed()) {
            info.put("connected", false);
            return info;
        }

        info.put("connected", true);
        info.put("server", currentServer);
        info.put("database", currentDatabase);

        return info;
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Close connection
     */
    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            connection = null;
            currentServer = null;
            currentDatabase = null;
        }
    }

    /**
     * Ensure we have an active connection
     */
    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("No active connection. Please connect to a database first.");
        }
    }

    /**
     * Test query (SELECT 1)
     */
    public boolean testConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                return false;
            }
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT 1");
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Get stored procedures for a database
     */
    public List<String> getStoredProcedures(String dbName) throws SQLException {
        ensureConnection();

        String query = "SELECT ROUTINE_NAME FROM " + dbName + ".INFORMATION_SCHEMA.ROUTINES " +
                "WHERE ROUTINE_TYPE = 'PROCEDURE' AND ROUTINE_SCHEMA = 'dbo' " +
                "ORDER BY ROUTINE_NAME";

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            List<String> procedures = new ArrayList<String>();
            while (rs.next()) {
                procedures.add(rs.getString(1));
            }
            return procedures;
        }
    }

    /**
     * Get functions for a database
     */
    public List<String> getFunctions(String dbName) throws SQLException {
        ensureConnection();

        String query = "SELECT ROUTINE_NAME FROM " + dbName + ".INFORMATION_SCHEMA.ROUTINES " +
                "WHERE ROUTINE_TYPE = 'FUNCTION' AND ROUTINE_SCHEMA = 'dbo' " +
                "ORDER BY ROUTINE_NAME";

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            List<String> functions = new ArrayList<String>();
            while (rs.next()) {
                functions.add(rs.getString(1));
            }
            return functions;
        }
    }
    // ADD TO DatabaseService.java

    /**
     * Get CREATE TABLE statement for a table
     */
    public String getTableDefinition(String dbName, String tableName) throws SQLException {
        ensureConnection();

        StringBuilder definition = new StringBuilder();
        definition.append("CREATE TABLE [").append(dbName).append("].[dbo].[").append(tableName).append("] (\n");

        // Get columns
        String query = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, " +
                "IS_NULLABLE, COLUMN_DEFAULT " +
                "FROM " + dbName + ".INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = ? " +
                "ORDER BY ORDINAL_POSITION";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, tableName);
            ResultSet rs = stmt.executeQuery();

            List<String> columnDefs = new ArrayList<String>();

            while (rs.next()) {
                StringBuilder colDef = new StringBuilder();
                colDef.append("    [").append(rs.getString("COLUMN_NAME")).append("] ");

                String dataType = rs.getString("DATA_TYPE").toUpperCase();
                colDef.append(dataType);

                // Add length for varchar, nvarchar, char, nchar
                if (dataType.contains("CHAR") || dataType.contains("BINARY")) {
                    Object maxLength = rs.getObject("CHARACTER_MAXIMUM_LENGTH");
                    if (maxLength != null) {
                        int length = ((Number) maxLength).intValue();
                        colDef.append("(").append(length == -1 ? "MAX" : String.valueOf(length)).append(")");
                    }
                }

                // Nullable
                String nullable = rs.getString("IS_NULLABLE");
                if ("NO".equals(nullable)) {
                    colDef.append(" NOT NULL");
                } else {
                    colDef.append(" NULL");
                }

                // Default
                String defaultValue = rs.getString("COLUMN_DEFAULT");
                if (defaultValue != null && !defaultValue.trim().isEmpty()) {
                    colDef.append(" DEFAULT ").append(defaultValue);
                }

                columnDefs.add(colDef.toString());
            }

            definition.append(String.join(",\n", columnDefs));
            definition.append("\n);");
        }

        return definition.toString();
    }

    /**
     * Get CREATE/ALTER statement for a stored procedure
     */
    public String getSPDefinition(String dbName, String spName) throws SQLException {
        ensureConnection();

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("USE [" + dbName + "]");
        }

        String query = "SELECT m.definition " +
                "FROM sys.sql_modules m " +
                "JOIN sys.objects o ON m.object_id = o.object_id " +
                "WHERE o.type = 'P' AND o.name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, spName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("definition");
                }
            }
        }

        return "-- Stored procedure not found";
    }

}