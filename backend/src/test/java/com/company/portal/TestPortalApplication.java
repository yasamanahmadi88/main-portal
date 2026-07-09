package com.company.portal;

import org.springframework.boot.SpringApplication;

public class TestPortalApplication {

	public static void main(String[] args) {
		SpringApplication.from(PortalApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
