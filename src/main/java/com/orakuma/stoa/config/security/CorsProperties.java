package com.orakuma.stoa.config.security;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.cors")
@Setter
@Getter
@Generated
public class CorsProperties {
    private List<String> allowedOrigins;
    private List<String> allowedMethods;
    private List<String> allowedHeaders;
    private Long maxAge;
    private Boolean allowCredentials;
}
