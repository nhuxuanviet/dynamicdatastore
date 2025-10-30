package com.company.dynamicds.dynamicds.entity;

import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.UUID;

@JmixEntity
@Table(name = "DWH_METADATA_FIELD", indexes = {
        @Index(name = "IDX_DWH_METADATA_FIELD_METADATA_DEFINITION", columnList = "METADATA_DEFINITION_ID")
})
@Entity(name = "dwh_MetadataField")
public class MetadataField {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @InstanceName
    @Column(name = "NAME")
    private String name;

    @Column(name = "FIELD_TYPE")
    private String fieldType;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "METADATA_DEFINITION_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MetadataDefinition metadataDefinition;

    @Column(name = "REQUIRE_")
    private Double require;

    @Column(name = "DESCRIPTION")
    private String description;

    public Double getRequire() {
        return require;
    }

    public void setRequire(Double require) {
        this.require = require;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MetadataDefinition getMetadataDefinition() {
        return metadataDefinition;
    }

    public void setMetadataDefinition(MetadataDefinition metadataDefinition) {
        this.metadataDefinition = metadataDefinition;
    }

    public MetadataFieldType getFieldType() {
        return fieldType == null ? null : MetadataFieldType.fromId(fieldType);
    }

    public void setFieldType(MetadataFieldType fieldType) {
        this.fieldType = fieldType == null ? null : fieldType.getId();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}