package com.orakuma.stoa.auth;

//import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
//import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class AuthenticationController {

    /*@GetMapping(value = "/token")
    public Mono<String> getHome(@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient) {
        String freshToken = authorizedClient.getAccessToken().getTokenValue();
        return Mono.just(freshToken);
    }*/
}
