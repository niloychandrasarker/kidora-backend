package com.kidora.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the return_requests status check constraint allows the new COMPLETED value.
 * Hibernate (ddl-auto=update) does not update existing CHECK constraints when an enum expands,
 * so we patch it manually at startup if needed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReturnRequestStatusConstraintFixer {

    private final JdbcTemplate jdbc;

    private static final String CONSTRAINT_NAME = "return_requests_status_check";

    @PostConstruct
    public void fixConstraintIfNeeded() {
        try {
            String definition = jdbc.query(
                    "SELECT cc.check_clause FROM information_schema.check_constraints cc " +
                            "WHERE cc.constraint_name = ?",
                    ps -> ps.setString(1, CONSTRAINT_NAME),
                    rs -> rs.next() ? rs.getString(1) : null
            );
            if (definition == null) {
                log.warn("Return request status check constraint not found; skipping fix.");
                return;
            }
            if (definition.contains("COMPLETED")) {
                // Already updated
                return;
            }
            log.info("Updating {} to include COMPLETED. Old definition: {}", CONSTRAINT_NAME, definition);
            // Drop and recreate with new allowed values
            jdbc.execute("ALTER TABLE return_requests DROP CONSTRAINT " + CONSTRAINT_NAME);
            jdbc.execute("ALTER TABLE return_requests ADD CONSTRAINT " + CONSTRAINT_NAME + " CHECK (status IN ('PENDING','APPROVED','REJECTED','COMPLETED'))");
            log.info("Constraint {} updated successfully.", CONSTRAINT_NAME);
        } catch (Exception e) {
            log.error("Failed to adjust return request status constraint", e);
        }
    }
}
