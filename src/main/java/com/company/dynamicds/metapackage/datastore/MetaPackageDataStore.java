package com.company.dynamicds.metapackage.datastore;

import com.company.dynamicds.metapackage.entity.MetaPackage;
import com.company.dynamicds.metapackage.service.MetaPackageExecutor;
import io.jmix.core.*;
import io.jmix.core.datastore.AbstractDataStore;
import io.jmix.core.datastore.DataStoreAfterEntityLoadEvent;
import io.jmix.core.datastore.DataStoreBeforeEntityCountEvent;
import io.jmix.core.entity.KeyValueEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * DataStore implementation for MetaPackage
 * Similar to DynamicDataStore and RestDataStore
 * Handles LoadContext with filtering, sorting, and pagination
 */
public class MetaPackageDataStore extends AbstractDataStore {

    private static final Logger log = LoggerFactory.getLogger(MetaPackageDataStore.class);

    private final MetaPackage metaPackage;
    private final MetaPackageExecutor executor;
    private final ApplicationEventPublisher eventPublisher;

    public MetaPackageDataStore(String storeName,
                                 MetaPackage metaPackage,
                                 MetaPackageExecutor executor,
                                 Metadata metadata,
                                 MetadataTools metadataTools,
                                 ApplicationEventPublisher eventPublisher) {
        this.metaPackage = metaPackage;
        this.executor = executor;
        this.eventPublisher = eventPublisher;

        log.info("MetaPackageDataStore created for: {} (store: {})",
                metaPackage.getName(), storeName);
    }

    @Override
    public String getName() {
        return metaPackage.getStoreName();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> List<E> loadAll(LoadContext<E> context) {
        log.debug("loadAll called for MetaPackage: {}", metaPackage.getName());

        if (context.getQuery() == null) {
            log.warn("LoadContext has no query, returning empty list");
            return Collections.emptyList();
        }

        // Extract filtering, sorting, and pagination from LoadContext
        LoadContext.Query query = context.getQuery();

        // Execute meta package aggregation
        List<KeyValueEntity> result = executor.executeLoad(
                metaPackage,
                query.getCondition(),      // PropertyCondition/LogicalCondition
                query.getSort(),            // Sort orders
                query.getFirstResult(),     // Pagination offset
                query.getMaxResults()       // Pagination limit
        );

        // Publish after load event (for Jmix framework integration)
        if (!result.isEmpty()) {
            eventPublisher.publishEvent(new DataStoreAfterEntityLoadEvent(this, result));
        }

        return (List<E>) result;
    }

    @Override
    public <E> long getCount(LoadContext<E> context) {
        log.debug("getCount called for MetaPackage: {}", metaPackage.getName());

        // Publish before count event
        eventPublisher.publishEvent(new DataStoreBeforeEntityCountEvent(this, context));

        if (context.getQuery() == null) {
            return 0;
        }

        // Load all and count (not optimized, but works for now)
        LoadContext.Query query = context.getQuery();

        List<KeyValueEntity> result = executor.executeLoad(
                metaPackage,
                query.getCondition(),
                null,  // No sorting needed for count
                null,  // No pagination
                null
        );

        return result.size();
    }

    @Override
    public <E> E save(E entity, SaveContext context) {
        throw new UnsupportedOperationException("MetaPackageDataStore is read-only");
    }

    @Override
    public void save(SaveContext context) {
        throw new UnsupportedOperationException("MetaPackageDataStore is read-only");
    }

    @Override
    public Set<Object> save(Set<Object> entities) {
        throw new UnsupportedOperationException("MetaPackageDataStore is read-only");
    }

    @Override
    public <E> E load(LoadContext<E> context) {
        List<E> list = loadAll(context);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<KeyValueEntity> loadValues(ValueLoadContext context) {
        log.debug("loadValues called for MetaPackage: {}", metaPackage.getName());

        // Similar to loadAll but for KeyValueEntity
        List<KeyValueEntity> result = executor.executeLoad(
                metaPackage,
                context.getQuery() != null ? context.getQuery().getCondition() : null,
                context.getQuery() != null ? context.getQuery().getSort() : null,
                context.getQuery() != null ? context.getQuery().getFirstResult() : null,
                context.getQuery() != null ? context.getQuery().getMaxResults() : null
        );

        if (!result.isEmpty()) {
            eventPublisher.publishEvent(new DataStoreAfterEntityLoadEvent(this, result));
        }

        return result;
    }

    @Override
    public long getCount(ValueLoadContext context) {
        log.debug("getCount (values) called for MetaPackage: {}", metaPackage.getName());

        if (context.getQuery() == null) {
            return 0;
        }

        List<KeyValueEntity> result = executor.executeLoad(
                metaPackage,
                context.getQuery().getCondition(),
                null,
                null,
                null
        );

        return result.size();
    }
}
