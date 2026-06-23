package com.blastradius.blastradius;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.blastradius")
@EntityScan(basePackages = "com.blastradius.model")
@EnableJpaRepositories(basePackages = "com.blastradius.model")
public class BlastradiusApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlastradiusApplication.class, args);
	}

}
