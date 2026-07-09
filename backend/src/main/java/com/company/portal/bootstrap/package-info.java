/**
 * Application bootstrap tasks: creating the first administrator when a portal
 * is provisioned into an empty database, seeding runtime data that is not part
 * of Flyway. Depends on the shared platform module.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Bootstrap"
)
package com.company.portal.bootstrap;
