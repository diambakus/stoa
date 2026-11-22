package com.orakuma.stoa.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security.oauth2")
public record TrustedIssuersProperties(List<String> issuers) {}
