package com.company.dynamicds.dynamicds;

import com.company.dynamicds.apisetting.dto.ApiResponse;
import com.company.dynamicds.apisetting.entity.ApiSetting;
import com.company.dynamicds.apisetting.enums.HttpMethodType;
import com.company.dynamicds.apisetting.service.ApiExecutorService;
import com.company.dynamicds.dynamicds.entity.MetadataDefinition;
import com.company.dynamicds.dynamicds.entity.MetadataField;
import com.company.dynamicds.repository.MetadataDefinitionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.DataManager;
import io.jmix.core.entity.KeyValueEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicKeyValueRestInvoker {

    private final ApiExecutorService apiExecutorService;
    private final ObjectMapper objectMapper;
    private final DynamicMetaClassFactory dynamicMetaClassFactory;
    private final MetadataDefinitionRepository metadataDefinitionRepository;
    private final DataManager dataManager;

    public List<KeyValueEntity> loadList(String dataStoreName, String entityName) throws RuntimeException {

        MetadataDefinition metadata = metadataDefinitionRepository.findByNameAndStoreName(entityName, dataStoreName)
                .orElseThrow(() -> new IllegalStateException("No metadata found for " + dataStoreName + "/" + entityName));

        return loadList(metadata);
    }

    /**
     * Load list of entities from API using MetadataDefinition
     */
    public List<KeyValueEntity> loadList(MetadataDefinition metadata) throws RuntimeException {
        ApiSetting apiSetting = convertMetadataToApiSetting(metadata);
        ApiResponse response = apiExecutorService.sendRequest(apiSetting);

        if (!response.isSuccess()) {
            throw new RuntimeException("API call failed: " + response.getStatusText());
        }

        return parseJsonToEntities(response.getBodyText(), metadata.getMetadataFields());
    }

    public ApiSetting convertMetadataToApiSetting(MetadataDefinition metadata) {
        ApiSetting setting = dataManager.create(ApiSetting.class);
        setting.setFinalUrl(metadata.getUrl());
        setting.setHttpMethod(HttpMethodType.GET);
        setting.setUseRawUrl(true);
        return setting;
    }

    private List<KeyValueEntity> parseJsonToEntities(String rawJson, List<MetadataField> fields) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            List<KeyValueEntity> result = new ArrayList<>();

            if (!root.isArray()) {
                throw new RuntimeException("Expected JSON array but got: " + root);
            }

            for (JsonNode item : root) {
                KeyValueEntity entity = dataManager.create(KeyValueEntity.class);

                for (MetadataField field : fields) {
                    String fieldName = field.getName();
                    JsonNode valueNode = item.get(fieldName);

                    if (valueNode != null && !valueNode.isNull()) {
                        Object value;

                        if (valueNode.isArray() || valueNode.isObject()) {
                            // Convert array/object to JSON string
                            value = objectMapper.writeValueAsString(valueNode);
                        } else {
                            // Convert normally
                            value = objectMapper.convertValue(valueNode, field.getFieldType().getJavaClass());
                        }

                        entity.setValue(fieldName, value);
                    }
                }

                result.add(entity);
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON to entity list", e);
        }
    }


}
