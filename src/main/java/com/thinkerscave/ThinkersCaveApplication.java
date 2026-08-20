package com.thinkerscave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * ThinkersCave SaaS Platform — main application entry point.
 *
 * Component scan explicitly covers all modular packages under
 * {@code com.thinkerscave} EXCEPT the decommissioned {@code common} sub-tree.
 * JPA entity and repository scanning is handled by
 * {@link com.thinkerscave.config.AppJpaConfig}.
 */
@SpringBootApplication(scanBasePackages = {
        "com.thinkerscave.access",
        "com.thinkerscave.academics",
        "com.thinkerscave.admission",
        "com.thinkerscave.attendance",
        "com.thinkerscave.audit",
        "com.thinkerscave.communication",
        "com.thinkerscave.config",
        "com.thinkerscave.dashboard",
        "com.thinkerscave.document",
        "com.thinkerscave.onboarding",
        "com.thinkerscave.platform",
        "com.thinkerscave.security",
        "com.thinkerscave.shared",
        "com.thinkerscave.staff",
        "com.thinkerscave.student"
})
@EnableAsync
@EnableScheduling
public class ThinkersCaveApplication {

    public static void main(String[] args) {
        // Must run before any JDBC connect. PG JDBC sends JVM default as TimeZone startup
        // param; remote PG rejects Windows "Asia/Calcutta". IDE runs often omit -Duser.timezone=UTC.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");
        SpringApplication.run(ThinkersCaveApplication.class, args);
    }
}
