package com.thinkerscave.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/home")
@Tag(name = "Home", description = "Public Access Controller for basic API health and reachability tests")
public class HomeController {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HomeController.class);

	@GetMapping("/test")
	@Operation(summary = "Simple health check endpoint")
	public String getMessage() {

		log.info("Accessing Home Test Endpoint");
		return "test";
	}
}
