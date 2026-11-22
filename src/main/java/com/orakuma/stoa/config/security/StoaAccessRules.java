package com.orakuma.stoa.config.security;

import com.orakuma.stoa.user.UserProfile;
import com.orakuma.stoa.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Authorization rules used by {@link SecurityConfig}.
 *
 * <p>The application role (ADMIN, ORGAN_ADMIN, USER, ...) is stoa's own {@link UserProfile#getRole()}, the same
 * value niasse shows; it is not in the Keycloak token, so it is looked up by the token subject. "Self" means the
 * path variable equals the token subject.
 */
@Component
@RequiredArgsConstructor
public class StoaAccessRules {

    public static final String ADMIN = "ADMIN";

    private final UserProfileRepository userProfileRepository;

    /** The caller's stoa profile has exactly this role (ORGAN_ADMIN is not ADMIN). */
    public ReactiveAuthorizationManager<AuthorizationContext> hasRole(String role) {
        return (authentication, context) -> decide(authentication.filter(Authentication::isAuthenticated)
                .flatMap(auth -> hasRole(auth, role)));
    }

    /** The path variable is the caller's own user id. */
    public ReactiveAuthorizationManager<AuthorizationContext> isSelf(String pathVariable) {
        return (authentication, context) -> decide(authentication.filter(Authentication::isAuthenticated)
                .map(auth -> isSelf(auth, context, pathVariable)));
    }

    /** The caller's own user id, or any user id for the given role. */
    public ReactiveAuthorizationManager<AuthorizationContext> isSelfOrHasRole(String pathVariable, String role) {
        return (authentication, context) -> decide(authentication.filter(Authentication::isAuthenticated)
                .flatMap(auth -> isSelf(auth, context, pathVariable) ? Mono.just(true) : hasRole(auth, role)));
    }

    private Mono<Boolean> hasRole(Authentication auth, String role) {
        // JPA is blocking: keep it off the event loop.
        return Mono.fromCallable(() -> userProfileRepository.findById(auth.getName())
                        .map(UserProfile::getRole)
                        .map(String::trim)
                        .filter(role::equalsIgnoreCase)
                        .isPresent())
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static boolean isSelf(Authentication auth, AuthorizationContext context, String pathVariable) {
        Object id = context.getVariables().get(pathVariable);
        return id != null && StringUtils.isNotBlank(auth.getName()) && auth.getName().equals(id.toString());
    }

    private static Mono<AuthorizationDecision> decide(Mono<Boolean> granted) {
        return granted.map(AuthorizationDecision::new).defaultIfEmpty(new AuthorizationDecision(false));
    }
}
