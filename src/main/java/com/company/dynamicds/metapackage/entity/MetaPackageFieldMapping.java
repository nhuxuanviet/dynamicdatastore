package com.company.dynamicds.metapackage.entity;

import com.company.dynamicds.entity.BaseEntity;
import com.company.dynamicds.enums.MetadataFieldType;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Field mapping from source metadata to target meta package
 * Maps: sourceAlias.sourceFieldName -> targetFieldName
 */
@JmixEntity
@Table(name = "DWH_META_PACKAGE_FIELD_MAPPING", indexes = {
        @Index(name = "IDX_META_PKG_FLD_TARGET", columnList = "META_PACKAGE_SOURCE_ID, TARGET_FIELD_NAME", unique = true)
})
@Entity
public class MetaPackageFieldMapping extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "META_PACKAGE_SOURCE_ID", nullable = false)
    private MetaPackageSource metaPackageSource;

    @NotNull
    @Column(name = "SOURCE_FIELD_NAME", nullable = false)
    private String sourceFieldName;

    @NotNull
    @Column(name = "TARGET_FIELD_NAME", nullable = false)
    private String targetFieldName;

    @NotNull
    @Column(name = "DATA_TYPE", nullable = false)
    private String dataType;

    @Lob
    @Column(name = "TRANSFORM_SCRIPT")
    private String transformScript;

    @Column(name = "IS_REQUIRED")
    private Boolean isRequired = false;

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public String getTransformScript() {
        return transformScript;
    }

    public void setTransformScript(String transformScript) {
        this.transformScript = transformScript;
    }

    public MetadataFieldType getDataType() {
        return dataType == null ? null : MetadataFieldType.fromId(dataType);
    }

    public void setDataType(MetadataFieldType dataType) {
        this.dataType = dataType == null ? null : dataType.getId();
    }

    public String getTargetFieldName() {
        return targetFieldName;
    }

    public void setTargetFieldName(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public String getSourceFieldName() {
        return sourceFieldName;
    }

    public void setSourceFieldName(String sourceFieldName) {
        this.sourceFieldName = sourceFieldName;
    }

    public MetaPackageSource getMetaPackageSource() {
        return metaPackageSource;
    }

    public void setMetaPackageSource(MetaPackageSource metaPackageSource) {
        this.metaPackageSource = metaPackageSource;
    }
}
