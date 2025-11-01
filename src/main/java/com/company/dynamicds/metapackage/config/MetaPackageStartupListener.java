package com.company.dynamicds.metapackage.config;

import com.company.dynamicds.metapackage.service.MetaPackageService;
import io.jmix.core.security.SystemAuthenticator;            // <-- import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MetaPackageStartupListener {

    private static final Logger log = LoggerFactory.getLogger(MetaPackageStartupListener.class);

    private final MetaPackageService metaPackageService;
    private final SystemAuthenticator systemAuthenticator;   // <-- inject

    public MetaPackageStartupListener(MetaPackageService metaPackageService,
                                      SystemAuthenticator systemAuthenticator) {
        this.metaPackageService = metaPackageService;
        this.systemAuthenticator = systemAuthenticator;      // <-- gán
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("═══════════════════════════════════════════════════════");
        log.info("🚀 Initializing MetaPackage System...");
        log.info("═══════════════════════════════════════════════════════");

        try {
            systemAuthenticator.withSystem(() -> {           // <-- QUẤN BẰNG withSystem
                metaPackageService.initializeActiveMetaPackages();
                return null;
            });
            log.info("✓ MetaPackage system initialized successfully");
        } catch (Exception e) {
            log.error("❌ Failed to initialize MetaPackage system", e);
        }
    }
}
