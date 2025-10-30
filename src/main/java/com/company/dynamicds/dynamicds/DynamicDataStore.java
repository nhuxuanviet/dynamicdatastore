package com.company.dynamicds.dynamicds;

import io.jmix.core.LoadContext;
import io.jmix.core.SaveContext;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.datastore.AbstractDataStore;
import io.jmix.core.entity.KeyValueEntity;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class DynamicDataStore extends AbstractDataStore {
    private final DynamicKeyValueRestInvoker dynamicKeyValueRestInvoker;
    private String storeName;

    @Override
    protected Object loadOne(LoadContext<?> context) {
        return null;
    }

    @Override
    protected List<Object> loadAll(LoadContext<?> context) {
        String entityName = context.getEntityMetaClass().getName();
        List<KeyValueEntity> entities = dynamicKeyValueRestInvoker.loadList(storeName, entityName);
        return new ArrayList<>(entities);
    }

    @Override
    protected long countAll(LoadContext<?> context) {
        return 0;
    }

    @Override
    protected Set<Object> saveAll(SaveContext context) {
        return Set.of();
    }

    @Override
    protected Set<Object> deleteAll(SaveContext context) {
        return Set.of();
    }

    @Override
    protected List<Object> loadAllValues(ValueLoadContext context) {
        return List.of();
    }

    @Override
    protected long countAllValues(ValueLoadContext context) {
        return 0;
    }

    @Override
    protected Object beginLoadTransaction(boolean joinTransaction) {
        return null;
    }

    @Override
    protected Object beginSaveTransaction(boolean joinTransaction) {
        return null;
    }

    @Override
    protected void commitTransaction(Object transaction) {

    }

    @Override
    protected void rollbackTransaction(Object transaction) {

    }

    @Override
    protected TransactionContextState getTransactionContextState(boolean isJoinTransaction) {
        return null;
    }

    @Override
    public String getName() {
        return storeName;
    }

    @Override
    public void setName(String name) {
        this.storeName = name;
    }
}
