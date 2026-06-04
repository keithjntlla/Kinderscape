package com.enrollment.system.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Utility to update database schema when new enum values are added.
 * This is particularly useful for SQLite which doesn't automatically update CHECK constraints.
 */
@Component
public class DatabaseSchemaUpdater {
    
    @Autowired(required = false)
    private DataSource dataSource;
    
    /**
     * Updates the users table to allow TEACHER role.
     * This method drops and recreates the role column's CHECK constraint to include TEACHER.
     */
    public void updateUsersTableForTeacherRole() {
        if (dataSource == null) {
            System.out.println("⚠ DataSource not available, skipping schema update");
            return;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            String dbUrl = conn.getMetaData().getURL();
            
            // Only proceed if it's SQLite
            if (!dbUrl.contains("sqlite")) {
                System.out.println("⚠ Not a SQLite database, skipping schema update");
                return;
            }
            
            // Check if TEACHER role constraint needs to be added
            if (needsTeacherRoleUpdate(conn)) {
                System.out.println("🔄 Updating users table schema to support TEACHER role...");
                updateRoleConstraint(conn);
                System.out.println("✅ Users table schema updated successfully");
            } else {
                System.out.println("✓ Users table already supports TEACHER role");
            }
            
        } catch (Exception e) {
            System.err.println("⚠ Error updating database schema: " + e.getMessage());
            e.printStackTrace();
            // Don't throw - allow application to continue
        }
    }
    
    private boolean needsTeacherRoleUpdate(Connection conn) throws Exception {
        // Try to insert a test value to see if TEACHER is allowed
        // This is a simple check - if the constraint exists and doesn't include TEACHER, we need to update
        try (Statement stmt = conn.createStatement()) {
            // Check if we can query the table structure
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "users", "role");
            
            if (columns.next()) {
                // Column exists, now check constraint
                // SQLite stores CHECK constraints in sqlite_master
                ResultSet tables = stmt.executeQuery(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='users'"
                );
                
                if (tables.next()) {
                    String createTableSql = tables.getString("sql");
                    if (createTableSql != null) {
                        // Check if TEACHER is in the CHECK constraint
                        boolean hasTeacher = createTableSql.toUpperCase().contains("TEACHER");
                        boolean hasCheckConstraint = createTableSql.toUpperCase().contains("CHECK");
                        
                        // If there's a CHECK constraint but TEACHER is not in it, we need to update
                        return hasCheckConstraint && !hasTeacher;
                    }
                }
            }
        }
        return false;
    }
    
    private void updateRoleConstraint(Connection conn) throws Exception {
        // SQLite doesn't support ALTER TABLE to modify CHECK constraints directly
        // We need to recreate the table
        conn.setAutoCommit(false);
        
        try (Statement stmt = conn.createStatement()) {
            // Step 1: Create new table with updated constraint
            stmt.executeUpdate(
                "CREATE TABLE users_new (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "password VARCHAR(255) NOT NULL, " +
                "full_name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100), " +
                "role VARCHAR(20) NOT NULL CHECK(role IN ('ADMIN', 'REGISTRAR', 'STAFF', 'TEACHER')), " +
                "is_active BOOLEAN NOT NULL DEFAULT 1, " +
                "created_at TIMESTAMP NOT NULL, " +
                "updated_at TIMESTAMP, " +
                "last_login TIMESTAMP" +
                ")"
            );
            
            // Step 2: Copy data from old table
            stmt.executeUpdate(
                "INSERT INTO users_new (id, username, password, full_name, email, role, is_active, created_at, updated_at, last_login) " +
                "SELECT id, username, password, full_name, email, role, is_active, created_at, updated_at, last_login FROM users"
            );
            
            // Step 3: Drop old table
            stmt.executeUpdate("DROP TABLE users");
            
            // Step 4: Rename new table
            stmt.executeUpdate("ALTER TABLE users_new RENAME TO users");
            
            // Step 5: Recreate indexes
            try {
                stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(username)");
            } catch (Exception e) {
                // Index might already exist, ignore
            }
            
            conn.commit();
            System.out.println("✅ Successfully updated users table schema");
            
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
    
    /**
     * Verifies and ensures the teacher_assignments table exists with correct schema.
     * This method checks if the table exists and has all required columns with correct constraints.
     */
    public void ensureTeacherAssignmentsTableExists() {
        if (dataSource == null) {
            System.out.println("⚠ DataSource not available, skipping teacher_assignments table verification");
            return;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            String dbUrl = conn.getMetaData().getURL();
            
            // Only proceed if it's SQLite
            if (!dbUrl.contains("sqlite")) {
                System.out.println("⚠ Not a SQLite database, skipping teacher_assignments table verification");
                return;
            }
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("CHECKING teacher_assignments TABLE SCHEMA");
            System.out.println("=".repeat(80));
            
            // Check if table exists
            DatabaseMetaData metaData = conn.getMetaData();
            boolean tableExists = false;
            try (ResultSet tables = metaData.getTables(null, null, "teacher_assignments", null)) {
                tableExists = tables.next();
            }
            
            if (!tableExists) {
                // Table doesn't exist, create it
                System.out.println("🔄 Table does not exist - Creating teacher_assignments table...");
                createTeacherAssignmentsTable(conn);
                System.out.println("✅ teacher_assignments table created successfully");
            } else {
                // Table exists - INSPECT ACTUAL SCHEMA
                System.out.println("📋 Inspecting existing table schema...");
                java.util.Set<String> actualColumns = new java.util.HashSet<>();
                try (ResultSet columns = metaData.getColumns(null, null, "teacher_assignments", null)) {
                    System.out.println("  Actual columns found:");
                    while (columns.next()) {
                        String colName = columns.getString("COLUMN_NAME");
                        actualColumns.add(colName.toLowerCase());
                        System.out.println("    - " + colName);
                    }
                }
                
                // Check the actual CREATE TABLE SQL
                String createTableSql = null;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                         "SELECT sql FROM sqlite_master WHERE type='table' AND name='teacher_assignments'")) {
                    if (rs.next()) {
                        createTableSql = rs.getString("sql");
                        System.out.println("  CREATE TABLE SQL:");
                        System.out.println("    " + (createTableSql != null ? createTableSql : "null"));
                    }
                }
                
                // Check for wrong column names
                boolean hasWrongColumns = false;
                if (actualColumns.contains("subject") && !actualColumns.contains("subject_id")) {
                    System.out.println("  ❌ FOUND WRONG COLUMN: 'subject' exists but 'subject_id' is missing!");
                    hasWrongColumns = true;
                }
                if (actualColumns.contains("section") && !actualColumns.contains("section_id")) {
                    System.out.println("  ❌ FOUND WRONG COLUMN: 'section' exists but 'section_id' is missing!");
                    hasWrongColumns = true;
                }
                if (actualColumns.contains("teacher") && !actualColumns.contains("teacher_id")) {
                    System.out.println("  ❌ FOUND WRONG COLUMN: 'teacher' exists but 'teacher_id' is missing!");
                    hasWrongColumns = true;
                }
                
                // Also check CREATE TABLE SQL for wrong column names
                if (createTableSql != null) {
                    String upperSql = createTableSql.toUpperCase();
                    if (upperSql.contains(" SUBJECT ") && !upperSql.contains(" SUBJECT_ID ")) {
                        System.out.println("  ❌ CREATE TABLE SQL contains 'subject' but not 'subject_id'!");
                        hasWrongColumns = true;
                    }
                }
                
                // ALWAYS check CREATE TABLE SQL for wrong column names
                boolean sqlHasWrongColumns = false;
                if (createTableSql != null) {
                    String upperSql = createTableSql.toUpperCase();
                    // Check if SQL contains wrong column names
                    if ((upperSql.contains(" SUBJECT ") || upperSql.contains("\"SUBJECT\"")) && 
                        !upperSql.contains(" SUBJECT_ID ") && !upperSql.contains("\"SUBJECT_ID\"")) {
                        sqlHasWrongColumns = true;
                        System.out.println("  ❌ CREATE TABLE SQL contains wrong column name!");
                    }
                }
                
                if (hasWrongColumns || sqlHasWrongColumns || needsTeacherAssignmentsTableUpdate(conn)) {
                    System.out.println("\n🔄 SCHEMA MISMATCH DETECTED - Fixing table structure...");
                    updateTeacherAssignmentsTable(conn);
                    System.out.println("✅ teacher_assignments table schema fixed successfully");
                } else {
                    System.out.println("✓ teacher_assignments table schema is correct");
                }
            }
            
            System.out.println("=".repeat(80) + "\n");
            
        } catch (Exception e) {
            System.err.println("❌ Error verifying teacher_assignments table: " + e.getMessage());
            e.printStackTrace();
            // Try to force fix as last resort
            try {
                System.out.println("🔄 Attempting force fix of teacher_assignments table...");
                forceFixTeacherAssignmentsTable();
            } catch (Exception e2) {
                System.err.println("❌ Force fix also failed: " + e2.getMessage());
            }
        }
    }
    
    /**
     * Force fix - drops and recreates the table regardless of current state
     * Use this as a last resort when schema is corrupted
     */
    public void forceFixTeacherAssignmentsTable() {
        if (dataSource == null) {
            return;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            String dbUrl = conn.getMetaData().getURL();
            if (!dbUrl.contains("sqlite")) {
                return;
            }
            
            System.out.println("\n🔧 FORCE FIXING teacher_assignments table...");
            conn.setAutoCommit(false);
            
            try (Statement stmt = conn.createStatement()) {
                // Drop table (sequence will be recreated)
                stmt.executeUpdate("DROP TABLE IF EXISTS teacher_assignments");
                
                // Recreate with correct schema (includes sequence table)
                createTeacherAssignmentsTable(conn);
                
                conn.commit();
                System.out.println("✅ Force fix completed - table and sequence recreated with correct schema");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Force fix failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean needsTeacherAssignmentsTableUpdate(Connection conn) throws Exception {
        // Check if all required columns exist with correct constraints
        DatabaseMetaData metaData = conn.getMetaData();
        
        // Required columns: id, teacher_id, subject_id, section_id, created_at, updated_at
        String[] requiredColumns = {"id", "teacher_id", "subject_id", "section_id", "created_at", "updated_at"};
        java.util.Set<String> existingColumns = new java.util.HashSet<>();
        
        try (ResultSet columns = metaData.getColumns(null, null, "teacher_assignments", null)) {
            while (columns.next()) {
                existingColumns.add(columns.getString("COLUMN_NAME").toLowerCase());
            }
        }
        
        // Check for incorrect column names (e.g., "subject" instead of "subject_id")
        if (existingColumns.contains("subject") && !existingColumns.contains("subject_id")) {
            return true; // Wrong column name, needs update
        }
        if (existingColumns.contains("section") && !existingColumns.contains("section_id")) {
            return true; // Wrong column name, needs update
        }
        if (existingColumns.contains("teacher") && !existingColumns.contains("teacher_id")) {
            return true; // Wrong column name, needs update
        }
        
        // Check if all required columns exist
        for (String col : requiredColumns) {
            if (!existingColumns.contains(col.toLowerCase())) {
                return true; // Missing column, needs update
            }
        }
        
        return false;
    }
    
    private void createTeacherAssignmentsTable(Connection conn) throws Exception {
        conn.setAutoCommit(false);
        
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
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
                ")"
            );
            
            // Create sequence table for Hibernate ID generation
            try {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS teacher_assignments_seq (" +
                    "next_val INTEGER NOT NULL DEFAULT 1" +
                    ")"
                );
                stmt.executeUpdate("INSERT OR IGNORE INTO teacher_assignments_seq (next_val) VALUES (1)");
            } catch (Exception e) {
                // Sequence table might already exist, ignore
            }
            
            // Create indexes for better performance
            try {
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_teacher_assignments_teacher_id ON teacher_assignments(teacher_id)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_teacher_assignments_subject_id ON teacher_assignments(subject_id)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_teacher_assignments_section_id ON teacher_assignments(section_id)");
            } catch (Exception e) {
                // Indexes might already exist, ignore
            }
            
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
    
    private void updateTeacherAssignmentsTable(Connection conn) throws Exception {
        // Recreate the table with correct schema
        // SQLite doesn't support ALTER TABLE to rename columns, so we need to recreate
        conn.setAutoCommit(false);
        
        try (Statement stmt = conn.createStatement()) {
            // Check if table has data
            int dataCount = 0;
            try (ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM teacher_assignments")) {
                if (countRs.next()) {
                    dataCount = countRs.getInt("cnt");
                }
            } catch (Exception e) {
                // Table might not exist or error reading
            }
            
            if (dataCount > 0) {
                System.out.println("  ⚠ Table has " + dataCount + " records - data will be lost during schema fix");
                System.out.println("  ⚠ This is necessary to fix the schema mismatch (subject vs subject_id column).");
            }
            
            // Drop ALL related objects first
            System.out.println("  → Dropping existing table and related objects...");
            try {
                stmt.executeUpdate("DROP TABLE IF EXISTS teacher_assignments");
            } catch (Exception e) {
                System.out.println("  ⚠ Error dropping table (may not exist): " + e.getMessage());
            }
            
            // Drop sequence table if it exists (Hibernate might have created it)
            try {
                stmt.executeUpdate("DROP TABLE IF EXISTS teacher_assignments_seq");
            } catch (Exception e) {
                // Ignore - sequence might not exist
            }
            
            // Recreate with correct schema
            System.out.println("  → Creating table with correct schema (subject_id, section_id, teacher_id)...");
            createTeacherAssignmentsTable(conn);
            
            conn.commit();
            System.out.println("  ✅ Schema fixed successfully!");
            
        } catch (Exception e) {
            conn.rollback();
            System.err.println("  ❌ Error fixing schema: " + e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}

