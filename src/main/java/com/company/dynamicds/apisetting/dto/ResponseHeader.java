package com.company.dynamicds.apisetting.dto;

import io.jmix.core.metamodel.annotation.JmixEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JmixEntity(name = "dwh_ResponseHeader")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseHeader {
    private String key;
    private String value;
}