package com.orakuma.stoa.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.authentication.ReactiveAuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerReactiveAuthenticationManagerResolver;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;


import static com.orakuma.stoa.config.security.StoaAccessRules.ADMIN;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * All of stoa's authorization lives here, as URL rules: reactive method security ({@code @PreAuthorize}) only
 * works on methods returning a Publisher, which these controllers do not.
 *
 * <p>The realm is shared by niasse (employees, admins) and samay (citizens), so "authenticated" alone is never
 * enough for user management: unit ids written here end up in the niasse token and grant access in rogator.
 */
@Configuration
@EnableWebFluxSecurity
@Profile("!noauth")
@EnableConfigurationProperties(TrustedIssuersProperties.class)
@Slf4j
public class SecurityConfig {

    private static final String USER_PROFILE = "/stoa/v1/user-profile";
    private static final String USER_PROVISIONING = "/stoa/v1/user-provisioning";

    /**
     * Accepts tokens from any of the configured realms (e.g. samay's and niasse's), picking the JWT decoder by the
     * token's {@code iss} claim; tokens from any other issuer are rejected with 401. Decoders (and their JWKS) are
     * created lazily on the first token of each issuer.
     */
    @Bean
    public ReactiveAuthenticationManagerResolver<ServerWebExchange> authenticationManagerResolver(
            TrustedIssuersProperties issuersProperties) {
        return JwtIssuerReactiveAuthenticationManagerResolver.fromTrustedIssuers(issuersProperties.issuers());
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http, StoaAccessRules rules,
            ReactiveAuthenticationManagerResolver<ServerWebExchange> authenticationManagerResolver) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Own profile (niasse and samay load it after login); admins may read anyone's.
                        .pathMatchers(HttpMethod.GET, USER_PROFILE + "/{userId}")
                        .access(rules.isSelfOrHasRole("userId", ADMIN))
                        // Creating profiles, changing roles and units: admins only.
                        .pathMatchers(HttpMethod.POST, USER_PROFILE + "/create").access(rules.hasRole(ADMIN))
                        .pathMatchers(HttpMethod.PATCH, USER_PROFILE, USER_PROFILE + "/{userId}")
                        .access(rules.hasRole(ADMIN))

                        .pathMatchers(HttpMethod.POST, USER_PROVISIONING + "/create").access(rules.hasRole(ADMIN))
                        // Only your own password (the endpoint also checks the current one).
                        .pathMatchers(HttpMethod.PATCH, USER_PROVISIONING + "/update-password/{userId}")
                        .access(rules.isSelf("userId"))
                        // Anything else under these prefixes (new endpoints) is denied until it gets a rule.
                        .pathMatchers(USER_PROFILE + "/**", USER_PROVISIONING + "/**").denyAll()

                        // Actuator exposes env/heapdump (secrets): health for any user, the rest for admins.
                        .pathMatchers("/actuator/health", "/actuator/health/**").authenticated()
                        .pathMatchers("/actuator/**").access(rules.hasRole(ADMIN))

                        // Gateway routes (/rogator/**, /servitus/**) and /token: the services behind check access.
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(authenticationManagerResolver))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(withDefaults())
                .build();
    }
}