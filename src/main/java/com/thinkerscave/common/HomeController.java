package com.thinkerscave.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/home")
public class HomeController {

	@GetMapping("/test")
	public String getMessage() {
		
		System.out.println("Hello Message");
		return "test";
	}
}
