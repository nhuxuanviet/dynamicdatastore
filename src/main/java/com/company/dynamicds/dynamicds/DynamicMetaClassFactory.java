package com.company.dynamicds.dynamicds;

import com.company.dynamicds.dynamicds.entity.MetadataField;
import io.jmix.core.Stores;
import io.jmix.core.impl.keyvalue.KeyValueMetaClassFactory;
import io.jmix.core.metamodel.model.MetaClass;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DynamicMetaClassFactory {

    private final KeyValueMetaClassFactory keyValueMetaClassFactory;
    private final Stores stores;

    public MetaClass buildMetaClass(String entityName, List<MetadataField> fields, String storeName) {
        DynamicMetaClass dynamicMetaClass = new DynamicMetaClass();
        dynamicMetaClass.setName(entityName);
        dynamicMetaClass.setStore(stores.get(storeName));
        KeyValueMetaClassFactory.Configurer configurer =
                keyValueMetaClassFactory.configurer(dynamicMetaClass);

        for (MetadataField field : fields) {
            String fieldName = field.getName();
            Class<?> javaClass = field.getFieldType().getJavaClass();
            configurer.addProperty(fieldName, javaClass);
        }
        return dynamicMetaClass;
    }
}
