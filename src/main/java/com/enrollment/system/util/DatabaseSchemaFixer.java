package com.enrollment.system.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * Standalone utility to scan and fix database schema mismatches.
 * This will inspect the actual database and fix any issues found.
 */
@Component
public class DatabaseSchemaFixer {
    
    @Autowired(required = false)
    private DataSource dataSource;
    
    /**
     * Force fixes the table by dropping and recreating it with correct schema.
     * Use this when you know the schema is wrong.
     */
    public void forceFixTeacherAssignmentsTable() {
        if (dataSource == null) {
            System.err.println("❌ DataSource not available");
            return;
        }
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("FORCE FIXING teacher_assignments TABLE");
        System.out.println("=".repeat(80));
        
        try (Connection conn = dataSource.getConnection()) {
            String dbUrl = conn.getMetaData().getURL();
            
            if (!dbUrl.contains("sqlite")) {
                System.out.println("⚠ Not a SQLite database");
                return;
            }
            
            System.out.println("🔧 Dropping and recreating table with correct schema...");
            fixTableSchema(conn, new HashMap<>());
            
            // Ensure sequence table exists
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS teacher_assignments_seq (" +
                    "next_val INTEGER NOT NULL DEFAULT 1" +
                    ")"
                );
                stmt.executeUpdate("INSERT OR IGNORE INTO teacher_assignments_seq (next_val) VALUES (1)");
                System.out.println("✅ Sequence table verified/created");
            } catch (Exception e) {
                System.err.println("⚠ Could not create sequence table: " + e.getMessage());
            }
            
            System.out.println("✅ Force fix completed!");
            System.out.println("=".repeat(80) + "\n");
            
        } catch (Exception e) {
            System.err.println("❌ Error force fixing schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Scans and fixes the teacher_assignments table schema
     */
    public void scanAndFixTeacherAssignmentsTable() {
        if (dataSource == null) {
            System.err.println("❌ DataSource not available");
            return;
        }
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SCANNING AND FIXING teacher_assignments TABLE SCHEMA");
        System.out.println("=".repeat(80));
        
        try (Connection conn = dataSource.getConnection()) {
            String dbUrl = conn.getMetaData().getURL();
            
            if (!dbUrl.contains("sqlite")) {
                System.out.println("⚠ Not a SQLite database");
                return;
            }
            
            // Step 1: Check if table exists
            DatabaseMetaData metaData = conn.getMetaData();
            boolean tableExists = false;
            try (ResultSet tables = metaData.getTables(null, null, "teacher_assignments", null)) {
                tableExists = tables.next();
            }
            
            if (!tableExists) {
                System.out.println("📋 Table does not exist - creating with correct schema...");
                createCorrectTable(conn);
                System.out.println("✅ Table created successfully");
                return;
            }
            
            // Step 2: Get actual columns
            System.out.println("\n📋 SCANNING ACTUAL TABLE SCHEMA:");
            Map<String, ColumnInfo> actualColumns = scanTableColumns(conn);
            
            System.out.println("  Found " + actualColumns.size() + " columns:");
            for (ColumnInfo col : actualColumns.values()) {
                System.out.println("    - " + col.name + " (" + col.type + ") " + 
                                 (col.notNull ? "NOT NULL" : "NULL") + 
                                 (col.primaryKey ? " PRIMARY KEY" : ""));
            }
            
            // Step 3: Get CREATE TABLE SQL
            String createTableSql = getCreateTableSql(conn);
            if (createTableSql != null) {
                System.out.println("\n📋 CREATE TABLE SQL:");
                System.out.println("  " + createTableSql);
            }
            
            // Step 4: Check for mismatches
            System.out.println("\n🔍 CHECKING FOR MISMATCHES:");
            List<String> issues = new ArrayList<>();
            
            // Check for WRONG columns that should NOT exist
            // These columns should NOT exist at all - they're wrong
            if (actualColumns.containsKey("subject")) {
                issues.add("❌ Column 'subject' exists but should NOT exist (use 'subject_id' instead)");
            }
            if (actualColumns.containsKey("section")) {
                issues.add("❌ Column 'section' exists but should NOT exist (use 'section_id' instead)");
            }
            if (actualColumns.containsKey("teacher")) {
                issues.add("❌ Column 'teacher' exists but should NOT exist (use 'teacher_id' instead)");
            }
            if (actualColumns.containsKey("grade_level")) {
                issues.add("❌ Column 'grade_level' exists but should NOT exist in teacher_assignments table");
            }
            
            // Check CREATE TABLE SQL for wrong columns
            if (createTableSql != null) {
                String upperSql = createTableSql.toUpperCase();
                if (upperSql.contains(" SUBJECT ") || upperSql.contains("\"SUBJECT\"")) {
                    if (!upperSql.contains(" SUBJECT_ID ") && !upperSql.contains("\"SUBJECT_ID\"")) {
                        issues.add("❌ CREATE TABLE SQL contains 'subject' but not 'subject_id'");
                    } else {
                        issues.add("❌ CREATE TABLE SQL contains BOTH 'subject' and 'subject_id' - 'subject' should be removed");
                    }
                }
                if (upperSql.contains(" GRADE_LEVEL ") || upperSql.contains("\"GRADE_LEVEL\"")) {
                    issues.add("❌ CREATE TABLE SQL contains 'grade_level' which should NOT exist");
                }
            }
            
            // Check for missing required columns
            String[] requiredColumns = {"id", "teacher_id", "subject_id", "section_id", "created_at", "updated_at"};
            for (String reqCol : requiredColumns) {
                if (!actualColumns.containsKey(reqCol)) {
                    issues.add("❌ Missing required column: " + reqCol);
                }
            }
            
            // Check if we have wrong columns that need fixing
            boolean hasWrongColumns = actualColumns.containsKey("subject") || 
                                     actualColumns.containsKey("section") || 
                                     actualColumns.containsKey("teacher") || 
                                     actualColumns.containsKey("grade_level");
            
            // Report issues
            if (issues.isEmpty() && !hasWrongColumns) {
                System.out.println("  ✅ No issues found - schema is correct!");
            } else {
                if (hasWrongColumns) {
                    System.out.println("  ⚠️  DETECTED WRONG COLUMNS - Will fix immediately!");
                }
                if (!issues.isEmpty()) {
                    System.out.println("  Found " + issues.size() + " issue(s):");
                    for (String issue : issues) {
                        System.out.println("    " + issue);
                    }
                }
                
                // ALWAYS fix if there are wrong columns or issues - drop and recreate
                System.out.println("\n🔧 FIXING SCHEMA (dropping and recreating table)...");
                fixTableSchema(conn, actualColumns);
                
                // Verify fix worked
                System.out.println("\n🔍 VERIFYING FIX...");
                Map<String, ColumnInfo> newColumns = scanTableColumns(conn);
                boolean stillHasIssues = false;
                if (newColumns.containsKey("subject") || newColumns.containsKey("section") || 
                    newColumns.containsKey("teacher") || newColumns.containsKey("grade_level")) {
                    System.err.println("  ❌ Fix failed - wrong columns still exist!");
                    stillHasIssues = true;
                }
                if (!newColumns.containsKey("subject_id") || !newColumns.containsKey("section_id") || 
                    !newColumns.containsKey("teacher_id")) {
                    System.err.println("  ❌ Fix failed - required columns missing!");
                    stillHasIssues = true;
                }
                if (!stillHasIssues) {
                    System.out.println("  ✅ Schema fix verified - all issues resolved!");
                    System.out.println("  Final columns: " + String.join(", ", newColumns.keySet()));
                }
            }
            
            System.out.println("=".repeat(80) + "\n");
            
        } catch (Exception e) {
            System.err.println("❌ Error scanning/fixing schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Map<String, ColumnInfo> scanTableColumns(Connection conn) throws Exception {
        Map<String, ColumnInfo> columns = new LinkedHashMap<>();
        DatabaseMetaData metaData = conn.getMetaData();
        
        try (ResultSet rs = metaData.getColumns(null, null, "teacher_assignments", null)) {
            while (rs.next()) {
                String colName = rs.getString("COLUMN_NAME");
                String typeName = rs.getString("TYPE_NAME");
                int nullable = rs.getInt("NULLABLE");
                boolean isPrimaryKey = false;
                
                // Check if primary key
                try (ResultSet pkRs = metaData.getPrimaryKeys(null, null, "teacher_assignments")) {
                    while (pkRs.next()) {
                        if (pkRs.getString("COLUMN_NAME").equalsIgnoreCase(colName)) {
                            isPrimaryKey = true;
                            break;
                        }
                    }
                }
                
                boolean notNull = (nullable == DatabaseMetaData.columnNoNulls);
                columns.put(colName.toLowerCase(), new ColumnInfo(colName, typeName, notNull, isPrimaryKey));
            }
        }
        
        return columns;
    }
    
    private String getCreateTableSql(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT sql FROM sqlite_master WHERE type='table' AND name='teacher_assignments'")) {
            if (rs.next()) {
                return rs.getString("sql");
            }
        }
        return null;
    }
    
    private void fixTableSchema(Connection conn, Map<String, ColumnInfo> actualColumns) throws Exception {
        conn.setAutoCommit(false);
        
        try (Statement stmt = conn.createStatement()) {
            // Check data count
            int dataCount = 0;
            try (ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM teacher_assignments")) {
                if (countRs.next()) {
                    dataCount = countRs.getInt("cnt");
                }
            } catch (Exception e) {
                // Ignore
            }
            
            if (dataCount > 0) {
                System.out.println("  ⚠ Table has " + dataCount + " record(s) - data will be lost");
            }
            
            // Drop table (but keep sequence - we'll recreate it)
            System.out.println("  → Dropping existing table...");
            stmt.executeUpdate("DROP TABLE IF EXISTS teacher_assignments");
            // Don't drop sequence - we'll recreate it if needed
            
            // Create with correct schema
            System.out.println("  → Creating table with CORRECT schema...");
            createCorrectTable(conn);
            
            conn.commit();
            System.out.println("  ✅ Schema fixed successfully!");
            
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
    
    private void createCorrectTable(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            String createSql = 
                "CREATE TABLE teacher_assignments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "teacher_id INTEGER NOT NULL, " +
                "subject_id INTEGER NOT NULL, " +
                "section_id INTEGER NOT NULL, " +
                "created_at TIMESTAMP NOT NULL, " +
                "updated_at TIMESTAMP, " +
                "FOREIGN KEY (teacher_id) REFERENCES users(id), " +
                "FOREIGN KEY (subject_id) REFERENCES subjects(id), " +
                "FOREIGN KEY (section_id) REFERENCES sections(id), " +
                "UNIQUE(teacher_id, subject_id, section_id)" +
                ")";
            
            stmt.executeUpdate(createSql);
            
            // Create sequence table for Hibernate ID generation
            // Hibernate uses this when GenerationType.AUTO is used
            try {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS teacher_assignments_seq (" +
                    "next_val INTEGER NOT NULL DEFAULT 1" +
                    ")"
                );
                // Initialize sequence if table was just created
                stmt.executeUpdate("INSERT OR IGNORE INTO teacher_assignments_seq (next_val) VALUES (1)");
                System.out.println("  → Created teacher_assignments_seq table for ID generation");
            } catch (Exception e) {
                System.out.println("  ⚠ Could not create sequence table (may already exist): " + e.getMessage());
            }
            
            // Create indexes
            try {
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_teacher_assignments_teacher_id ON teacher_assignments(teacher_id)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_teacher_assignments_subject_id ON teacher_assignments(subject_id)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_teacher_assignments_section_id ON teacher_assignments(section_id)");
            } catch (Exception e) {
                // Ignore index errors
            }
        }
    }
    
    private static class ColumnInfo {
        String name;
        String type;
        boolean notNull;
        boolean primaryKey;
        
        ColumnInfo(String name, String type, boolean notNull, boolean primaryKey) {
            this.name = name;
            this.type = type;
            this.notNull = notNull;
            this.primaryKey = primaryKey;
        }
    }
}

