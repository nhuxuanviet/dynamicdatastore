# Meta Package System - Comprehensive Documentation

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Installation & Setup](#installation--setup)
- [User Guide](#user-guide)
- [Technical Reference](#technical-reference)
- [Performance](#performance)
- [Troubleshooting](#troubleshooting)
- [API Reference](#api-reference)

---

## Overview

### What is Meta Package System?

The **Meta Package System** is a powerful data aggregation framework built on top of Jmix 2.6 that allows you to:

- 📊 **Aggregate data from multiple APIs** into a single unified view
- 🔄 **Dynamically configure field mappings** between different data sources
- 🎯 **Apply Jmix-compatible filtering** (PropertyCondition/LogicalCondition)
- ⚡ **Parallel API fetching** for improved performance (CompletableFuture)
- 🔌 **Runtime store registration** - no application restart required
- 📝 **Three merge strategies**: SEQUENTIAL, CROSS_JOIN, NESTED

### Problem Solved

**Before Meta Package:**
- You have multiple APIs returning different data structures
- Need to manually aggregate and combine data
- Hard-coded integration logic
- No dynamic filtering or querying

**After Meta Package:**
- Define metadata for each API source
- Create a Meta Package to aggregate them
- Select specific fields from each source
- Apply filters, sorting, and pagination
- Query aggregated data through Jmix DataManager

---

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────┐
│                 META PACKAGE                         │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐      │
│  │  Source A  │ │  Source B  │ │  Source C  │      │
│  │ (Customer) │ │  (Orders)  │ │ (Products) │      │
│  └─────┬──────┘ └─────┬──────┘ └─────┬──────┘      │
│        │              │              │               │
│  ┌─────▼──────────────▼──────────────▼─────┐       │
│  │    MetaPackageExecutor (Async Fetch)    │       │
│  │  ┌────────────────────────────────────┐ │       │
│  │  │  CompletableFuture<Source A>       │ │       │
│  │  │  CompletableFuture<Source B>       │ │       │
│  │  │  CompletableFuture<Source C>       │ │       │
│  │  └────────────────────────────────────┘ │       │
│  └────────────┬─────────────────────────────┘       │
│               │                                      │
│  ┌────────────▼───────────────┐                     │
│  │  Merge Strategy            │                     │
│  │  - SEQUENTIAL              │                     │
│  │  - CROSS_JOIN              │                     │
│  │  - NESTED                  │                     │
│  └────────────┬───────────────┘                     │
│               │                                      │
│  ┌────────────▼────────────────┐                    │
│  │  MetaPackageFilterProcessor │                    │
│  │  (Jmix Conditions)          │                    │
│  └────────────┬────────────────┘                    │
│               │                                      │
│  ┌────────────▼────────────────┐                    │
│  │  Sorting & Pagination       │                    │
│  └────────────┬────────────────┘                    │
│               │                                      │
│  ┌────────────▼────────────────┐                    │
│  │  Aggregated KeyValueEntity  │                    │
│  └─────────────────────────────┘                    │
└─────────────────────────────────────────────────────┘
```

### Key Components

#### 1. **MetaPackage Entity** (Database)
- **Purpose**: Defines an aggregation of multiple API sources
- **Fields**:
  - `name`: Display name
  - `storeName`: Unique Jmix store name (e.g., "customerOrdersStore")
  - `mergeStrategy`: How to combine data (SEQUENTIAL/CROSS_JOIN/NESTED)
  - `isActive`: Whether the package is registered with Jmix
  - `sources`: List of data sources

#### 2. **MetaPackageSource Entity**
- **Purpose**: Represents one API source within a package
- **Fields**:
  - `alias`: Short name (e.g., "customer", "order")
  - `metadataDefinition`: Reference to existing MetadataDefinition
  - `orderIndex`: Execution order (0, 1, 2...)
  - `fieldMappings`: Field mapping rules

#### 3. **MetaPackageFieldMapping Entity**
- **Purpose**: Maps fields from source to target
- **Fields**:
  - `sourceFieldName`: Field name in API response
  - `targetFieldName`: Field name in aggregated result
  - `dataType`: Data type (STRING, INTEGER, DOUBLE, BOOLEAN)
  - `transformScript`: Optional JavaScript transformation (future)
  - `isRequired`: Validation flag

#### 4. **MetaPackageExecutor** (Service)
- **Purpose**: Core aggregation engine with async fetching
- **Features**:
  - Parallel API calls using `CompletableFuture`
  - Thread pool management (`ExecutorService`)
  - Timeout handling (30 seconds)
  - Merge strategies implementation
  - Performance logging

#### 5. **MetaPackageFilterProcessor** (Service)
- **Purpose**: Jmix-compatible filtering
- **Supports 18 operators**:
  - Comparison: `=`, `<>`, `>`, `<`, `>=`, `<=`
  - String: `contains`, `doesNotContain`, `startsWith`, `endsWith`
  - Collection: `in`, `notIn`
  - Null: `is_set`

#### 6. **MetaPackageDataStore** (DataStore)
- **Purpose**: Jmix DataStore implementation
- **Extends**: `AbstractDataStore`
- **Methods**:
  - `loadAll(LoadContext)`: Load with filtering/sorting/pagination
  - `getCount(LoadContext)`: Count records
  - `load(LoadContext)`: Load single record
  - `loadValues(ValueLoadContext)`: Load KeyValueEntity

#### 7. **MetaPackageDataStoreRegister** (Runtime Registration)
- **Purpose**: Register/unregister dynamic stores with Jmix
- **Features**:
  - Reflection-based Jmix internals access
  - Spring bean creation at runtime
  - MetaClass registration
  - Store lifecycle management

---

## Installation & Setup

### Prerequisites
- Jmix 2.6.x
- Java 17+
- Existing MetadataDefinition system

### Database Migration

The system automatically creates these tables via Liquibase:

```sql
DWH_META_PACKAGE (id, name, store_name, merge_strategy, is_active, description)
DWH_META_PACKAGE_SOURCE (id, alias, metadata_definition_id, order_index)
DWH_META_PACKAGE_FIELD_MAPPING (id, source_field_name, target_field_name, data_type, transform_script, is_required)
```

### Auto-Initialization

Active Meta Packages are automatically registered on application startup:

```java
@EventListener(ApplicationReadyEvent.class)
public void onApplicationReady() {
    metaPackageService.initializeActiveMetaPackages();
}
```

---

## User Guide

### Step-by-Step: Creating a Meta Package

#### Step 1: Create Metadata Definitions (if not exists)

Navigate to: **Menu → Dynamic Data Stores → Metadata Definitions**

Create two metadata definitions:
1. **Customers API**
   - Name: `customers`
   - Store Name: `apiDataStore`
   - URL: `https://api.example.com/customers`
   - Fields: `customerId` (INT), `customerName` (STRING)

2. **Orders API**
   - Name: `orders`
   - Store Name: `apiDataStore`
   - URL: `https://api.example.com/orders`
   - Fields: `orderId` (INT), `customerId` (INT), `orderDate` (STRING), `totalAmount` (DOUBLE)

#### Step 2: Create Meta Package

Navigate to: **Menu → Meta Packages → Create**

**Basic Information:**
- Name: `Customer Orders Report`
- Store Name: `customerOrdersStore` *(must be unique!)*
- Merge Strategy: `CROSS_JOIN`
- Description: `Combines customer info with orders`

Click **OK**

#### Step 3: Add Data Sources

In the **Data Sources** section:

**Source 1:**
- Alias: `customer`
- Metadata Definition: Select `customers` from dropdown
- Order: `0`

**Source 2:**
- Alias: `order`
- Metadata Definition: Select `orders` from dropdown
- Order: `1`

#### Step 4: Configure Field Mappings

**For Source "customer":**
- Click **Create** in Field Mappings section
- Mapping 1: `customerId` → `customerId` (INTEGER)
- Mapping 2: `customerName` → `name` (STRING)

**For Source "order":**
- Select "order" source in the sources grid
- Click **Create** in Field Mappings section
- Mapping 1: `orderId` → `orderId` (INTEGER)
- Mapping 2: `orderDate` → `date` (STRING)
- Mapping 3: `totalAmount` → `amount` (DOUBLE)

**Save** the Meta Package

#### Step 5: Activate

In the **Meta Packages List View**:
1. Select `Customer Orders Report`
2. Click **Activate** button
3. ✅ Status changes to `Active`

Now the dynamic store `customerOrdersStore` is registered!

#### Step 6: Query Data

**Via DataManager (Java):**
```java
@Autowired
private DataManager dataManager;

List<KeyValueEntity> data = dataManager.load(KeyValueEntity.class)
    .store("customerOrdersStore")
    .query("select e from customerOrdersStore e")
    .condition(PropertyCondition.greater("amount", 1000))
    .list();

for (KeyValueEntity entity : data) {
    String name = entity.getValue("name");
    Double amount = entity.getValue("amount");
    log.info("{}: ${}", name, amount);
}
```

**Via Generic Filter UI:**
1. Create a new view with DataGrid
2. Set data source to store: `customerOrdersStore`
3. Jmix automatically shows all 5 fields:
   - `customerId`, `name`, `orderId`, `date`, `amount`
4. Use Generic Filter to filter by any field

---

## Technical Reference

### Merge Strategies

#### 1. **SEQUENTIAL** (Append All)

```java
MergeStrategy.SEQUENTIAL
```

**Behavior**: Appends all records from all sources in order.

**Example:**
```
API A (Customers): [
  {id: 1, name: "Alice"},
  {id: 2, name: "Bob"}
]

API B (Orders): [
  {orderId: 101, amount: 500},
  {orderId: 102, amount: 1000}
]

Result (4 records):
[
  {customerId: 1, name: "Alice"},          // From A
  {customerId: 2, name: "Bob"},            // From A
  {orderId: 101, amount: 500},             // From B
  {orderId: 102, amount: 1000}             // From B
]
```

**Use Case**: Different entity types, no relationship

#### 2. **CROSS_JOIN** (Cartesian Product)

```java
MergeStrategy.CROSS_JOIN
```

**Behavior**: Combines every record from source A with every record from source B.

**Example:**
```
API A (Customers): [
  {id: 1, name: "Alice"},
  {id: 2, name: "Bob"}
]

API B (Orders): [
  {orderId: 101, amount: 500},
  {orderId: 102, amount: 1000}
]

Result (2 × 2 = 4 records):
[
  {customerId: 1, name: "Alice", orderId: 101, amount: 500},
  {customerId: 1, name: "Alice", orderId: 102, amount: 1000},
  {customerId: 2, name: "Bob", orderId: 101, amount: 500},
  {customerId: 2, name: "Bob", orderId: 102, amount: 1000}
]
```

**Use Case**: Create all combinations for analysis

#### 3. **NESTED** (Master-Detail)

```java
MergeStrategy.NESTED
```

**Behavior**: Currently same as SEQUENTIAL (future: implement join logic)

**Future Enhancement:**
```
For each customer:
  Fetch related orders where order.customerId = customer.id
  Combine into single record
```

---

### Filtering with Jmix Conditions

#### PropertyCondition Examples

```java
// Equal
PropertyCondition.equal("name", "Alice")

// Greater than
PropertyCondition.greater("amount", 1000)

// Less than or equal
PropertyCondition.lessOrEqual("amount", 5000)

// Contains (String)
PropertyCondition.contains("name", "Ali")

// Starts with
PropertyCondition.startsWith("name", "A")

// In list
PropertyCondition.inList("status", Arrays.asList("ACTIVE", "PENDING"))

// Is set (not null)
PropertyCondition.create("amount", PropertyCondition.Operation.IS_SET)
```

#### LogicalCondition Examples

```java
// AND condition
LogicalCondition.and()
    .add(PropertyCondition.equal("status", "ACTIVE"))
    .add(PropertyCondition.greater("amount", 1000))

// OR condition
LogicalCondition.or()
    .add(PropertyCondition.equal("type", "PREMIUM"))
    .add(PropertyCondition.greater("amount", 5000))

// Nested conditions
LogicalCondition.and()
    .add(PropertyCondition.equal("status", "ACTIVE"))
    .add(LogicalCondition.or()
        .add(PropertyCondition.greater("amount", 5000))
        .add(PropertyCondition.equal("vip", true)))
```

#### Supported Operators (18 total)

| Operator | Code | Example |
|----------|------|---------|
| Equal | `=`, `equal` | `amount = 1000` |
| Not Equal | `<>`, `not_equal` | `status <> 'INACTIVE'` |
| Greater | `>`, `greater` | `amount > 1000` |
| Less | `<`, `less` | `amount < 5000` |
| Greater or Equal | `>=`, `greater_or_equal` | `amount >= 1000` |
| Less or Equal | `<=`, `less_or_equal` | `amount <= 5000` |
| Contains | `contains` | `name contains 'John'` |
| Does Not Contain | `doesNotContain`, `not_contains` | |
| Starts With | `startsWith`, `starts_with` | `name startsWith 'A'` |
| Ends With | `endsWith`, `ends_with` | `email endsWith '.com'` |
| In List | `in`, `in_list` | `status in ['ACTIVE', 'PENDING']` |
| Not In List | `notIn`, `not_in_list` | |
| Is Set (Not Null) | `is_set` | `amount is set` |

---

## Performance

### Async Fetching with CompletableFuture

**Key Features:**
- All API sources fetched **in parallel** (not sequentially)
- Thread pool sized based on CPU cores (min 4 threads)
- Timeout: 30 seconds
- Graceful degradation: returns partial results on timeout

**Performance Improvement:**

```
WITHOUT Async (Sequential):
API A: 2s
API B: 3s
API C: 2.5s
Total: 7.5s ❌

WITH Async (Parallel):
API A: 2s  ┐
API B: 3s  ├─ Concurrent
API C: 2.5s┘
Total: 3s ✅ (2.5x faster)
```

**Code Snippet:**

```java
// All sources fetched concurrently
List<CompletableFuture<Map.Entry<String, List<KeyValueEntity>>>> futures =
    sources.stream()
        .map(source -> CompletableFuture.supplyAsync(() -> {
            return Map.entry(
                source.getAlias(),
                restInvoker.loadList(source.getMetadataDefinition())
            );
        }, executorService))
        .collect(Collectors.toList());

// Wait for all with timeout
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .get(30, TimeUnit.SECONDS);
```

### Performance Metrics

Logged automatically:
```
MetaPackageExecutor - Executing meta package: Customer Orders Report
MetaPackageExecutor - Source customer returned 150 records in 1850ms
MetaPackageExecutor - Source order returned 450 records in 2150ms
MetaPackageExecutor - Meta package returned 67500 records in 2200ms
```

### Optimization Tips

1. **Reduce field mappings**: Only map fields you need
2. **Apply filters early**: Use PropertyCondition to reduce data
3. **Use pagination**: Set `maxResults` to limit records
4. **Monitor thread pool**: Adjust `threadPoolSize` if needed
5. **Cache API responses**: (future enhancement)

---

## Troubleshooting

### Issue 1: Store Already Registered

**Error:**
```
IllegalStateException: Store 'customerOrdersStore' is already registered
```

**Solution:**
1. Go to Meta Packages List View
2. Find the package with same `storeName`
3. Click **Deactivate** first
4. Then activate the new one

### Issue 2: MetadataDefinition Not Found

**Error:**
```
IllegalStateException: No metadata found for apiDataStore/customers
```

**Solution:**
1. Check MetadataDefinition exists
2. Verify `name` and `storeName` match exactly
3. Ensure metadataFields are configured

### Issue 3: Timeout Fetching Data

**Error:**
```
TimeoutException: Timeout waiting for source data fetching
```

**Solution:**
1. Check API endpoints are reachable
2. Increase timeout in `MetaPackageExecutor` (default: 30s)
3. Check network connectivity
4. Review API performance

### Issue 4: Field Mapping Not Working

**Symptom**: Field values are null in aggregated result

**Solution:**
1. Check `sourceFieldName` matches API response field exactly
2. Verify `dataType` is correct
3. Check API returns data for that field
4. Review logs for parsing errors

### Issue 5: Cross Join Too Many Records

**Symptom**: CROSS_JOIN returns millions of records

**Solution:**
1. Apply filters to reduce source data FIRST
2. Use pagination
3. Consider SEQUENTIAL or NESTED strategy instead
4. Add WHERE conditions to limit results

---

## API Reference

### MetaPackageService

```java
@Service
public class MetaPackageService {

    // Activate a meta package (register with Jmix)
    void activate(MetaPackage metaPackage);

    // Deactivate a meta package (unregister)
    void deactivate(MetaPackage metaPackage);

    // Reload configuration (re-register)
    void reload(MetaPackage metaPackage);

    // Initialize all active packages on startup
    void initializeActiveMetaPackages();
}
```

### MetaPackageExecutor

```java
@Component
public class MetaPackageExecutor {

    // Execute data loading with filtering/sorting/pagination
    List<KeyValueEntity> executeLoad(
        MetaPackage metaPackage,
        Condition condition,      // Jmix filter condition
        Sort sort,                 // Sorting orders
        Integer firstResult,       // Pagination offset
        Integer maxResults         // Pagination limit
    );
}
```

### MetaPackageFilterProcessor

```java
@Component
public class MetaPackageFilterProcessor {

    // Apply filtering to loaded entities
    List<KeyValueEntity> applyFilter(
        List<KeyValueEntity> entities,
        Condition condition
    );
}
```

### MetaPackageDataStoreRegister

```java
@Component
public class MetaPackageDataStoreRegister {

    // Register meta package as dynamic store
    void registerMetaPackage(MetaPackage metaPackage);

    // Unregister meta package store
    void unregisterMetaPackage(String storeName);

    // Check if registered
    boolean isRegistered(String storeName);

    // Get registered store
    MetaPackageDataStore getStore(String storeName);
}
```

---

## Advanced Topics

### Custom Field Transformations

**Future Enhancement**: Execute JavaScript transformations

```javascript
// Transform Script Example (planned)
function transform(value, sourceEntity) {
    // Convert to uppercase
    if (typeof value === 'string') {
        return value.toUpperCase();
    }

    // Calculate derived field
    if (sourceEntity.quantity && sourceEntity.price) {
        return sourceEntity.quantity * sourceEntity.price;
    }

    return value;
}
```

### JOIN-based Nested Strategy

**Future Enhancement**: Proper JOIN logic

```java
// Configuration (planned)
MetaPackageSource orderSource = ...;
orderSource.setJoinKey("customerId");
orderSource.setJoinType(JoinType.INNER);

// Behavior
for (customer : customers) {
    List<Order> relatedOrders = orders.stream()
        .filter(o -> o.customerId == customer.id)
        .collect(Collectors.toList());

    // Merge customer + orders
}
```

### Caching Strategy

**Future Enhancement**: Cache API responses

```java
@Cacheable(value = "metaPackageCache", key = "#source.alias")
public List<KeyValueEntity> loadList(MetadataDefinition metadata) {
    // ...
}
```

---

## Best Practices

### 1. Naming Conventions
- **Store Name**: Use camelCase (e.g., `customerOrdersStore`)
- **Alias**: Short, lowercase (e.g., `customer`, `order`)
- **Target Fields**: Descriptive (e.g., `customerName`, not `cn`)

### 2. Performance
- ✅ Use pagination for large datasets
- ✅ Apply filters to reduce data volume
- ✅ Map only necessary fields
- ❌ Avoid CROSS_JOIN with large datasets
- ❌ Don't activate packages you don't use

### 3. Security
- ✅ Validate API authentication
- ✅ Use HTTPS for API endpoints
- ✅ Sanitize user inputs in filters
- ❌ Don't expose sensitive fields

### 4. Maintenance
- ✅ Document field mappings
- ✅ Test with sample data first
- ✅ Monitor performance logs
- ❌ Don't change storeName after activation

---

## Roadmap

### Planned Features
- [ ] JOIN-based NESTED strategy with join keys
- [ ] JavaScript transformation scripts (GraalVM)
- [ ] Caching layer for API responses
- [ ] Push-down filters to API query params
- [ ] UI-based filter builder
- [ ] Export aggregated data (CSV, Excel)
- [ ] Scheduling for periodic refresh
- [ ] Webhooks for data change notifications

---

## License & Credits

**Built with:**
- Jmix 2.6.2
- Spring Boot 3.x
- CompletableFuture (Java Concurrency)
- Jmix RestDS pattern

**Author**: DynamicStore Team
**Version**: 1.0.0
**Last Updated**: 2025-11-01

---

## Support

For issues and questions:
- Check [Troubleshooting](#troubleshooting) section
- Review application logs
- Contact development team

---

**🎉 Congratulations! You now know everything about the Meta Package System.**
