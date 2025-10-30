package com.company.dynamicds.dynamicds.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "DWH_METADATA_DEFINITION")
@Entity(name = "dwh_MetadataDefinition")
public class MetadataDefinition {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Column(name = "URL")
    private String url;

    @InstanceName
    @Column(name = "NAME")
    private String name;

    @Composition
    @OneToMany(mappedBy = "metadataDefinition")
    private List<MetadataField> metadataFields;

    @Column(name = "STORE_NAME")
    private String storeName;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public List<MetadataField> getMetadataFields() {
        return metadataFields;
    }

    public void setMetadataFields(List<MetadataField> metadataFields) {
        this.metadataFields = metadataFields;
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