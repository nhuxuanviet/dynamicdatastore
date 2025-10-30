package com.company.dynamicds.apisetting.entity;

import com.company.dynamicds.apisetting.enums.ApiAuthType;
import com.company.dynamicds.apisetting.enums.ApiKeyPlacement;
import com.company.dynamicds.entity.BaseEntity;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.UUID;

@JmixEntity
@Table(name = "DWH_API_AUTHORIZATION_SETTING")
@Entity(name = "dwh_ApiAuthorizationSetting")
public class ApiAuthorizationSetting extends BaseEntity {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Column(name = "AUTH_TYPE")
    private String authType = ApiAuthType.NO_AUTH.getId();

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "PASSWORD")
    private String password;

    @Lob
    @Column(name = "BEARER_TOKEN")
    private String bearerToken;

    @Column(name = "API_KEY_NAME")
    private String apiKeyName;

    @Column(name = "API_KEY_VALUE")
    private String apiKeyValue;

    @Column(name = "API_KEY_PLACEMENT")
    private String apiKeyPlacement = ApiKeyPlacement.HEADER.getId();

    public ApiKeyPlacement getApiKeyPlacement() {
        return apiKeyPlacement == null ? null : ApiKeyPlacement.fromId(apiKeyPlacement);
    }

    public void setApiKeyPlacement(ApiKeyPlacement apiKeyPlacement) {
        this.apiKeyPlacement = apiKeyPlacement == null ? null : apiKeyPlacement.getId();
    }

    public String getApiKeyValue() {
        return apiKeyValue;
    }

    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
    }

    public String getApiKeyName() {
        return apiKeyName;
    }

    public void setApiKeyName(String apiKeyName) {
        this.apiKeyName = apiKeyName;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ApiAuthType getAuthType() {
        return authType == null ? null : ApiAuthType.fromId(authType);
    }

    public void setAuthType(ApiAuthType apiAuthType) {
        this.authType = apiAuthType == null ? null : apiAuthType.getId();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}