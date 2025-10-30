package com.company.dynamicds.apisetting.entity;


import com.company.dynamicds.apisetting.enums.ApiFormDataType;
import com.company.dynamicds.entity.BaseEntity;
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;

import java.util.UUID;

@JmixEntity
@Table(name = "DWH_API_FORM_DATA_FIELD", indexes = {
        @Index(name = "IDX_DWH_API_FORM_DATA_FIELD_API_BODY", columnList = "API_BODY_ID")
})
@Entity(name = "dwh_ApiFormDataField")
public class ApiFormDataField extends BaseEntity {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Column(name = "FORM_KEY")
    private String key;

    @Column(name = "FORM_VALUE")
    private String value;

    @InstanceName
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DATA_TYPE")
    private String dataType;

    @Column(name = "FILE_NAME")
    private String fileName;

    @Column(name = "FILE_CONTENT")
    private byte[] fileContent;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "API_BODY_ID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ApiBody apiBody;

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    public ApiBody getApiBody() {
        return apiBody;
    }

    public void setApiBody(ApiBody apiBody) {
        this.apiBody = apiBody;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public ApiFormDataType getDataType() {
        return dataType == null ? null : ApiFormDataType.fromId(dataType);
    }

    public void setDataType(ApiFormDataType dataType) {
        this.dataType = dataType == null ? null : dataType.getId();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @PostConstruct
    public void initFormDataFieldType() {
        if (dataType == null) {
            setDataType(ApiFormDataType.TEXT);
        }
    }

}