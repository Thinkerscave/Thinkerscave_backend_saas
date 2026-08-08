package com.thinkerscave.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

/**
 * Utility to reset test user passwords.
 * Run: mvn test -Dtest=PasswordResetUtil
 */
public class PasswordResetUtil {
    
    public static void main(String[] args) {
        // Set timezone to avoid Asia/Calcutta error
        System.setProperty("user.timezone", "UTC");
        
        String dbUrl = "jdbc:postgresql://72.61.244.175:5435/thinkerscave_db?TimeZone=UTC";
        String dbUser = "thinkerscave";
        String dbPassword = "cP7_min30Xt17DpFYuE8";
        String newPassword = "Password@123";
        
        // Match the strength used in SecurityConfig (12)
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hashedPassword = encoder.encode(newPassword);
        
        System.out.println("Generated BCrypt hash for 'Password@123':");
        System.out.println(hashedPassword);
        System.out.println();
        
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            System.out.println("Connected to database successfully!");
            
            // Query current state
            System.out.println("\nCurrent state of test users:");
            var selectStmt = conn.prepareStatement(
                "SELECT * FROM public.users " +
                "WHERE email IN ('admin.20260724113915@example.com', 'copilot.20260724113915@example.com') " +
                "ORDER BY email LIMIT 1"
            );
            var rs = selectStmt.executeQuery();
            if (rs.next()) {
                var metaData = rs.getMetaData();
                System.out.println("\nAvailable columns:");
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    System.out.printf("  %d. %s (%s)%n", i, metaData.getColumnName(i), metaData.getColumnTypeName(i));
                }
                
                // Show first user details
                System.out.println("\nFirst user:");
                System.out.printf("  ID: %d%n", rs.getLong("id"));
                System.out.printf("  Username: %s%n", rs.getString("username"));
                System.out.printf("  Email: %s%n", rs.getString("email"));
                System.out.printf("  Organization ID: %d%n", rs.getLong("organization_id"));
                System.out.printf("  First Time Login: %b%n", rs.getBoolean("first_time_login"));
            }
            rs.close();
            selectStmt.close();
            
            // Debug: Check organization for id=7
            System.out.println("\nDEBUG: Checking organization:");
            String orgSql = "SELECT * FROM public.organizations WHERE id = 7";
            PreparedStatement orgStmt = conn.prepareStatement(orgSql);
            ResultSet orgRs = orgStmt.executeQuery();
            ResultSetMetaData orgMeta = orgRs.getMetaData();
            System.out.println("Organizations table columns:");
            for (int i = 1; i <= orgMeta.getColumnCount(); i++) {
                System.out.println("  " + i + ". " + orgMeta.getColumnName(i));
            }
            if (orgRs.next()) {
                System.out.println("\nOrganization ID 7:");
                System.out.println("  ID: " + orgRs.getLong("id"));
                System.out.println("  Name: " + orgRs.getString("organization_name"));
                System.out.println("  Code: " + orgRs.getString("organization_code"));
            }
            orgRs.close();
            orgStmt.close();
            
            // Debug: Check user and roles
            System.out.println("\nDEBUG: Checking user and roles:");
            String userSql = "SELECT id, username, email, password, status FROM public.users WHERE email = 'admin.20260724113915@example.com'";
            PreparedStatement userStmt = conn.prepareStatement(userSql);
            ResultSet userRs = userStmt.executeQuery();
            if (userRs.next()) {
                long userId = userRs.getLong("id");
                System.out.println("\n--- ADMIN USER (ID: " + userId + ") ---");
                System.out.println("Username: '" + userRs.getString("username") + "'");
                System.out.println("Email: '" + userRs.getString("email") + "'");
                System.out.println("Status: " + userRs.getString("status"));
                System.out.println("Password matches: " + encoder.matches(newPassword, userRs.getString("password")));
                
                // Check roles
                String rolesSql = "SELECT ur.active, r.role_name, r.role_type FROM public.user_roles ur JOIN public.roles r ON ur.role_id = r.id WHERE ur.user_id = ?";
                PreparedStatement rolesStmt = conn.prepareStatement(rolesSql);
                rolesStmt.setLong(1, userId);
                ResultSet rolesRs = rolesStmt.executeQuery();
                System.out.println("\n--- ROLES ---");
                int roleCount = 0;
                while (rolesRs.next()) {
                    roleCount++;
                    System.out.println("  Role: " + rolesRs.getString("role_name") + 
                                     " (Type: " + rolesRs.getString("role_type") + 
                                     ", Active: " + rolesRs.getBoolean("active") + ")");
                }
                if (roleCount == 0) {
                    System.out.println("  *** NO ROLES FOUND - THIS IS THE PROBLEM! ***");
                }
                rolesRs.close();
                rolesStmt.close();
            }
            userRs.close();
            userStmt.close();
            var checkStmt = conn.prepareStatement(
                "SELECT id, username, email, password, first_time_login, organization_id " +
                "FROM public.users " +
                "WHERE email IN ('admin.20260724113915@example.com', 'copilot.20260724113915@example.com') " +
                "ORDER BY email"
            );
            var checkRs = checkStmt.executeQuery();
            while (checkRs.next()) {
                System.out.printf("ID: %d%n", checkRs.getLong("id"));
                System.out.printf("  Username: %s%n", checkRs.getString("username"));
                System.out.printf("  Email: %s%n", checkRs.getString("email"));
                System.out.printf("  Password hash: %s%n", checkRs.getString("password"));
                System.out.printf("  First Time Login: %b%n", checkRs.getBoolean("first_time_login"));
                System.out.printf("  Organization ID: %d%n%n", checkRs.getLong("organization_id"));
                
                // Verify if the password matches
                BCryptPasswordEncoder verifier = new BCryptPasswordEncoder();
                boolean matches = verifier.matches(newPassword, checkRs.getString("password"));
                System.out.printf("  Password '%s' matches: %b%n%n", newPassword, matches);
            }
            checkRs.close();
            checkStmt.close();
            
            // Verify updates
            System.out.println("\nVerifying updates:");
            var verifyStmt = conn.prepareStatement(
                "SELECT id, username, email, organization_id, first_time_login, is_active, " +
                "(password = ?) as password_matches " +
                "FROM public.users " +
                "WHERE email IN ('admin.20260724113915@example.com', 'copilot.20260724113915@example.com') " +
                "ORDER BY email"
            );
            verifyStmt.setString(1, hashedPassword);
            var verifyRs = verifyStmt.executeQuery();
            while (verifyRs.next()) {
                System.out.printf("ID: %d, Email: %s, OrgId: %d, FirstTimeLogin: %b, Active: %b, PasswordSet: %b%n",
                    verifyRs.getLong("id"),
                    verifyRs.getString("email"),
                    verifyRs.getLong("organization_id"),
                    verifyRs.getBoolean("first_time_login"),
                    verifyRs.getBoolean("is_active"),
                    verifyRs.getBoolean("password_matches")
                );
            }
            verifyRs.close();
            verifyStmt.close();
            
            System.out.println("\n✓ Password reset completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
