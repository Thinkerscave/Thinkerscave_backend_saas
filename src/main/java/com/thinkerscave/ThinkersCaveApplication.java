package com.thinkerscave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

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
        "com.thinkerscave.platform",
        "com.thinkerscave.security",
        "com.thinkerscave.shared",
        "com.thinkerscave.staff",
        "com.thinkerscave.student"
})
@EnableAsync
public class ThinkersCaveApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThinkersCaveApplication.class, args);
    }
}
