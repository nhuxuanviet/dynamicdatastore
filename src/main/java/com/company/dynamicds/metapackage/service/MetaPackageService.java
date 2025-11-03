package com.company.dynamicds.metapackage.service;

import com.company.dynamicds.metapackage.datastore.MetaPackageDataStoreRegister;
import com.company.dynamicds.metapackage.entity.MetaPackage;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanRepository;
import io.jmix.core.FetchPlans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic service for MetaPackage management
 * Handles activation/deactivation and registration with Jmix
 */
@Service
public class MetaPackageService {

    private static final Logger log = LoggerFactory.getLogger(MetaPackageService.class);

    private final DataManager dataManager;
    private final MetaPackageDataStoreRegister storeRegister;
    private final FetchPlanRepository fetchPlanRepository;
    private final FetchPlans fetchPlans;

    public MetaPackageService(DataManager dataManager,
                              MetaPackageDataStoreRegister storeRegister,
                              FetchPlanRepository fetchPlanRepository, FetchPlans fetchPlans) {
        this.dataManager = dataManager;
        this.storeRegister = storeRegister;
        this.fetchPlanRepository = fetchPlanRepository;
        this.fetchPlans = fetchPlans;
    }

    /**
     * Activate MetaPackage - register as dynamic data store
     */
    @Transactional
    public void activate(MetaPackage metaPackage) {
        log.info("Activating MetaPackage: {}", metaPackage.getName());

        // Load with full fetch plan
        MetaPackage fullPackage = loadFullMetaPackage(metaPackage.getId());

        // Validate
        validateMetaPackage(fullPackage);

        // Check if already registered
        if (storeRegister.isRegistered(fullPackage.getStoreName())) {
            throw new IllegalStateException("Store '" + fullPackage.getStoreName() + "' is already registered");
        }

        // Register with Jmix
        storeRegister.registerMetaPackage(fullPackage);

        // Update isActive flag
        fullPackage.setIsActive(true);
        dataManager.save(fullPackage);

        log.info("✓ MetaPackage '{}' activated successfully", fullPackage.getName());
    }

    /**
     * Deactivate MetaPackage - unregister from Jmix
     */
    @Transactional
    public void deactivate(MetaPackage metaPackage) {
        log.info("Deactivating MetaPackage: {}", metaPackage.getName());

        MetaPackage fullPackage = loadFullMetaPackage(metaPackage.getId());

        // Unregister from Jmix
        storeRegister.unregisterMetaPackage(fullPackage.getStoreName());

        // Update isActive flag
        fullPackage.setIsActive(false);
        dataManager.save(fullPackage);

        log.info("✓ MetaPackage '{}' deactivated successfully", fullPackage.getName());
    }

    /**
     * Reload/refresh MetaPackage registration
     * Useful when configuration changes
     */
    @Transactional
    public void reload(MetaPackage metaPackage) {
        log.info("Reloading MetaPackage: {}", metaPackage.getName());

        MetaPackage fullPackage = loadFullMetaPackage(metaPackage.getId());

        if (fullPackage.getIsActive()) {
            // Unregister and re-register
            storeRegister.unregisterMetaPackage(fullPackage.getStoreName());
            storeRegister.registerMetaPackage(fullPackage);

            log.info("✓ MetaPackage '{}' reloaded successfully", fullPackage.getName());
        } else {
            log.warn("MetaPackage '{}' is not active, cannot reload", fullPackage.getName());
        }
    }

    /**
     * Initialize all active MetaPackages on application startup
     */
    @Transactional
    public void initializeActiveMetaPackages() {
        log.info("Initializing active MetaPackages...");

        List<MetaPackage> activePackages = dataManager.load(MetaPackage.class)
                .query("select e from MetaPackage e where e.isActive = true")
                .fetchPlan(createFullFetchPlan())
                .list();

        for (MetaPackage metaPackage : activePackages) {
            try {
                log.info("Auto-registering MetaPackage: {}", metaPackage.getName());
                storeRegister.registerMetaPackage(metaPackage);
            } catch (Exception e) {
                log.error("Failed to register MetaPackage '{}' on startup", metaPackage.getName(), e);
                // Continue with other packages
            }
        }

        log.info("✓ Initialized {} active MetaPackages", activePackages.size());
    }

    /**
     * Validate MetaPackage configuration
     */
    private void validateMetaPackage(MetaPackage metaPackage) {
        if (metaPackage.getName() == null || metaPackage.getName().isBlank()) {
            throw new IllegalArgumentException("MetaPackage name is required");
        }

        if (metaPackage.getStoreName() == null || metaPackage.getStoreName().isBlank()) {
            throw new IllegalArgumentException("Store name is required");
        }

        if (metaPackage.getMergeStrategy() == null) {
            throw new IllegalArgumentException("Merge strategy is required");
        }

        if (metaPackage.getSources() == null || metaPackage.getSources().isEmpty()) {
            throw new IllegalArgumentException("MetaPackage must have at least one source");
        }

        // Validate each source has field mappings
        for (var source : metaPackage.getSources()) {
            if (source.getFieldMappings() == null || source.getFieldMappings().isEmpty()) {
                throw new IllegalArgumentException(
                        "Source '" + source.getAlias() + "' must have at least one field mapping");
            }

            if (source.getMetadataDefinition() == null) {
                throw new IllegalArgumentException(
                        "Source '" + source.getAlias() + "' must reference a MetadataDefinition");
            }
        }
    }

    /**
     * Load MetaPackage with full graph (sources, mappings, metadata definitions)
     */
    private MetaPackage loadFullMetaPackage(UUID id) {
        return dataManager.load(MetaPackage.class)
                .id(id)
                .fetchPlan(createFullFetchPlan())
                .one();
    }

    /**
     * Create full fetch plan for MetaPackage
     */
    private FetchPlan createFullFetchPlan() {
        return fetchPlans.builder(MetaPackage.class)
                .addFetchPlan(FetchPlan.BASE)               // _base của MetaPackage
                .add("sources", b -> b
                        .addFetchPlan(FetchPlan.BASE)       // _base của MetaPackageSource
                        .add("metadataDefinition", b2 -> b2
                                .addFetchPlan(FetchPlan.BASE)   // _base của MetadataDefinition
                                .add("metadataFields", FetchPlan.BASE)) // _base của MetadataField
                        .add("fieldMappings", FetchPlan.BASE)) // _base của FieldMapping
                .build();
    }
}
