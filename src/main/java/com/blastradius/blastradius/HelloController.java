package com.blastradius.blastradius;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	@GetMapping("/health")
	public String health() {
		return "Blast Radius backend is alive";
	}

}
