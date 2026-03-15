package com.orakuma.stoa.config.security;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@AllArgsConstructor
@Slf4j
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final KeycloakAdminProperties adminProperties;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        String clientId = jwt.getClaimAsString("azp");
        if (!StringUtils.equals(adminProperties.getClientId(), clientId)) {
            throw new JwtException("Token not issued for this client.");
        }

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

        if (resourceAccess != null) {
            Map<String, Object> client = (Map<String, Object>) resourceAccess.get(adminProperties.getClientId());

            if (client != null) {
                Collection<String> roles = (Collection<String>) client.get("roles");

                if (roles != null) {
                    roles.forEach(role ->
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                }
            }
        }

        return new JwtAuthenticationToken(jwt, authorities);
    }
}
