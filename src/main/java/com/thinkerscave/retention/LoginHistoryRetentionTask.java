package com.thinkerscave.retention;

import org.springframework.stereotype.Component;

@Component
public class LoginHistoryRetentionTask implements RetentionTask {

    public static final String KEY = "LOGIN_HISTORY";
    public static final int RETENTION_DAYS = 30;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String label() {
        return "Login history";
    }

    @Override
    public String description() {
        return "Deletes sign-in records older than the configured retention window (default 30 days). "
                + "The window can be changed centrally under app.retention.tasks.LOGIN_HISTORY.";
    }

    @Override
    public int retentionDays() {
        return RETENTION_DAYS;
    }

    @Override
    public String table() {
        return "login_history";
    }

    @Override
    public String dateColumn() {
        return "login_time";
    }

    @Override
    public boolean organizationScoped() {
        return true;
    }

    @Override
    public String organizationClause() {
        return "user_id IN (SELECT id FROM users WHERE organization_id = ?)";
    }
}