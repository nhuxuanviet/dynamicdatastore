package com.company.dynamicds.dynamicds;

import com.company.dynamicds.dynamicds.entity.DynamicDataStoreConfig;
import io.jmix.core.Stores;
import io.jmix.core.impl.StoreDescriptorsRegistry;
import io.jmix.core.metamodel.model.Store;
import io.jmix.core.metamodel.model.StoreDescriptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class DynamicDataStoreRegister {

    private final ConfigurableListableBeanFactory beanFactory;
    private final ApplicationContext applicationContext;
    private final StoreDescriptorsRegistry storeDescriptorsRegistry;
    private final Stores stores;

    private final Map<String, DynamicDataStore> registeredStores = new ConcurrentHashMap<>();

    public synchronized void registerDataStore(DynamicDataStoreConfig config) {
        String storeName = config.getStoreName();

        if (registeredStores.containsKey(storeName)) {
            log.warn("DataStore '{}' is already registered. Skipping.", storeName);
            return;
        }

        // 1. Tạo DynamicDataStore bean
        DynamicDataStore store = beanFactory.createBean(DynamicDataStore.class);
        store.setName(storeName);
        String storeBeanName = "dynamicDataStore_" + storeName;

        // 2. Đăng ký bean với Spring container
        beanFactory.registerSingleton(storeBeanName, store);
        beanFactory.initializeBean(store, storeBeanName);

        // 3. Tạo StoreDescriptor runtime
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

        // Không được ghi vào getAdditionalDataStoreNames() vì list đó là immutable!
        // => Chỉ chèn descriptor vào map nội bộ
        getDescriptorMap().put(storeName, descriptor);

        // 4. Đăng ký Store với Jmix
        Store jmixStore = applicationContext.getBean(Store.class, storeName, descriptor);
        getStoresMap().put(storeName, jmixStore);

        // 5. Lưu lại store để quản lý bên trong app
        registeredStores.put(storeName, store);

        log.info(" Registered DynamicDataStore '{}' (beanName={}, descriptor={})",
                storeName, storeBeanName, descriptor.getClass().getSimpleName());
    }

    public DynamicDataStore getStore(String storeName) {
        return registeredStores.get(storeName);
    }

    public boolean isRegistered(String storeName) {
        return registeredStores.containsKey(storeName);
    }

    public Map<String, DynamicDataStore> getAllStores() {
        return registeredStores;
    }

    // ===== Utility methods =====

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
}
