/**
 * Application bootstrap tasks: creating the first administrator when a portal
 * is provisioned into an empty database, seeding runtime data that is not part
 * of Flyway. Depends on the shared platform and audit modules.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Bootstrap",
        allowedDependencies = {
                "shared :: config",
                "shared :: logging",
                "shared :: security",
                "audit :: api"
        }
)
package com.company.portal.bootstrap;
