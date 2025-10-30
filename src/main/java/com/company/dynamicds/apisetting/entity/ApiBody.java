package com.company.dynamicds.apisetting.entity;

import com.company.dynamicds.apisetting.enums.ApiBodyType;
import com.company.dynamicds.apisetting.enums.ApiRawType;
import com.company.dynamicds.entity.BaseEntity;
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "DWH_API_BODY")
@Entity(name = "dwh_ApiBody")
public class ApiBody extends BaseEntity {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Column(name = "BODY_TYPE")
    private String bodyType;

    @Column(name = "RAW_TYPE")
    private String rawType;

    @Column(name = "RAW_CONTENT")
    @Lob
    private String rawContent;

    @Column(name = "BINARY_FILE_NAME")
    private String binaryFileName;

    @Column(name = "BINARY_FILE_CONTENT")
    private byte[] binaryFileContent;

    @OnDelete(DeletePolicy.CASCADE)
    @Composition
    @OneToMany(mappedBy = "apiBody")
    private List<ApiFormDataField> formDataFields;

    @OnDelete(DeletePolicy.CASCADE)
    @Composition
    @OneToMany(mappedBy = "apiBody")
    private List<ApiUrlEncodedField> urlFormEncodedField;

    public List<ApiUrlEncodedField> getUrlFormEncodedField() {
        return urlFormEncodedField;
    }

    public void setUrlFormEncodedField(List<ApiUrlEncodedField> urlFormEncodedField) {
        this.urlFormEncodedField = urlFormEncodedField;
    }

    public String getBinaryFileName() {
        return binaryFileName;
    }

    public void setBinaryFileName(String binaryFileName) {
        this.binaryFileName = binaryFileName;
    }

    public byte[] getBinaryFileContent() {
        return binaryFileContent;
    }

    public void setBinaryFileContent(byte[] binaryFileContent) {
        this.binaryFileContent = binaryFileContent;
    }

    public List<ApiFormDataField> getFormDataFields() {
        return formDataFields;
    }

    public void setFormDataFields(List<ApiFormDataField> formDataFields) {
        this.formDataFields = formDataFields;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public ApiRawType getRawType() {
        return rawType == null ? null : ApiRawType.fromId(rawType);
    }

    public void setRawType(ApiRawType rawType) {
        this.rawType = rawType == null ? null : rawType.getId();
    }

    public ApiBodyType getBodyType() {
        return bodyType == null ? null : ApiBodyType.fromId(bodyType);
    }

    public void setBodyType(ApiBodyType bodyType) {
        this.bodyType = bodyType == null ? null : bodyType.getId();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}