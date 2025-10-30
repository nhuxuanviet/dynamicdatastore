package com.company.dynamicds.apisetting.entity;

import com.company.dynamicds.entity.BaseEntity;
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.UUID;

@JmixEntity
@Table(name = "DWH_API_URL_ENCODED_FIELD", indexes = {
        @Index(name = "IDX_DWH_API_URL_ENCODED_FIELD_API_BODY", columnList = "API_BODY_ID")
})
@Entity(name = "dwh_ApiUrlEncodedField")
public class ApiUrlEncodedField extends BaseEntity {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Column(name = "KEY_")
    private String key;

    @Column(name = "VALUE_")
    private String value;

    @InstanceName
    @Column(name = "DESCRIPTION")
    private String description;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "API_BODY_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ApiBody apiBody;

    public ApiBody getApiBody() {
        return apiBody;
    }

    public void setApiBody(ApiBody apiBody) {
        this.apiBody = apiBody;
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

}