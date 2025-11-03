package com.company.dynamicds.metapackage.service;

import com.company.dynamicds.dynamicds.DynamicKeyValueRestInvoker;
import com.company.dynamicds.metapackage.entity.MetaPackage;
import com.company.dynamicds.metapackage.entity.MetaPackageFieldMapping;
import com.company.dynamicds.metapackage.entity.MetaPackageRelationship;
import com.company.dynamicds.metapackage.entity.MetaPackageSource;
import com.company.dynamicds.metapackage.enums.MergeStrategy;
import com.company.dynamicds.metapackage.enums.RelationshipType;
import io.jmix.core.*;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.querycondition.Condition;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Executes meta package data aggregation from multiple metadata sources
 * Supports parallel/async fetching for improved performance
 */
@Component
public class MetaPackageExecutor {

    private static final Logger log = LoggerFactory.getLogger(MetaPackageExecutor.class);

    private final DynamicKeyValueRestInvoker restInvoker;
    private final MetaPackageFilterProcessor filterProcessor;
    private final DataManager dataManager;
    private final FetchPlanRepository fetchPlanRepository;
    private final FetchPlans fetchPlans;

    // Thread pool for async fetching
    private final ExecutorService executorService;

    public MetaPackageExecutor(DynamicKeyValueRestInvoker restInvoker,
                               MetaPackageFilterProcessor filterProcessor,
                               DataManager dataManager,
                               FetchPlanRepository fetchPlanRepository, FetchPlans fetchPlans) {
        this.restInvoker = restInvoker;
        this.filterProcessor = filterProcessor;
        this.dataManager = dataManager;
        this.fetchPlanRepository = fetchPlanRepository;
        this.fetchPlans = fetchPlans;

        // Create thread pool with size based on available processors
        int threadPoolSize = Math.max(4, Runtime.getRuntime().availableProcessors());
        this.executorService = Executors.newFixedThreadPool(
            threadPoolSize,
            r -> {
                Thread t = new Thread(r);
                t.setName("MetaPackage-Fetcher-" + t.getId());
                t.setDaemon(true);
                return t;
            }
        );
        log.info("MetaPackageExecutor initialized with thread pool size: {}", threadPoolSize);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down MetaPackageExecutor thread pool...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Execute meta package data loading with filtering, sorting, and pagination
     * Fetches from multiple sources in parallel for better performance
     */
    public List<KeyValueEntity> executeLoad(MetaPackage metaPackage,
                                             Condition condition,
                                             Sort sort,
                                             Integer firstResult,
                                             Integer maxResults) {
        long startTime = System.currentTimeMillis();
        log.info("Executing meta package: {} with merge strategy: {}",
                metaPackage.getName(), metaPackage.getMergeStrategy());

        // Load full MetaPackage with sources and mappings
        MetaPackage fullPackage = loadFullMetaPackage(metaPackage.getId());

        if (fullPackage.getSources() == null || fullPackage.getSources().isEmpty()) {
            log.warn("MetaPackage {} has no sources", fullPackage.getName());
            return Collections.emptyList();
        }

        // Sort sources by orderIndex
        List<MetaPackageSource> sortedSources = fullPackage.getSources().stream()
                .sorted(Comparator.comparing(MetaPackageSource::getOrderIndex))
                .collect(Collectors.toList());

        // Fetch data from all sources ASYNC (parallel)
        Map<String, List<KeyValueEntity>> sourceDataMap = fetchAllSourcesAsync(sortedSources);

        // Merge data - use relationships if defined, otherwise use legacy merge strategy
        List<KeyValueEntity> mergedData;
        if (fullPackage.getRelationships() != null && !fullPackage.getRelationships().isEmpty()) {
            // NEW: Relationship-based merge (SQL JOIN-like)
            mergedData = mergeWithRelationships(sourceDataMap, sortedSources, fullPackage.getRelationships());
        } else {
            // LEGACY: Strategy-based merge (for backward compatibility)
            mergedData = mergeData(sourceDataMap, sortedSources, fullPackage.getMergeStrategy());
        }

        // Apply filtering (PropertyCondition/LogicalCondition)
        if (condition != null) {
            mergedData = filterProcessor.applyFilter(mergedData, condition);
        }

        // Apply sorting
        if (sort != null && !sort.getOrders().isEmpty()) {
            mergedData = applySorting(mergedData, sort);
        }

        // Apply pagination
        if (firstResult != null || maxResults != null) {
            mergedData = applyPagination(mergedData, firstResult, maxResults);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Meta package {} returned {} records in {}ms",
                fullPackage.getName(), mergedData.size(), duration);
        return mergedData;
    }

    /**
     * Load MetaPackage with all sources, mappings, and metadata definitions
     */
    /** Load MetaPackage với đầy đủ graph (sources, mappings, relationships, metadata definitions) */
    private MetaPackage loadFullMetaPackage(UUID metaPackageId) {
        // 1) Thử lấy fetch plan đặt tên nếu bạn đã khai báo trong XML: "metaPackage-full"
        FetchPlan fetchPlan = null;
        try {
            fetchPlan = fetchPlanRepository.getFetchPlan(MetaPackage.class, "metaPackage-full");
        } catch (Exception ignored) {
            // không sao, sẽ fallback qua builder
        }

        // 2) Nếu chưa có, tự build bằng FetchPlans (Jmix 2.6)
        if (fetchPlan == null) {
            fetchPlan = fetchPlans.builder(MetaPackage.class)
                    .addFetchPlan(FetchPlan.BASE)
                    .add("sources", b -> b
                            .addFetchPlan(FetchPlan.BASE)
                            .add("metadataDefinition", b2 -> b2
                                    .addFetchPlan(FetchPlan.BASE)
                                    .add("metadataFields", FetchPlan.BASE))
                            .add("fieldMappings", FetchPlan.BASE))
                    .add("relationships", FetchPlan.BASE)
                    .build();
        }

        return dataManager.load(MetaPackage.class)
                .id(metaPackageId)
                .fetchPlan(fetchPlan)
                .one();
    }

    /**
     * Fetch data from all sources using CompletableFuture for parallel execution
     * IMPROVED PERFORMANCE: All API calls run concurrently
     */
    private Map<String, List<KeyValueEntity>> fetchAllSourcesAsync(List<MetaPackageSource> sources) {
        log.debug("Starting async fetch for {} sources", sources.size());

        // Create CompletableFuture for each source
        List<CompletableFuture<Map.Entry<String, List<KeyValueEntity>>>> futures = sources.stream()
                .map(source -> CompletableFuture.supplyAsync(() -> {
                    try {
                        log.debug("Fetching data from source: {} ({})",
                                source.getAlias(), source.getMetadataDefinition().getName());

                        long sourceStart = System.currentTimeMillis();
                        List<KeyValueEntity> data = restInvoker.loadList(source.getMetadataDefinition());
                        long sourceDuration = System.currentTimeMillis() - sourceStart;

                        log.debug("Source {} returned {} records in {}ms",
                                source.getAlias(), data.size(), sourceDuration);

                        return Map.entry(source.getAlias(), data);

                    } catch (Exception e) {
                        log.error("Failed to fetch data from source: {}", source.getAlias(), e);
                        return Map.entry(source.getAlias(), Collections.<KeyValueEntity>emptyList());
                    }
                }, executorService))
                .collect(Collectors.toList());

        // Wait for all futures to complete and collect results
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            // Wait with timeout (30 seconds)
            allFutures.get(30, TimeUnit.SECONDS);

            // Collect results in order
            Map<String, List<KeyValueEntity>> dataMap = new LinkedHashMap<>();
            for (CompletableFuture<Map.Entry<String, List<KeyValueEntity>>> future : futures) {
                Map.Entry<String, List<KeyValueEntity>> entry = future.get();
                dataMap.put(entry.getKey(), entry.getValue());
            }

            return dataMap;

        } catch (TimeoutException e) {
            log.error("Timeout waiting for source data fetching", e);
            // Return partial results
            Map<String, List<KeyValueEntity>> dataMap = new LinkedHashMap<>();
            for (CompletableFuture<Map.Entry<String, List<KeyValueEntity>>> future : futures) {
                if (future.isDone() && !future.isCompletedExceptionally()) {
                    try {
                        Map.Entry<String, List<KeyValueEntity>> entry = future.get();
                        dataMap.put(entry.getKey(), entry.getValue());
                    } catch (Exception ignored) {
                    }
                }
            }
            return dataMap;

        } catch (Exception e) {
            log.error("Error during async fetching", e);
            // Fallback to empty map
            return new LinkedHashMap<>();
        }
    }

    /**
     * Merge data from multiple sources according to merge strategy
     */
    private List<KeyValueEntity> mergeData(Map<String, List<KeyValueEntity>> sourceDataMap,
                                            List<MetaPackageSource> sources,
                                            MergeStrategy strategy) {
        return switch (strategy) {
            case SEQUENTIAL -> mergeSequential(sourceDataMap, sources);
            case CROSS_JOIN -> mergeCrossJoin(sourceDataMap, sources);
            case NESTED -> mergeNested(sourceDataMap, sources);
        };
    }

    /**
     * SEQUENTIAL: Append all records from all sources
     */
    private List<KeyValueEntity> mergeSequential(Map<String, List<KeyValueEntity>> sourceDataMap,
                                                  List<MetaPackageSource> sources) {
        List<KeyValueEntity> result = new ArrayList<>();

        for (MetaPackageSource source : sources) {
            List<KeyValueEntity> sourceData = sourceDataMap.get(source.getAlias());
            if (sourceData == null || sourceData.isEmpty()) {
                continue;
            }

            for (KeyValueEntity entity : sourceData) {
                KeyValueEntity mappedEntity = dataManager.create(KeyValueEntity.class);
                applyFieldMappings(entity, mappedEntity, source);
                result.add(mappedEntity);
            }
        }

        return result;
    }

    /**
     * CROSS_JOIN: Cartesian product of all sources
     */
    private List<KeyValueEntity> mergeCrossJoin(Map<String, List<KeyValueEntity>> sourceDataMap,
                                                 List<MetaPackageSource> sources) {
        List<KeyValueEntity> result = new ArrayList<>();

        // Start with first source
        if (sources.isEmpty()) {
            return result;
        }

        List<KeyValueEntity> firstSourceData = sourceDataMap.get(sources.get(0).getAlias());
        if (firstSourceData == null || firstSourceData.isEmpty()) {
            return result;
        }

        // Initialize with first source
        for (KeyValueEntity entity : firstSourceData) {
            KeyValueEntity mappedEntity = dataManager.create(KeyValueEntity.class);
            applyFieldMappings(entity, mappedEntity, sources.get(0));
            result.add(mappedEntity);
        }

        // Cross join with remaining sources
        for (int i = 1; i < sources.size(); i++) {
            MetaPackageSource source = sources.get(i);
            List<KeyValueEntity> sourceData = sourceDataMap.get(source.getAlias());

            if (sourceData == null || sourceData.isEmpty()) {
                continue;
            }

            List<KeyValueEntity> newResult = new ArrayList<>();

            for (KeyValueEntity existingEntity : result) {
                for (KeyValueEntity newEntity : sourceData) {
                    KeyValueEntity combined = copyEntity(existingEntity);
                    applyFieldMappings(newEntity, combined, source);
                    newResult.add(combined);
                }
            }

            result = newResult;
        }

        return result;
    }

    /**
     * NESTED: For each record from first source, include all related records from other sources
     */
    private List<KeyValueEntity> mergeNested(Map<String, List<KeyValueEntity>> sourceDataMap,
                                              List<MetaPackageSource> sources) {
        // For now, similar to SEQUENTIAL
        // TODO: Implement proper nested logic with join keys
        return mergeSequential(sourceDataMap, sources);
    }

    /**
     * NEW: Merge data based on relationships (SQL JOIN-like)
     * Supports ONE_TO_MANY, MANY_TO_ONE, and MANY_TO_MANY relationships
     */
    private List<KeyValueEntity> mergeWithRelationships(Map<String, List<KeyValueEntity>> sourceDataMap,
                                                         List<MetaPackageSource> sources,
                                                         List<MetaPackageRelationship> relationships) {
        log.info("Merging data using {} relationships", relationships.size());

        if (sources.isEmpty()) {
            return Collections.emptyList();
        }

        // Filter active relationships only
        List<MetaPackageRelationship> activeRelationships = relationships.stream()
                .filter(r -> r.getIsActive() != null && r.getIsActive())
                .collect(Collectors.toList());

        if (activeRelationships.isEmpty()) {
            log.warn("No active relationships found, falling back to SEQUENTIAL merge");
            return mergeSequential(sourceDataMap, sources);
        }

        // Step 1: Start with first source as base
        MetaPackageSource baseSource = sources.get(0);
        List<KeyValueEntity> baseData = sourceDataMap.get(baseSource.getAlias());

        if (baseData == null || baseData.isEmpty()) {
            log.warn("Base source {} has no data", baseSource.getAlias());
            return Collections.emptyList();
        }

        // Step 2: Initialize result with base source data (mapped to target fields)
        List<KeyValueEntity> result = new ArrayList<>();
        for (KeyValueEntity baseEntity : baseData) {
            KeyValueEntity mappedEntity = dataManager.create(KeyValueEntity.class);
            applyFieldMappings(baseEntity, mappedEntity, baseSource);
            result.add(mappedEntity);
        }

        // Step 3: Apply each relationship to join data
        for (MetaPackageRelationship relationship : activeRelationships) {
            log.debug("Applying relationship: {} ({}) -> {} ({})",
                    relationship.getSourceAlias(), relationship.getSourceField(),
                    relationship.getTargetAlias(), relationship.getTargetField());

            result = applyRelationshipJoin(result, sourceDataMap, sources, relationship);
        }

        log.info("Merge with relationships completed, result count: {}", result.size());
        return result;
    }

    /**
     * Apply a single relationship join to the current result set
     */
    private List<KeyValueEntity> applyRelationshipJoin(List<KeyValueEntity> currentResult,
                                                        Map<String, List<KeyValueEntity>> sourceDataMap,
                                                        List<MetaPackageSource> sources,
                                                        MetaPackageRelationship relationship) {
        // Find source and target in sources list
        MetaPackageSource targetSource = sources.stream()
                .filter(s -> s.getAlias().equals(relationship.getTargetAlias()))
                .findFirst()
                .orElse(null);

        if (targetSource == null) {
            log.warn("Target source not found for alias: {}", relationship.getTargetAlias());
            return currentResult;
        }

        List<KeyValueEntity> targetData = sourceDataMap.get(relationship.getTargetAlias());
        if (targetData == null || targetData.isEmpty()) {
            log.warn("Target source {} has no data", relationship.getTargetAlias());
            return currentResult;
        }

        // Build index for target data for faster lookup
        Map<Object, List<KeyValueEntity>> targetIndex = buildIndex(targetData, relationship.getTargetField());

        RelationshipType type = relationship.getRelationshipType();
        List<KeyValueEntity> joinedResult = new ArrayList<>();

        for (KeyValueEntity entity : currentResult) {
            // Get foreign key value from source field
            Object foreignKeyValue = getFieldValue(entity, relationship.getSourceField());

            if (foreignKeyValue == null) {
                // No match - add entity as-is (LEFT JOIN behavior)
                joinedResult.add(entity);
                continue;
            }

            // Handle different relationship types
            if (type == RelationshipType.MANY_TO_ONE || type == RelationshipType.ONE_TO_MANY) {
                // For MANY_TO_ONE and ONE_TO_MANY: find matching record(s)
                List<KeyValueEntity> matches = targetIndex.getOrDefault(foreignKeyValue, Collections.emptyList());

                if (matches.isEmpty()) {
                    // No match - add entity as-is
                    joinedResult.add(entity);
                } else {
                    // Join with each match
                    for (KeyValueEntity match : matches) {
                        KeyValueEntity joined = copyEntity(entity);
                        applyFieldMappings(match, joined, targetSource);
                        joinedResult.add(joined);
                    }
                }

            } else if (type == RelationshipType.MANY_TO_MANY) {
                // For MANY_TO_MANY: foreignKeyValue might be array/collection
                List<Object> foreignKeys = extractArrayValues(foreignKeyValue);

                if (foreignKeys.isEmpty()) {
                    joinedResult.add(entity);
                } else {
                    // Join with all matching records
                    for (Object fkValue : foreignKeys) {
                        List<KeyValueEntity> matches = targetIndex.getOrDefault(fkValue, Collections.emptyList());
                        for (KeyValueEntity match : matches) {
                            KeyValueEntity joined = copyEntity(entity);
                            applyFieldMappings(match, joined, targetSource);
                            joinedResult.add(joined);
                        }
                    }
                }
            }
        }

        return joinedResult;
    }

    /**
     * Build index for fast lookup: targetField value -> List of entities
     */
    private Map<Object, List<KeyValueEntity>> buildIndex(List<KeyValueEntity> data, String fieldPath) {
        Map<Object, List<KeyValueEntity>> index = new HashMap<>();

        for (KeyValueEntity entity : data) {
            Object value = getFieldValue(entity, fieldPath);
            if (value != null) {
                index.computeIfAbsent(value, k -> new ArrayList<>()).add(entity);
            }
        }

        return index;
    }

    /**
     * Get field value from entity, supporting nested paths (e.g., "address.city")
     */
    private Object getFieldValue(KeyValueEntity entity, String fieldPath) {
        if (fieldPath == null || fieldPath.isBlank()) {
            return null;
        }

        // Simple field (no nesting)
        if (!fieldPath.contains(".")) {
            try {
                return entity.getValue(fieldPath);
            } catch (Exception e) {
                return null;
            }
        }

        // Nested field path
        String[] parts = fieldPath.split("\\.");
        Object current = entity;

        for (String part : parts) {
            if (current == null) {
                return null;
            }

            if (current instanceof KeyValueEntity) {
                try {
                    current = ((KeyValueEntity) current).getValue(part);
                } catch (Exception e) {
                    return null;
                }
            } else if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                // Unsupported type
                return null;
            }
        }

        return current;
    }

    /**
     * Extract array values for MANY_TO_MANY relationships
     * Handles: List, Array, or single value
     */
    @SuppressWarnings("unchecked")
    private List<Object> extractArrayValues(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }

        if (value instanceof List) {
            return new ArrayList<>((List<?>) value);
        }

        if (value instanceof Object[]) {
            return Arrays.asList((Object[]) value);
        }

        // Single value - wrap in list
        return Collections.singletonList(value);
    }

    /**
     * Apply field mappings from source entity to target entity
     */
    private void applyFieldMappings(KeyValueEntity sourceEntity,
                                     KeyValueEntity targetEntity,
                                     MetaPackageSource source) {
        if (source.getFieldMappings() == null || source.getFieldMappings().isEmpty()) {
            return;
        }

        for (MetaPackageFieldMapping mapping : source.getFieldMappings()) {
            Object value = sourceEntity.getValue(mapping.getSourceFieldName());

            // TODO: Apply transform script if present
            if (mapping.getTransformScript() != null && !mapping.getTransformScript().isBlank()) {
                // Use ScriptService for transformation
                // For now, just use raw value
            }

            targetEntity.setValue(mapping.getTargetFieldName(), value);
        }
    }

    /**
     * Copy KeyValueEntity
     */
    private KeyValueEntity copyEntity(KeyValueEntity source) {
        KeyValueEntity copy = dataManager.create(KeyValueEntity.class);

        if (source.getInstanceMetaClass() != null) {
            // Phải set metaClass trước khi setValue
            copy.setInstanceMetaClass(source.getInstanceMetaClass());

            for (MetaProperty property : source.getInstanceMetaClass().getProperties()) {
                String name = property.getName();

                // Lấy giá trị; không dùng hasValue vì Jmix 2.6 không có
                Object value;
                try {
                    // Cách 1: gọi trực tiếp
                    value = source.getValue(name);

                    // Cách 2 (tuỳ chọn): dùng EntityValues
                    // value = EntityValues.getValue(source, name);

                } catch (IllegalArgumentException ex) {
                    // Phòng trường hợp property không tồn tại trong instance map
                    continue;
                }

                // Nếu muốn copy cả null thì bỏ if này
                if (value != null) {
                    // Cách 1:
                    copy.setValue(name, value);

                    // Cách 2 (tuỳ chọn):
                    // EntityValues.setValue(copy, name, value);
                }
            }
        }
        return copy;
    }

    /**
     * Apply sorting to entities
     */
    private List<KeyValueEntity> applySorting(List<KeyValueEntity> entities, Sort sort) {
        List<KeyValueEntity> sorted = new ArrayList<>(entities);

        sorted.sort((e1, e2) -> {
            for (Sort.Order order : sort.getOrders()) {
                Object v1 = e1.getValue(order.getProperty());
                Object v2 = e2.getValue(order.getProperty());

                int cmp = compareValues(v1, v2);
                if (cmp != 0) {
                    return order.getDirection() == Sort.Direction.ASC ? cmp : -cmp;
                }
            }
            return 0;
        });

        return sorted;
    }

    @SuppressWarnings("unchecked")
    private int compareValues(Object v1, Object v2) {
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;

        if (v1 instanceof Comparable && v2 instanceof Comparable) {
            try {
                return ((Comparable<Object>) v1).compareTo(v2);
            } catch (ClassCastException e) {
                return 0;
            }
        }

        return v1.toString().compareTo(v2.toString());
    }

    /**
     * Apply pagination
     */
    private List<KeyValueEntity> applyPagination(List<KeyValueEntity> entities,
                                                  Integer firstResult,
                                                  Integer maxResults) {
        int start = firstResult != null ? firstResult : 0;
        int end = maxResults != null ? Math.min(start + maxResults, entities.size()) : entities.size();

        if (start >= entities.size()) {
            return Collections.emptyList();
        }

        return entities.subList(start, end);
    }
}
