package com.company.dynamicds.metapackage.config;

import com.company.dynamicds.metapackage.service.MetaPackageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initializes active MetaPackages on application startup
 * Registers all active MetaPackage stores with Jmix framework
 */
@Component
public class MetaPackageStartupListener {

    private static final Logger log = LoggerFactory.getLogger(MetaPackageStartupListener.class);

    private final MetaPackageService metaPackageService;

    public MetaPackageStartupListener(MetaPackageService metaPackageService) {
        this.metaPackageService = metaPackageService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("═══════════════════════════════════════════════════════");
        log.info("🚀 Initializing MetaPackage System...");
        log.info("═══════════════════════════════════════════════════════");

        try {
            metaPackageService.initializeActiveMetaPackages();
            log.info("✓ MetaPackage system initialized successfully");
        } catch (Exception e) {
            log.error("❌ Failed to initialize MetaPackage system", e);
        }
    }
}
