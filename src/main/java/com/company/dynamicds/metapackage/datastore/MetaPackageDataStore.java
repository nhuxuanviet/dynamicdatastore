package com.company.dynamicds.metapackage.datastore;

import com.company.dynamicds.metapackage.entity.MetaPackage;
import com.company.dynamicds.metapackage.service.MetaPackageExecutor;
import io.jmix.core.*;
import io.jmix.core.datastore.AbstractDataStore;
import io.jmix.core.entity.KeyValueEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * DataStore implementation for MetaPackage
 * Similar to DynamicDataStore - handles LoadContext with filtering, sorting, and pagination
 */
public class MetaPackageDataStore extends AbstractDataStore {

    private static final Logger log = LoggerFactory.getLogger(MetaPackageDataStore.class);

    private final MetaPackage metaPackage;
    private final MetaPackageExecutor executor;
    private String storeName;

    public MetaPackageDataStore(String storeName,
                                 MetaPackage metaPackage,
                                 MetaPackageExecutor executor) {
        this.storeName = storeName;
        this.metaPackage = metaPackage;
        this.executor = executor;

        log.info("MetaPackageDataStore created for: {} (store: {})",
                metaPackage.getName(), storeName);
    }

    @Override
    public String getName() {
        return storeName;
    }

    @Override
    public void setName(String name) {
        this.storeName = name;
    }

    @Override
    protected Object loadOne(LoadContext<?> context) {
        List<Object> list = loadAll(context);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    protected List<Object> loadAll(LoadContext<?> context) {
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

        return new ArrayList<>(result);
    }

    @Override
    protected long countAll(LoadContext<?> context) {
        log.debug("countAll called for MetaPackage: {}", metaPackage.getName());

        if (context.getQuery() == null) {
            return 0;
        }

        // Load all and count (not optimized, but works)
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
    protected Set<Object> saveAll(SaveContext context) {
        throw new UnsupportedOperationException("MetaPackageDataStore is read-only");
    }

    @Override
    protected Set<Object> deleteAll(SaveContext context) {
        throw new UnsupportedOperationException("MetaPackageDataStore is read-only");
    }

    @Override
    protected List<Object> loadAllValues(ValueLoadContext context) {
        log.debug("loadAllValues called for MetaPackage: {}", metaPackage.getName());

        // Similar to loadAll but for KeyValueEntity
        List<KeyValueEntity> result = executor.executeLoad(
                metaPackage,
                context.getQuery() != null ? context.getQuery().getCondition() : null,
                context.getQuery() != null ? context.getQuery().getSort() : null,
                context.getQuery() != null ? context.getQuery().getFirstResult() : null,
                context.getQuery() != null ? context.getQuery().getMaxResults() : null
        );

        return new ArrayList<>(result);
    }

    @Override
    protected long countAllValues(ValueLoadContext context) {
        log.debug("countAllValues called for MetaPackage: {}", metaPackage.getName());

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

    @Override
    protected Object beginLoadTransaction(boolean joinTransaction) {
        return null; // Read-only, no transaction needed
    }

    @Override
    protected Object beginSaveTransaction(boolean joinTransaction) {
        return null; // Read-only, no transaction needed
    }

    @Override
    protected void commitTransaction(Object transaction) {
        // No-op
    }

    @Override
    protected void rollbackTransaction(Object transaction) {
        // No-op
    }

    @Override
    protected TransactionContextState getTransactionContextState(boolean isJoinTransaction) {
        return null; // Read-only, no transaction context
    }
}
