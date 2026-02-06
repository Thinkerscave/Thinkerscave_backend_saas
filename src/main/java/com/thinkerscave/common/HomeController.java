package com.thinkerscave.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/home")
public class HomeController {

	@GetMapping("/test")
	public String getMessage() {

		log.info("Accessing Home Test Endpoint");
		return "test";
	}
}
