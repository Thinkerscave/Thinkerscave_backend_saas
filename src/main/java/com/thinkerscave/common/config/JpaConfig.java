package com.thinkerscave.common.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.thinkerscave.common")
@EntityScan(basePackages = "com.thinkerscave.common")
public class JpaConfig {
}
