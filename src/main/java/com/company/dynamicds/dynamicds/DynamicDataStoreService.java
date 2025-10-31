package com.company.dynamicds.dynamicds;

import com.company.dynamicds.apisetting.dto.ApiResponse;
import com.company.dynamicds.apisetting.entity.ApiSetting;
import com.company.dynamicds.apisetting.enums.HttpMethodType;
import com.company.dynamicds.apisetting.service.ApiExecutorService;
import com.company.dynamicds.dynamicds.entity.MetadataDefinition;
import com.company.dynamicds.dynamicds.entity.MetadataField;
import com.company.dynamicds.dynamicds.entity.MetadataFieldType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicDataStoreService {

    private final DataManager dataManager;
    private final ObjectMapper objectMapper;
    private final ApiExecutorService apiExecutorService;

    // Method mới - chỉ generate fields, không save
    public List<MetadataField> generateFieldsFromApi(String url) throws RuntimeException {
        log.info("Calling API to generate metadata fields from URL: {}", url);

        ApiSetting setting = dataManager.create(ApiSetting.class);
        setting.setUseRawUrl(true);
        setting.setFinalUrl(url);
        setting.setHttpMethod(HttpMethodType.GET);

        ApiResponse response = apiExecutorService.sendRequest(setting);

        if (!response.isSuccess()) {
            throw new RuntimeException("API call failed: " + response.getStatusText());
        }

        String rawJson = response.getBodyText();
        return this.parseFieldsFromJson(rawJson);
    }

    // Method helper để parse fields từ JSON
    private List<MetadataField> parseFieldsFromJson(String rawJson) throws RuntimeException {
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            if (!root.isArray() || root.isEmpty()) {
                throw new RuntimeException("Expected non-empty JSON array");
            }

            JsonNode sample = root.get(0);
            List<MetadataField> fields = new ArrayList<>();

            Iterator<String> fieldNames = sample.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                JsonNode value = sample.get(name);

                MetadataField field = dataManager.create(MetadataField.class);
                field.setName(name);
                field.setFieldType(guessType(value));
                fields.add(field);
            }

            log.info("Parsed {} fields from JSON", fields.size());
            return fields;

        } catch (Exception e) {
            log.error("Failed to parse JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    // Sửa lại method cũ - KHÔNG save, chỉ update entity hiện tại
    public MetadataDefinition generateFromApi(MetadataDefinition metadataDefinition) throws RuntimeException {
        log.info("Generating fields for entity [{}]...", metadataDefinition.getName());

        // Generate fields từ API
        List<MetadataField> fields = generateFieldsFromApi(metadataDefinition.getUrl());

        // Luôn tạo list mới để tránh null pointer
        List<MetadataField> newFields = new ArrayList<>();

        // Add new fields và set parent reference
        for (MetadataField field : fields) {
            field.setMetadataDefinition(metadataDefinition);
            newFields.add(field);
        }

        // Set list mới cho metadata definition
        metadataDefinition.setMetadataFields(newFields);

        log.info("Updated entity [{}] with {} fields", metadataDefinition.getName(), fields.size());

        return metadataDefinition;
    }

    // Method cũ - rename và mark as deprecated nếu vẫn cần cho backward compatibility
    @Deprecated
    public MetadataDefinition generateAndSaveFromJson(String rawJson, String entityName, String storeName, String url) throws RuntimeException {
        // Method này chỉ dùng khi cần tạo và save entity mới
        // KHÔNG dùng trong DetailView
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            if (!root.isArray() || root.isEmpty()) {
                throw new RuntimeException("Expected non-empty JSON array");
            }

            JsonNode sample = root.get(0);
            List<MetadataField> fields = new ArrayList<>();

            Iterator<String> fieldNames = sample.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                JsonNode value = sample.get(name);

                MetadataField field = dataManager.create(MetadataField.class);
                field.setName(name);
                field.setFieldType(guessType(value));
                fields.add(field);
            }

            MetadataDefinition definition = dataManager.create(MetadataDefinition.class);
            definition.setName(entityName);
            definition.setStoreName(storeName);
            definition.setUrl(url);
            definition.setMetadataFields(fields);

            for (MetadataField field : fields) {
                field.setMetadataDefinition(definition);
            }

            // Chỉ save khi thực sự cần tạo entity mới
            SaveContext ctx = new SaveContext().saving(definition);
            fields.forEach(ctx::saving);
            dataManager.save(ctx);

            log.info("NEW Metadata saved for [{}] with {} fields", entityName, fields.size());
            return definition;

        } catch (Exception e) {
            log.error("Failed to parse JSON for entity [{}]: {}", entityName, e.getMessage(), e);
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    private MetadataFieldType guessType(JsonNode value) {
        if (value.isInt() || value.isLong()) {
            return MetadataFieldType.INTEGER;
        } else if (value.isDouble() || value.isFloat()) {
            return MetadataFieldType.DOUBLE;
        } else if (value.isBoolean()) {
            return MetadataFieldType.BOOLEAN;
        } else {
            return MetadataFieldType.STRING;
        }
    }
}