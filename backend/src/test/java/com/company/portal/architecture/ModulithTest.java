package com.company.portal.architecture;

import com.company.portal.PortalApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifies Spring Modulith module boundaries. This will pick up
 * {@code package-info.java} declarations in {@code com.company.portal.shared}
 * and {@code com.company.portal.bootstrap}; additional feature modules add
 * their own {@code package-info.java}.
 */
class ModulithTest {

    @Test
    void verifiesModulithStructure() {
        ApplicationModules modules = ApplicationModules.of(PortalApplication.class);
        modules.verify();
    }
}
