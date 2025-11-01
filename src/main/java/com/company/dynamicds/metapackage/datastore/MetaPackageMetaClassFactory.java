package com.company.dynamicds.metapackage.datastore;

import com.company.dynamicds.dynamicds.DynamicMetaClass;
import com.company.dynamicds.metapackage.entity.MetaPackage;
import com.company.dynamicds.metapackage.entity.MetaPackageFieldMapping;
import com.company.dynamicds.metapackage.entity.MetaPackageSource;
import io.jmix.core.metamodel.datatype.Datatype;
import io.jmix.core.metamodel.datatype.DatatypeRegistry;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating dynamic MetaClass from MetaPackage field mappings
 * Similar to DynamicMetaClassFactory
 */
@Component
public class MetaPackageMetaClassFactory {

    private static final Logger log = LoggerFactory.getLogger(MetaPackageMetaClassFactory.class);

    private final DatatypeRegistry datatypeRegistry;

    public MetaPackageMetaClassFactory(DatatypeRegistry datatypeRegistry) {
        this.datatypeRegistry = datatypeRegistry;
    }

    /**
     * Create MetaClass from MetaPackage field mappings
     */
    public MetaClass createMetaClass(MetaPackage metaPackage) {
        String entityName = metaPackage.getStoreName();
        log.info("Creating MetaClass for MetaPackage: {} (entity: {})",
                metaPackage.getName(), entityName);

        DynamicMetaClass metaClass = new DynamicMetaClass(entityName);

        // Collect all field mappings from all sources
        List<MetaPackageFieldMapping> allMappings = collectAllMappings(metaPackage);

        // Create MetaProperty for each field mapping
        for (MetaPackageFieldMapping mapping : allMappings) {
            try {
                Class<?> javaClass = mapping.getDataType().getJavaClass();
                Datatype<?> datatype = datatypeRegistry.get(javaClass);

                MetaProperty property = metaClass.createPropertyFromDatatype(
                        mapping.getTargetFieldName(),
                        datatype
                );

                log.debug("Added property: {} (type: {})",
                        mapping.getTargetFieldName(), javaClass.getSimpleName());

            } catch (Exception e) {
                log.error("Failed to create property for field: {}", mapping.getTargetFieldName(), e);
            }
        }

        log.info("MetaClass created with {} properties", metaClass.getProperties().size());
        return metaClass;
    }

    /**
     * Collect all field mappings from all sources
     */
    private List<MetaPackageFieldMapping> collectAllMappings(MetaPackage metaPackage) {
        List<MetaPackageFieldMapping> allMappings = new ArrayList<>();

        if (metaPackage.getSources() == null) {
            return allMappings;
        }

        for (MetaPackageSource source : metaPackage.getSources()) {
            if (source.getFieldMappings() != null) {
                allMappings.addAll(source.getFieldMappings());
            }
        }

        return allMappings;
    }
}
