package com.kidora.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the users role check constraint includes SUB_ADMIN after enum expansion.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserRoleConstraintFixer {

    private final JdbcTemplate jdbc;
    private static final String CONSTRAINT_NAME = "users_role_check";

    @PostConstruct
    public void fixConstraintIfNeeded() {
        try {
            // Fetch current constraint definition (portable across Postgres versions)
            String definition = jdbc.query(
                "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c JOIN pg_class t ON c.conrelid = t.oid WHERE c.conname = ? AND t.relname = 'users'",
                ps -> ps.setString(1, CONSTRAINT_NAME),
                rs -> rs.next() ? rs.getString(1) : null
            );

            if (definition == null) {
                log.warn("users_role_check constraint not found. Attempting to create a fresh one including SUB_ADMIN.");
                createConstraint();
                return;
            }

            log.info("Current {} definition: {}", CONSTRAINT_NAME, definition);
            // Normalize for easier search
            String normalized = definition != null ? definition.toUpperCase() : "";
            if (normalized.contains("SUB_ADMIN")) {
                log.info("Constraint already includes SUB_ADMIN – no change needed.");
                return;
            }

            log.info("Updating constraint to include SUB_ADMIN...");
            dropConstraintQuietly();
            // Additionally drop any other legacy constraints on users.role that might still exist
            dropLegacyRoleConstraints();
            createConstraint();
        } catch (Exception e) {
            log.error("Failed to adjust users role constraint", e);
        }
    }

    private void dropConstraintQuietly() {
        try {
            jdbc.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS " + CONSTRAINT_NAME);
        } catch (Exception e) {
            log.warn("Unable to drop existing constraint (may not exist): {}", e.getMessage());
        }
    }

    private void createConstraint() {
        try {
            jdbc.execute("ALTER TABLE users ADD CONSTRAINT " + CONSTRAINT_NAME + " CHECK (role IN ('USER','SUB_ADMIN','ADMIN'))");
            log.info("Created/Updated {} constraint with SUB_ADMIN successfully.", CONSTRAINT_NAME);
        } catch (Exception e) {
            log.error("Failed to create users role constraint", e);
        }
    }

    private void dropLegacyRoleConstraints() {
        try {
            var legacy = jdbc.query(
                "SELECT c.conname, pg_get_constraintdef(c.oid) AS def FROM pg_constraint c JOIN pg_class t ON c.conrelid=t.oid WHERE t.relname='users' AND c.contype='c'",
                rs -> {
                    java.util.List<String> list = new java.util.ArrayList<>();
                    while (rs.next()) {
                        String name = rs.getString(1);
                        String def = rs.getString(2);
                        String norm = def.toUpperCase();
                        if (norm.contains("ROLE") && norm.contains("USER") && norm.contains("ADMIN") && !norm.contains("SUB_ADMIN") && !name.equals(CONSTRAINT_NAME)) {
                            list.add(name);
                        }
                    }
                    return list;
                }
            );
            if(legacy != null) {
                for (String name : legacy) {
                try {
                    log.info("Dropping legacy role constraint {} lacking SUB_ADMIN", name);
                    jdbc.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS " + name);
                } catch (Exception inner) {
                    log.warn("Failed to drop legacy constraint {}: {}", name, inner.getMessage());
                }
                }
            }
        } catch (Exception e) {
            log.warn("Error while scanning for legacy role constraints: {}", e.getMessage());
        }
    }
}
