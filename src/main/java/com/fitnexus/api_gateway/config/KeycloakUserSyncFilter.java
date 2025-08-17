package com.fitnexus.api_gateway.config;

import com.fitnexus.api_gateway.user.service.UserService;
import com.fitnexus.api_gateway.user.service.dto.UserRequest;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserSyncFilter implements WebFilter {

    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-USER-ID");
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        UserRequest userDetails = getUserDetails(token);
        if (userId == null) {
            userId = userDetails != null ? userDetails.getKeycloakId() : null;
        }
        if (userId == null || userId.isEmpty()) {
            return chain.filter(exchange);
        }
        String finalUserId = userId;
        return userService.userValidation(finalUserId)
                .flatMap(valid -> {
                    if (!valid) {
                        if (userDetails != null) {
                            return userService.registerUser(userDetails)
                                    .then();
                        } else {
                            return Mono.empty();
                        }
                    } else {
                        log.info("User already exists.");
                        return Mono.empty();
                    }
                })
                .then(Mono.defer(() -> {
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-USER-ID", finalUserId)
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                }));
    }

    private UserRequest getUserDetails(String token) {
        try {
            String bearerToken = token.replace("Bearer ", "").trim();
            SignedJWT jwt = SignedJWT.parse(bearerToken);
            JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();

            return UserRequest.builder()
                    .email(jwtClaimsSet.getStringClaim("email"))
                    .keycloakId(jwtClaimsSet.getStringClaim("sub"))
                    .password("password@123")
                    .firstName(jwtClaimsSet.getStringClaim("given_name"))
                    .lastName(jwtClaimsSet.getStringClaim("family_name"))
                    .build();

        } catch (Exception e) {
            log.error("Exception while getting the user details from the token: {}" , e.getMessage());
            return null;
        }
    }

}
