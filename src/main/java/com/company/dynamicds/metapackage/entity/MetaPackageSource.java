package com.company.dynamicds.metapackage.entity;

import com.company.dynamicds.dynamicds.entity.MetadataDefinition;
import com.company.dynamicds.entity.BaseEntity;
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
 * Represents a data source (MetadataDefinition) within a MetaPackage
 * Each source has an alias and field mappings
 */
@JmixEntity
@Table(name = "DWH_META_PACKAGE_SOURCE", indexes = {
        @Index(name = "IDX_META_PKG_SRC_ALIAS", columnList = "META_PACKAGE_ID, ALIAS", unique = true)
})
@Entity
public class MetaPackageSource {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "META_PACKAGE_ID", nullable = false)
    private MetaPackage metaPackage;

    @NotNull
    @Column(name = "ALIAS", nullable = false)
    private String alias;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "METADATA_DEFINITION_ID", nullable = false)
    private MetadataDefinition metadataDefinition;

    @NotNull
    @Column(name = "ORDER_INDEX", nullable = false)
    private Integer orderIndex = 0;

    @Composition
    @OneToMany(mappedBy = "metaPackageSource")
    @OnDeleteInverse(DeletePolicy.CASCADE)
    private List<MetaPackageFieldMapping> fieldMappings;

    public List<MetaPackageFieldMapping> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(List<MetaPackageFieldMapping> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public MetadataDefinition getMetadataDefinition() {
        return metadataDefinition;
    }

    public void setMetadataDefinition(MetadataDefinition metadataDefinition) {
        this.metadataDefinition = metadataDefinition;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public MetaPackage getMetaPackage() {
        return metaPackage;
    }

    public void setMetaPackage(MetaPackage metaPackage) {
        this.metaPackage = metaPackage;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
