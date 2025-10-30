package com.company.dynamicds.apisetting.entity;

import com.company.dynamicds.apisetting.enums.HttpMethodType;
import com.company.dynamicds.entity.BaseEntity;
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@JmixEntity
@Table(name = "DWH_API_SETTING", indexes = {
        @Index(name = "IDX_DWH_API_SETTING_AUTHORIZATION", columnList = "AUTHORIZATION_ID"),
        @Index(name = "IDX_DWH_API_SETTING_API_BODY", columnList = "API_BODY_ID")
})
@Entity(name = "dwh_ApiSetting")
public class ApiSetting extends BaseEntity {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @OnDelete(DeletePolicy.CASCADE)
    @JoinColumn(name = "API_BODY_ID")
    @Composition
    @OneToOne(fetch = FetchType.LAZY)
    private ApiBody apiBody;

    @InstanceName
    @Column(name = "NAME")
    private String name;

    @Column(name = "BASE_URL")
    private String baseUrl;

    @Column(name = "FINAL_URL")
    private String finalUrl;

    @Column(name = "HTTP_METHOD")
    private String httpMethod = HttpMethodType.GET.getId();

    @OnDelete(DeletePolicy.CASCADE)
    @Composition
    @OneToMany(mappedBy = "apiSetting")
    private List<ApiHeader> apiHeader;

    @OnDelete(DeletePolicy.CASCADE)
    @JoinColumn(name = "AUTHORIZATION_ID")
    @Composition
    @OneToOne(fetch = FetchType.LAZY)
    private ApiAuthorizationSetting authorization;

    @OnDelete(DeletePolicy.CASCADE)
    @Composition
    @OneToMany(mappedBy = "apiSetting")
    private List<ApiQueryParam> apiQueryParam;

    @Column(name = "POST_RESPONSE_SCRIPT")
    @Lob
    private String postResponseScript;

    @Column(name = "USE_RAW_URL")
    private Boolean useRawUrl;

    public String getPostResponseScript() {
        return postResponseScript;
    }

    public void setPostResponseScript(String postResponseScript) {
        this.postResponseScript = postResponseScript;
    }

    public ApiBody getApiBody() {
        return apiBody;
    }

    public void setApiBody(ApiBody apiBody) {
        this.apiBody = apiBody;
    }


    public Boolean getUseRawUrl() {
        return useRawUrl;
    }

    public void setUseRawUrl(Boolean useRawUrl) {
        this.useRawUrl = useRawUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<ApiQueryParam> getApiQueryParam() {
        return apiQueryParam;
    }

    public void setApiQueryParam(List<ApiQueryParam> apiQueryParam) {
        this.apiQueryParam = apiQueryParam;
    }

    public ApiAuthorizationSetting getAuthorization() {
        return authorization;
    }

    public void setAuthorization(ApiAuthorizationSetting authorization) {
        this.authorization = authorization;
    }

    public List<ApiHeader> getApiHeader() {
        return apiHeader;
    }

    public void setApiHeader(List<ApiHeader> apiHeader) {
        this.apiHeader = apiHeader;
    }

    public String getFinalUrl() {
        return finalUrl;
    }

    public void setFinalUrl(String finalUrl) {
        this.finalUrl = finalUrl;
    }

    public HttpMethodType getHttpMethod() {
        return httpMethod == null ? null : HttpMethodType.fromId(httpMethod);
    }

    public void setHttpMethod(HttpMethodType httpMethod) {
        this.httpMethod = httpMethod == null ? null : httpMethod.getId();
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