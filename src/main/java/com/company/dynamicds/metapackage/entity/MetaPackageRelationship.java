package com.company.dynamicds.metapackage.entity;

import com.company.dynamicds.metapackage.enums.RelationshipType;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Defines relationship between two data sources within a MetaPackage
 * Similar to database foreign key relationships for joining data
 *
 * Example: Carts.userId = Users.id (MANY_TO_ONE)
 */
@JmixEntity
@Table(name = "DWH_META_PACKAGE_RELATIONSHIP", indexes = {
        @Index(name = "IDX_META_PKG_REL_PACKAGE", columnList = "META_PACKAGE_ID")
})
@Entity
public class MetaPackageRelationship {

    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "META_PACKAGE_ID", nullable = false)
    private MetaPackage metaPackage;

    @InstanceName
    @NotNull
    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    @NotNull
    @Column(name = "SOURCE_ALIAS", nullable = false, length = 100)
    private String sourceAlias;

    @NotNull
    @Column(name = "SOURCE_FIELD", nullable = false, length = 200)
    private String sourceField;

    @NotNull
    @Column(name = "TARGET_ALIAS", nullable = false, length = 100)
    private String targetAlias;

    @NotNull
    @Column(name = "TARGET_FIELD", nullable = false, length = 200)
    private String targetField;

    @NotNull
    @Column(name = "RELATIONSHIP_TYPE", nullable = false)
    private String relationshipType;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive = true;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MetaPackage getMetaPackage() {
        return metaPackage;
    }

    public void setMetaPackage(MetaPackage metaPackage) {
        this.metaPackage = metaPackage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceAlias() {
        return sourceAlias;
    }

    public void setSourceAlias(String sourceAlias) {
        this.sourceAlias = sourceAlias;
    }

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    public String getTargetAlias() {
        return targetAlias;
    }

    public void setTargetAlias(String targetAlias) {
        this.targetAlias = targetAlias;
    }

    public String getTargetField() {
        return targetField;
    }

    public void setTargetField(String targetField) {
        this.targetField = targetField;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType == null ? null : RelationshipType.fromId(relationshipType);
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType == null ? null : relationshipType.getId();
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
