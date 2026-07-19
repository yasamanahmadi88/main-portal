/**
 * Shared platform code (configuration, error handling, filters, crypto,
 * security primitives). Feature modules depend on this module; it must not
 * depend on any feature module.
 *
 * <p>All sub-packages ({@code config}, {@code error}, {@code web},
 * {@code logging}, {@code crypto}, {@code security}) are declared as
 * {@link org.springframework.modulith.NamedInterface named interfaces} so
 * feature modules can import them without violating Spring Modulith's
 * "internal by default" rule.</p>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Shared Platform"
)
package com.company.portal.shared;
