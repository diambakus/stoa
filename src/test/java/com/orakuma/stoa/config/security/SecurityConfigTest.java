package com.orakuma.stoa.config.security;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.PlainJWT;
import com.orakuma.stoa.user.UserProfile;
import com.orakuma.stoa.user.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.authentication.ReactiveAuthenticationManagerResolver;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerReactiveAuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Runs the real filter chain. Tokens are unsigned JWTs whose {@code sub} is the user id and whose {@code iss} is one
 * of the trusted realms; the issuer resolver works exactly as in production, only the per-issuer decoder is stubbed
 * (no Keycloak / JWKS lookup).
 */
@SpringJUnitConfig(SecurityConfigTest.TestConfig.class)
@TestPropertySource(properties = {
        "app.security.oauth2.issuers[0]=" + SecurityConfigTest.SAMAY_ISSUER,
        "app.security.oauth2.issuers[1]=" + SecurityConfigTest.NIASSE_ISSUER})
class SecurityConfigTest {

    static final String SAMAY_ISSUER = "https://keycloak.test/realms/kumpo";
    static final String NIASSE_ISSUER = "https://keycloak.test/realms/kumpo-niasse";

    @Configuration
    @EnableWebFlux
    @Import({SecurityConfig.class, StoaAccessRules.class, StubController.class})
    static class TestConfig {
        @Bean
        UserProfileRepository userProfileRepository() {
            return mock(UserProfileRepository.class);
        }

        /** Replaces {@link SecurityConfig#authenticationManagerResolver}, whose decoders would call Keycloak. */
        @Bean
        @Primary
        ReactiveAuthenticationManagerResolver<ServerWebExchange> testAuthenticationManagerResolver(
                TrustedIssuersProperties issuersProperties) {
            var manager = new JwtReactiveAuthenticationManager(stubDecoder());
            return new JwtIssuerReactiveAuthenticationManagerResolver(issuer ->
                    issuersProperties.issuers().contains(issuer) ? Mono.just(manager) : Mono.empty());
        }

        /** Accepts any well-formed JWT, trusting its claims (the signature is what the stub skips). */
        private static ReactiveJwtDecoder stubDecoder() {
            return token -> {
                try {
                    JWTClaimsSet claims = JWTParser.parse(token).getJWTClaimsSet();
                    return Mono.just(Jwt.withTokenValue(token).header("alg", "none")
                            .issuer(claims.getIssuer()).subject(claims.getSubject())
                            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build());
                } catch (ParseException e) {
                    return Mono.error(new BadJwtException("bad token", e));
                }
            };
        }
    }

    @RestController
    static class StubController {
        @RequestMapping({"/stoa/v1/user-profile/**", "/stoa/v1/user-profile",
                "/stoa/v1/user-provisioning/**", "/actuator/**", "/rogator/**"})
        Mono<String> ok() {
            return Mono.just("ok");
        }
    }

    @Autowired
    ApplicationContext context;
    @Autowired
    UserProfileRepository repository;
    WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToApplicationContext(context).build();
        Map.of("admin", "ADMIN", "organ", "ORGAN_ADMIN", "emp", "USER").forEach((id, role) ->
                when(repository.findById(id)).thenReturn(Optional.of(new UserProfile().setId(id).setRole(role))));
        when(repository.findById("citizen")).thenReturn(Optional.empty());
    }

    private static String token(String issuer, String user) {
        return new PlainJWT(new JWTClaimsSet.Builder().issuer(issuer).subject(user)
                .issueTime(new Date()).build()).serialize();
    }

    private WebTestClient.ResponseSpec call(HttpMethod method, String path, String user) {
        return call(method, path, user, NIASSE_ISSUER);
    }

    private WebTestClient.ResponseSpec call(HttpMethod method, String path, String user, String issuer) {
        var request = client.method(method).uri(path);
        if (user != null) {
            request.headers(h -> h.setBearerAuth(token(issuer, user)));
        }
        return request.exchange();
    }

    private void allowed(HttpMethod method, String path, String user) {
        call(method, path, user).expectStatus().isOk();
    }

    private void forbidden(HttpMethod method, String path, String user) {
        call(method, path, user).expectStatus().isForbidden();
    }

    @Test
    void anonymousIsRejected() {
        call(HttpMethod.GET, "/stoa/v1/user-profile/emp", null).expectStatus().isUnauthorized();
        call(HttpMethod.GET, "/rogator/applications", null).expectStatus().isUnauthorized();
    }

    @Test
    void tokensFromEveryTrustedIssuerAreAccepted() {
        call(HttpMethod.GET, "/rogator/applications", "citizen", SAMAY_ISSUER).expectStatus().isOk();
        call(HttpMethod.GET, "/rogator/applications", "emp", NIASSE_ISSUER).expectStatus().isOk();
    }

    @Test
    void tokensFromOtherIssuersOrMalformedAreRejected() {
        call(HttpMethod.GET, "/rogator/applications", "emp", "https://evil.test/realms/kumpo")
                .expectStatus().isUnauthorized();
        // Rules are evaluated only after authentication: an admin id from an untrusted realm gets nothing.
        call(HttpMethod.GET, "/actuator/env", "admin", "https://evil.test/realms/kumpo")
                .expectStatus().isUnauthorized();
        client.get().uri("/rogator/applications").headers(h -> h.setBearerAuth("not-a-jwt"))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void ownProfileOrAdmin() {
        allowed(HttpMethod.GET, "/stoa/v1/user-profile/emp", "emp");
        allowed(HttpMethod.GET, "/stoa/v1/user-profile/citizen", "citizen");
        allowed(HttpMethod.GET, "/stoa/v1/user-profile/emp", "admin");
        forbidden(HttpMethod.GET, "/stoa/v1/user-profile/admin", "emp");
        forbidden(HttpMethod.GET, "/stoa/v1/user-profile/emp", "organ");
    }

    @Test
    void userManagementIsAdminOnly() {
        for (String user : new String[]{"emp", "citizen", "organ"}) {
            forbidden(HttpMethod.POST, "/stoa/v1/user-provisioning/create", user);
            forbidden(HttpMethod.POST, "/stoa/v1/user-profile/create", user);
            forbidden(HttpMethod.PATCH, "/stoa/v1/user-profile/" + user, user); // own units
            forbidden(HttpMethod.PATCH, "/stoa/v1/user-profile?userId=" + user, user); // own role
        }
        allowed(HttpMethod.POST, "/stoa/v1/user-provisioning/create", "admin");
        allowed(HttpMethod.POST, "/stoa/v1/user-profile/create", "admin");
        allowed(HttpMethod.PATCH, "/stoa/v1/user-profile/emp", "admin");
        allowed(HttpMethod.PATCH, "/stoa/v1/user-profile?userId=emp", "admin");
    }

    @Test
    void onlyYourOwnPassword() {
        allowed(HttpMethod.PATCH, "/stoa/v1/user-provisioning/update-password/emp", "emp");
        forbidden(HttpMethod.PATCH, "/stoa/v1/user-provisioning/update-password/emp", "citizen");
        forbidden(HttpMethod.PATCH, "/stoa/v1/user-provisioning/update-password/emp", "admin");
    }

    @Test
    void unlistedUserEndpointsAreDenied() {
        forbidden(HttpMethod.DELETE, "/stoa/v1/user-profile/emp", "admin");
        forbidden(HttpMethod.GET, "/stoa/v1/user-provisioning/anything", "admin");
    }

    @Test
    void actuatorBeyondHealthIsAdminOnly() {
        allowed(HttpMethod.GET, "/actuator/health", "citizen");
        forbidden(HttpMethod.GET, "/actuator/env", "citizen");
        forbidden(HttpMethod.GET, "/actuator/heapdump", "organ");
        allowed(HttpMethod.GET, "/actuator/env", "admin");
    }

    @Test
    void gatewayRoutesNeedOnlyAuthentication() {
        allowed(HttpMethod.GET, "/rogator/applications", "citizen");
    }
}
