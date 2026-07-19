package com.company.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;

/**
 * Portal entry point. Modules discover each other via Spring Modulith package
 * conventions; {@link com.company.portal.shared} hosts platform primitives and
 * {@link com.company.portal.bootstrap} performs first-boot provisioning.
 */
@SpringBootApplication
@ConfigurationPropertiesScan("com.company.portal")
@Modulithic(
        systemName = "Portal",
        sharedModules = { "shared" }
)
public class PortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortalApplication.class, args);
    }
}
