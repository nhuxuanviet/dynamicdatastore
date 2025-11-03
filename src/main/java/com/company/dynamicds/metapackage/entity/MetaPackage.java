package com.company.dynamicds.metapackage.entity;

import com.company.dynamicds.metapackage.enums.MergeStrategy;
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Meta Package - Aggregates data from multiple metadata sources
 * Creates a dynamic store with combined fields from multiple API sources
 */
@JmixEntity
@Table(name = "DWH_META_PACKAGE", indexes = {
        @Index(name = "IDX_META_PACKAGE_STORE_NAME", columnList = "STORE_NAME", unique = true)
})
@Entity
public class MetaPackage {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @NotNull
    @Column(name = "NAME", nullable = false)
    private String name;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @NotNull
    @Column(name = "STORE_NAME", nullable = false, unique = true)
    private String storeName;

    @Column(name = "MERGE_STRATEGY")
    private String mergeStrategy;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive = false;

    @Composition
    @OneToMany(mappedBy = "metaPackage")
    @OnDeleteInverse(DeletePolicy.CASCADE)
    private List<MetaPackageSource> sources;

    @Composition
    @OneToMany(mappedBy = "metaPackage")
    @OnDeleteInverse(DeletePolicy.CASCADE)
    private List<MetaPackageRelationship> relationships;

    public List<MetaPackageRelationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<MetaPackageRelationship> relationships) {
        this.relationships = relationships;
    }

    public List<MetaPackageSource> getSources() {
        return sources;
    }

    public void setSources(List<MetaPackageSource> sources) {
        this.sources = sources;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public MergeStrategy getMergeStrategy() {
        // Default to SEQUENTIAL for backward compatibility (not used if relationships exist)
        return mergeStrategy == null ? MergeStrategy.SEQUENTIAL : MergeStrategy.fromId(mergeStrategy);
    }

    public void setMergeStrategy(MergeStrategy mergeStrategy) {
        this.mergeStrategy = mergeStrategy == null ? null : mergeStrategy.getId();
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
