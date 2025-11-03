package com.company.dynamicds.metapackage.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.company.dynamicds.dynamicds.DynamicKeyValueRestInvoker;
import com.company.dynamicds.metapackage.datastore.MetaPackageMetaClassFactory;
import com.company.dynamicds.metapackage.entity.MetaPackage;
import com.company.dynamicds.metapackage.entity.MetaPackageFieldMapping;
import com.company.dynamicds.metapackage.entity.MetaPackageSource;
import com.company.dynamicds.metapackage.enums.MergeStrategy;

import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanRepository;
import io.jmix.core.FetchPlans;
import io.jmix.core.Sort;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.querycondition.Condition;
import jakarta.annotation.PreDestroy;

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
    private final MetaPackageMetaClassFactory metaClassFactory;

    // Thread pool for async fetching
    private final ExecutorService executorService;

    public MetaPackageExecutor(DynamicKeyValueRestInvoker restInvoker,
                               MetaPackageFilterProcessor filterProcessor,
                               DataManager dataManager,
                               FetchPlanRepository fetchPlanRepository, FetchPlans fetchPlans,
                               MetaPackageMetaClassFactory metaClassFactory) {
        this.restInvoker = restInvoker;
        this.filterProcessor = filterProcessor;
        this.dataManager = dataManager;
        this.fetchPlanRepository = fetchPlanRepository;
        this.fetchPlans = fetchPlans;
        this.metaClassFactory = metaClassFactory;

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

        // Create MetaClass for mapped KeyValueEntity of this MetaPackage
        MetaClass targetMetaClass = metaClassFactory.createMetaClass(fullPackage, fullPackage.getStoreName());

        // Merge data according to strategy and ensure metaClass is set for created entities
        List<KeyValueEntity> mergedData = mergeData(sourceDataMap, sortedSources, fullPackage.getMergeStrategy(), targetMetaClass);

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
    /** Load MetaPackage với đầy đủ graph (sources, mappings, metadata definitions) */
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
                        // Chuẩn hoá/flatten dữ liệu nếu có mảng lồng (vd: cart.products[])
                        data = flattenSourceData(source, data);
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
     * Flatten dữ liệu nguồn khi có field dạng mảng (ví dụ: products[]) và các mapping trỏ tới phần tử con
     * Quy ước: nếu một sourceFieldName có dạng "prefix.child" và entity chứa prefix là List,
     * ta sẽ expand thành nhiều bản ghi, mỗi bản ghi tương ứng một phần tử trong List đó.
     */
    @SuppressWarnings("unchecked")
    private List<KeyValueEntity> flattenSourceData(MetaPackageSource source, List<KeyValueEntity> original) {
        if (source.getFieldMappings() == null || source.getFieldMappings().isEmpty() || original == null || original.isEmpty()) {
            return original;
        }

        // Tìm các tiền tố mảng (prefix) xuất hiện trong mapping, ví dụ: products.productId -> prefix = products
        Set<String> arrayPrefixes = source.getFieldMappings().stream()
                .map(MetaPackageFieldMapping::getSourceFieldName)
                .filter(n -> n != null && n.contains("."))
                .map(n -> n.substring(0, n.indexOf('.')))
                .collect(Collectors.toSet());

        if (arrayPrefixes.isEmpty()) {
            return original;
        }

        List<KeyValueEntity> flattened = new ArrayList<>();

        for (KeyValueEntity entity : original) {
            // Tìm prefix mảng đầu tiên có trong entity
            String usedPrefix = null;
            List<Object> arrayValue = null;
            for (String prefix : arrayPrefixes) {
                Object v;
                try {
                    v = entity.getValue(prefix);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                if (v instanceof List<?> list) {
                    arrayValue = (List<Object>) list;
                    usedPrefix = prefix;
                    break;
                }
            }

            if (usedPrefix == null || arrayValue == null) {
                // Không có mảng cần flatten -> giữ nguyên
                flattened.add(entity);
                continue;
            }

            // Phân loại mapping: thuộc prefix (áp vào phần tử con) và không thuộc prefix (lấy từ entity gốc)
            List<MetaPackageFieldMapping> prefixMappings = new ArrayList<>();
            List<MetaPackageFieldMapping> rootMappings = new ArrayList<>();
            for (MetaPackageFieldMapping m : source.getFieldMappings()) {
                if (m.getSourceFieldName() != null && m.getSourceFieldName().startsWith(usedPrefix + ".")) {
                    prefixMappings.add(m);
                } else {
                    rootMappings.add(m);
                }
            }

            // Expand theo từng phần tử con
            for (Object item : arrayValue) {
                KeyValueEntity row = new KeyValueEntity();

                // Áp các mapping từ root (scalar)
                for (MetaPackageFieldMapping m : rootMappings) {
                    Object value = safeGetNestedValue(entity, m.getSourceFieldName());
                    row.setValue(m.getTargetFieldName(), value);
                }

                // Áp các mapping lấy từ phần tử con
                for (MetaPackageFieldMapping m : prefixMappings) {
                    String remainder = m.getSourceFieldName().substring((usedPrefix + ".").length());
                    Object value = safeGetNestedFromObject(item, remainder);
                    row.setValue(m.getTargetFieldName(), value);
                }

                flattened.add(row);
            }
        }

        return flattened.isEmpty() ? original : flattened;
    }

    private Object safeGetNestedValue(KeyValueEntity entity, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (!path.contains(".")) {
            try {
                return entity.getValue(path);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        String[] parts = path.split("\\.");
        Object current = entity;
        for (String p : parts) {
            if (current == null) return null;
            if (current instanceof KeyValueEntity kve) {
                try {
                    current = kve.getValue(p);
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            } else if (current instanceof Map<?,?> map) {
                current = map.get(p);
            } else {
                return null;
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private Object safeGetNestedFromObject(Object base, String path) {
        if (base == null || path == null || path.isBlank()) {
            return null;
        }
        String[] parts = path.split("\\.");
        Object current = base;
        for (String p : parts) {
            if (current == null) return null;
            if (current instanceof Map<?,?> map) {
                current = map.get(p);
            } else if (current instanceof KeyValueEntity kve) {
                try {
                    current = kve.getValue(p);
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return current;
    }

    /**
     * Merge data from multiple sources according to merge strategy
     */
    private List<KeyValueEntity> mergeData(Map<String, List<KeyValueEntity>> sourceDataMap,
                                            List<MetaPackageSource> sources,
                                            MergeStrategy strategy,
                                            MetaClass targetMetaClass) {
        return switch (strategy) {
            case SEQUENTIAL -> mergeSequential(sourceDataMap, sources, targetMetaClass);
            case CROSS_JOIN -> mergeCrossJoin(sourceDataMap, sources, targetMetaClass);
            case NESTED -> mergeNested(sourceDataMap, sources, targetMetaClass);
        };
    }

    /**
     * SEQUENTIAL: Append all records from all sources
     */
    private List<KeyValueEntity> mergeSequential(Map<String, List<KeyValueEntity>> sourceDataMap,
                                                  List<MetaPackageSource> sources,
                                                  MetaClass targetMetaClass) {
        List<KeyValueEntity> result = new ArrayList<>();

        for (MetaPackageSource source : sources) {
            List<KeyValueEntity> sourceData = sourceDataMap.get(source.getAlias());
            if (sourceData == null || sourceData.isEmpty()) {
                continue;
            }

            for (KeyValueEntity entity : sourceData) {
                KeyValueEntity mappedEntity = dataManager.create(KeyValueEntity.class);
                mappedEntity.setInstanceMetaClass(targetMetaClass);
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
                                                 List<MetaPackageSource> sources,
                                                 MetaClass targetMetaClass) {
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
            mappedEntity.setInstanceMetaClass(targetMetaClass);
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
                    KeyValueEntity combined = copyEntity(existingEntity, targetMetaClass);
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
                                              List<MetaPackageSource> sources,
                                              MetaClass targetMetaClass) {
        List<KeyValueEntity> result = new ArrayList<>();

        if (sources.isEmpty()) {
            return result;
        }

        // Chọn source đầu tiên làm ROOT (ví dụ: cart)
        MetaPackageSource rootSource = sources.get(0);
        List<KeyValueEntity> rootData = sourceDataMap.getOrDefault(rootSource.getAlias(), Collections.emptyList());

        if (rootData.isEmpty()) {
            return result;
        }

        // Tạo index cho các source còn lại theo các "targetFieldName" chung với ROOT
        Map<MetaPackageSource, Map<String, List<KeyValueEntity>>> sourceIndexes = new HashMap<>();

        // Tập hợp target fields của root
        Set<String> rootTargetFields = rootSource.getFieldMappings() == null ? Collections.emptySet()
                : rootSource.getFieldMappings().stream().map(MetaPackageFieldMapping::getTargetFieldName).collect(Collectors.toSet());

        for (int i = 1; i < sources.size(); i++) {
            MetaPackageSource src = sources.get(i);
            List<KeyValueEntity> data = sourceDataMap.getOrDefault(src.getAlias(), Collections.emptyList());
            if (data.isEmpty()) {
                continue;
            }

            // Xác định các target field chung để join (ví dụ: userId, productId)
            Set<String> srcTargetFields = src.getFieldMappings() == null ? Collections.emptySet()
                    : src.getFieldMappings().stream().map(MetaPackageFieldMapping::getTargetFieldName).collect(Collectors.toSet());
            Set<String> commonKeys = new HashSet<>(rootTargetFields);
            commonKeys.retainAll(srcTargetFields);

            // Nếu không có khoá chung, bỏ qua index (sẽ append sau)
            if (commonKeys.isEmpty()) {
                continue;
            }

            // Với nhiều khoá chung, tạo composite key bằng nối chuỗi
            Map<String, List<KeyValueEntity>> index = new HashMap<>();
            for (KeyValueEntity e : data) {
                String key = buildCompositeKey(e, commonKeys);
                index.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
            }
            sourceIndexes.put(src, index);
        }

        // Duyệt root, join các source khác theo key, đổ về targetMetaClass
        for (KeyValueEntity root : rootData) {
            KeyValueEntity out = dataManager.create(KeyValueEntity.class);
            out.setInstanceMetaClass(targetMetaClass);
            // Đầu tiên: map từ root
            applyFieldMappings(root, out, rootSource);

            for (int i = 1; i < sources.size(); i++) {
                MetaPackageSource src = sources.get(i);
                Map<String, List<KeyValueEntity>> index = sourceIndexes.get(src);
                if (index == null) {
                    // Không có key chung -> append đơn giản các field từ bản ghi đầu (nếu có)
                    List<KeyValueEntity> data = sourceDataMap.getOrDefault(src.getAlias(), Collections.emptyList());
                    if (!data.isEmpty()) {
                        applyFieldMappings(data.get(0), out, src);
                    }
                    continue;
                }

                // Tạo key từ bản ghi root theo các khoá chung với src
                Set<String> srcTargetFields = src.getFieldMappings() == null ? Collections.emptySet()
                        : src.getFieldMappings().stream().map(MetaPackageFieldMapping::getTargetFieldName).collect(Collectors.toSet());
                Set<String> commonKeys = new HashSet<>(rootTargetFields);
                commonKeys.retainAll(srcTargetFields);
                String key = buildCompositeKey(out, commonKeys); // dùng out vì out đã có giá trị từ root theo target fields

                List<KeyValueEntity> matches = index.get(key);
                if (matches != null && !matches.isEmpty()) {
                    // Gộp field từ bản ghi match đầu tiên; có thể mở rộng để merge nhiều bản ghi
                    applyFieldMappings(matches.get(0), out, src);
                }
            }

            result.add(out);
        }

        return result;
    }

    private String buildCompositeKey(KeyValueEntity entity, Set<String> keyFields) {
        if (keyFields == null || keyFields.isEmpty()) {
            return "";
        }
        return keyFields.stream()
                .sorted()
                .map(k -> {
                    Object v = null;
                    try {
                        v = entity.getValue(k);
                    } catch (IllegalArgumentException ignored) {
                    }
                    return v == null ? "" : String.valueOf((Object) v);
                })
                .collect(Collectors.joining("|"));
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
    private KeyValueEntity copyEntity(KeyValueEntity source, MetaClass targetMetaClass) {
        KeyValueEntity copy = dataManager.create(KeyValueEntity.class);
        copy.setInstanceMetaClass(targetMetaClass);

        // Duyệt qua danh sách property của target meta class để copy giá trị trùng tên nếu có
        for (MetaProperty property : targetMetaClass.getProperties()) {
            String name = property.getName();
            Object value;
            try {
                value = source.getValue(name);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            if (value != null) {
                copy.setValue(name, value);
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
