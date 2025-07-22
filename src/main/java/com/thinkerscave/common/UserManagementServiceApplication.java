package com.thinkerscave.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan({"com.thinkerscave.common.student.domain","com.thinkerscave.common.usrm.domain","com.thinkerscave.common.commonModel","com.thinkerscave.common.role.domain"})
@EnableJpaRepositories({"com.thinkerscave.common.student.repository","com.thinkerscave.common.usrm.repository","com.thinkerscave.common.commonModel","com.thinkerscave.common.role.repository"})
@ComponentScan({"com.thinkerscave.common.student","com.thinkerscave.common.usrm"})
public class UserManagementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserManagementServiceApplication.class, args);
	}

}
