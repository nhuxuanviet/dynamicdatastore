package com.company.dynamicds.metapackage.datastore;

import com.company.dynamicds.metapackage.entity.MetaPackage;
import com.company.dynamicds.metapackage.service.MetaPackageExecutor;
import io.jmix.core.Metadata;
import io.jmix.core.Stores;
import io.jmix.core.impl.MetadataImpl;
import io.jmix.core.impl.StoreDescriptorsRegistry;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.Store;
import io.jmix.core.metamodel.model.StoreDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers MetaPackageDataStore dynamically with Jmix framework
 * Similar to DynamicDataStoreRegister
 */
@Component
@Slf4j
public class MetaPackageDataStoreRegister {

    private final ConfigurableListableBeanFactory beanFactory;
    private final ApplicationContext applicationContext;
    private final StoreDescriptorsRegistry storeDescriptorsRegistry;
    private final Stores stores;
    private final Metadata metadata;
    private final MetaPackageExecutor executor;
    private final MetaPackageMetaClassFactory metaClassFactory;

    private final Map<String, MetaPackageDataStore> registeredStores = new ConcurrentHashMap<>();

    public MetaPackageDataStoreRegister(ConfigurableListableBeanFactory beanFactory,
                                         ApplicationContext applicationContext,
                                         StoreDescriptorsRegistry storeDescriptorsRegistry,
                                         Stores stores,
                                         Metadata metadata,
                                         MetaPackageExecutor executor,
                                         MetaPackageMetaClassFactory metaClassFactory) {
        this.beanFactory = beanFactory;
        this.applicationContext = applicationContext;
        this.storeDescriptorsRegistry = storeDescriptorsRegistry;
        this.stores = stores;
        this.metadata = metadata;
        this.executor = executor;
        this.metaClassFactory = metaClassFactory;
    }

    /**
     * Register MetaPackage as a dynamic data store
     */
    public synchronized void registerMetaPackage(MetaPackage metaPackage) {
        String storeName = metaPackage.getStoreName();

        if (registeredStores.containsKey(storeName)) {
            log.warn("MetaPackage store '{}' is already registered. Skipping.", storeName);
            return;
        }

        log.info("Registering MetaPackage: {} (store: {})", metaPackage.getName(), storeName);

        try {
            // 1. Create MetaPackageDataStore instance
            MetaPackageDataStore dataStore = new MetaPackageDataStore(
                    storeName,
                    metaPackage,
                    executor
            );

            String storeBeanName = "metaPackageDataStore_" + storeName;

            // 2. Register as Spring bean
            beanFactory.registerSingleton(storeBeanName, dataStore);
            beanFactory.initializeBean(dataStore, storeBeanName);

            // 3. Create StoreDescriptor
            StoreDescriptor descriptor = new StoreDescriptor() {
                @Override
                public String getBeanName() {
                    return storeBeanName;
                }

                @Override
                public boolean isJpa() {
                    return false;
                }
            };

            // 4. Register descriptor with Jmix
            getDescriptorMap().put(storeName, descriptor);

            // 5. Create and register Store FIRST
            Store jmixStore = applicationContext.getBean(Store.class, storeName, descriptor);
            getStoresMap().put(storeName, jmixStore);

            // 6. NOW create dynamic MetaClass (after Store exists)
            MetaClass metaClass = metaClassFactory.createMetaClass(metaPackage, storeName);

            // 7. Set store for MetaClass (required for proper operation)
            if (metaClass instanceof com.company.dynamicds.dynamicds.DynamicMetaClass dynamicMetaClass) {
                dynamicMetaClass.setStore(jmixStore);
            }

            // 8. Register MetaClass with metadata
            registerMetaClass(metaClass, storeName);

            // 9. Save to internal registry
            registeredStores.put(storeName, dataStore);

            log.info("✓ Successfully registered MetaPackage store: {}", storeName);

        } catch (Exception e) {
            log.error("Failed to register MetaPackage: {}", storeName, e);
            throw new RuntimeException("Failed to register MetaPackage: " + storeName, e);
        }
    }

    /**
     * Unregister MetaPackage store
     */
    public synchronized void unregisterMetaPackage(String storeName) {
        if (!registeredStores.containsKey(storeName)) {
            log.warn("MetaPackage store '{}' is not registered. Skipping.", storeName);
            return;
        }

        log.info("Unregistering MetaPackage store: {}", storeName);

        try {
            // 1. Remove from internal registry
            registeredStores.remove(storeName);

            // 2. Remove from Jmix stores
            getStoresMap().remove(storeName);

            // 3. Remove descriptor
            getDescriptorMap().remove(storeName);

            // 4. Unregister MetaClass from metadata
            unregisterMetaClass(storeName);

            // 5. Note: Spring bean cleanup not needed for singleton registry
            // ConfigurableListableBeanFactory doesn't provide destroySingleton in Jmix 2.6

            log.info("✓ Successfully unregistered MetaPackage store: {}", storeName);

        } catch (Exception e) {
            log.error("Failed to unregister MetaPackage: {}", storeName, e);
        }
    }

    /**
     * Check if MetaPackage is registered
     */
    public boolean isRegistered(String storeName) {
        return registeredStores.containsKey(storeName);
    }

    /**
     * Get registered MetaPackageDataStore
     */
    public MetaPackageDataStore getStore(String storeName) {
        return registeredStores.get(storeName);
    }

    /**
     * Get all registered stores
     */
    public Map<String, MetaPackageDataStore> getAllStores() {
        return registeredStores;
    }

    // ===== Internal utility methods =====

    @SuppressWarnings("unchecked")
    private Map<String, StoreDescriptor> getDescriptorMap() {
        try {
            var field = StoreDescriptorsRegistry.class.getDeclaredField("descriptors");
            field.setAccessible(true);
            return (Map<String, StoreDescriptor>) field.get(storeDescriptorsRegistry);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to access StoreDescriptorsRegistry.descriptors", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Store> getStoresMap() {
        try {
            var field = Stores.class.getDeclaredField("stores");
            field.setAccessible(true);
            return (Map<String, Store>) field.get(stores);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to access Stores.stores", e);
        }
    }

    /**
     * Register MetaClass with Jmix metadata
     */
    @SuppressWarnings("unchecked")
    private void registerMetaClass(MetaClass metaClass, String storeName) {
        try {
            if (!(metadata instanceof MetadataImpl metadataImpl)) {
                throw new IllegalStateException("Metadata is not instance of MetadataImpl");
            }

            // Access internal session field
            var sessionField = MetadataImpl.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            Object session = sessionField.get(metadataImpl);

            // Access metaClasses map
            var metaClassesField = session.getClass().getDeclaredField("metaClasses");
            metaClassesField.setAccessible(true);
            Map<String, MetaClass> metaClasses = (Map<String, MetaClass>) metaClassesField.get(session);

            // Register MetaClass
            String entityName = metaClass.getName();
            metaClasses.put(entityName, metaClass);

            log.debug("Registered MetaClass: {} in store: {}", entityName, storeName);

        } catch (Exception e) {
            log.error("Failed to register MetaClass: {}", metaClass.getName(), e);
            throw new RuntimeException("Failed to register MetaClass", e);
        }
    }

    /**
     * Unregister MetaClass from metadata
     */
    @SuppressWarnings("unchecked")
    private void unregisterMetaClass(String entityName) {
        try {
            if (!(metadata instanceof MetadataImpl metadataImpl)) {
                return;
            }

            var sessionField = MetadataImpl.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            Object session = sessionField.get(metadataImpl);

            var metaClassesField = session.getClass().getDeclaredField("metaClasses");
            metaClassesField.setAccessible(true);
            Map<String, MetaClass> metaClasses = (Map<String, MetaClass>) metaClassesField.get(session);

            metaClasses.remove(entityName);

            log.debug("Unregistered MetaClass: {}", entityName);

        } catch (Exception e) {
            log.error("Failed to unregister MetaClass: {}", entityName, e);
        }
    }
}
