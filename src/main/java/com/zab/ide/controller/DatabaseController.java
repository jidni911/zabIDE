package com.zab.ide.controller;

import com.zab.ide.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/database")
public class DatabaseController {

    @Autowired
    private DatabaseService databaseService;

    /**
     * Connect to Database
     * POST /api/database/connect
     */
    @PostMapping("/connect")
    public ResponseEntity<?> connectToDatabase(
            @RequestParam String serverName,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String dbName) {
        try {
            databaseService.connectToDatabase(serverName, username, password, dbName);
            
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("message", "Connected to " + dbName + " at " + serverName);
            response.put("server", serverName);
            response.put("database", dbName);
            
            return ResponseEntity.ok(response);
        } catch (SQLException | ClassNotFoundException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to connect: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Execute SQL Query
     * POST /api/database/execute
     */
    @PostMapping("/execute")
    public ResponseEntity<?> executeQuery(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            if (query == null || query.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<String, Object>();
                error.put("success", false);
                error.put("message", "Query cannot be empty");
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, Object> result = databaseService.executeQuery(query);
            result.put("success", true);
            
            return ResponseEntity.ok(result);
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Error executing query: " + e.getMessage());
            error.put("error", e.getSQLState());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get Connection Info
     * GET /api/database/connection-info
     */
    @GetMapping("/connection-info")
    public ResponseEntity<?> getConnectionInfo() {
        try {
            Map<String, Object> info = databaseService.getConnectionInfo();
            return ResponseEntity.ok(info);
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("connected", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get List of Databases
     * GET /api/database/databases
     */
    @GetMapping("/databases")
    public ResponseEntity<?> getDatabases() {
        try {
            List<String> databases = databaseService.getDatabases();
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("databases", databases);
            response.put("count", databases.size());
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to retrieve databases: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get Tables in Database
     * GET /api/database/tables?dbName=mydb
     */
    @GetMapping("/tables")
    public ResponseEntity<?> getTables(@RequestParam String dbName) {
        try {
            List<String> tables = databaseService.getTables(dbName);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("tables", tables);
            response.put("database", dbName);
            response.put("count", tables.size());
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to retrieve tables: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get Table Columns
     * GET /api/database/columns?dbName=mydb&tableName=users
     */
    @GetMapping("/columns")
    public ResponseEntity<?> getTableColumns(
            @RequestParam String dbName,
            @RequestParam String tableName) {
        try {
            List<Map<String, Object>> columns = databaseService.getTableColumns(dbName, tableName);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("columns", columns);
            response.put("database", dbName);
            response.put("table", tableName);
            response.put("count", columns.size());
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to retrieve columns: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Close Database Connection
     * POST /api/database/close
     */
    @PostMapping("/close")
    public ResponseEntity<?> closeConnection() {
        try {
            databaseService.closeConnection();
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("message", "Connection closed");
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Error closing connection: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Test Connection
     * GET /api/database/test
     */
    @GetMapping("/test")
    public ResponseEntity<?> testConnection() {
        boolean connected = databaseService.testConnection();
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("connected", connected);
        return ResponseEntity.ok(response);
    }

    // Add these endpoints to DatabaseController.java

    /**
     * Get Stored Procedures in Database
     * GET /api/database/stored-procedures?dbName=mydb
     */
    @GetMapping("/stored-procedures")
    public ResponseEntity<?> getStoredProcedures(@RequestParam String dbName) {
        try {
            List<String> procedures = databaseService.getStoredProcedures(dbName);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("procedures", procedures);
            response.put("database", dbName);
            response.put("count", procedures.size());
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to retrieve stored procedures: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get Functions in Database
     * GET /api/database/functions?dbName=mydb
     */
    @GetMapping("/functions")
    public ResponseEntity<?> getFunctions(@RequestParam String dbName) {
        try {
            List<String> functions = databaseService.getFunctions(dbName);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("functions", functions);
            response.put("database", dbName);
            response.put("count", functions.size());
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to retrieve functions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ADD TO DatabaseController.java

    /**
     * Get Table Definition (CREATE TABLE statement)
     * GET /api/database/table-definition?dbName=mydb&tableName=users
     */
    @GetMapping("/table-definition")
    public ResponseEntity<?> getTableDefinition(
            @RequestParam String dbName,
            @RequestParam String tableName) {
        try {
            String definition = databaseService.getTableDefinition(dbName, tableName);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("definition", definition);
            response.put("database", dbName);
            response.put("table", tableName);
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to get table definition: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get Stored Procedure Definition (CREATE/ALTER statement)
     * GET /api/database/sp-definition?dbName=mydb&spName=GetUsers
     */
    @GetMapping("/sp-definition")
    public ResponseEntity<?> getSPDefinition(
            @RequestParam String dbName,
            @RequestParam String spName) {
        try {
            String definition = databaseService.getSPDefinition(dbName, spName);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("definition", definition);
            response.put("database", dbName);
            response.put("procedure", spName);
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to get SP definition: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}