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
@Table(name = "DWH_API_QUERY_PARAM", indexes = {
        @Index(name = "IDX_DWH_API_QUERY_PARAM_API_SETTING", columnList = "API_SETTING_ID")
})
@Entity(name = "dwh_ApiQueryParam")
public class ApiQueryParam extends BaseEntity {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Column(name = "QP_KEY")
    private String key;

    @Column(name = "QP_VALUE")
    private String value;

    @InstanceName
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "IS_ENABLE")
    private Boolean isEnable = Boolean.TRUE;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "API_SETTING_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ApiSetting apiSetting;

    public Boolean getIsEnable() {
        return isEnable;
    }

    public void setIsEnable(Boolean isEnable) {
        this.isEnable = isEnable;
    }

    public ApiSetting getApiSetting() {
        return apiSetting;
    }

    public void setApiSetting(ApiSetting apiSetting) {
        this.apiSetting = apiSetting;
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