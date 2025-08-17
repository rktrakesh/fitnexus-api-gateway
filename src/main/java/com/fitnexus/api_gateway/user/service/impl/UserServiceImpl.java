package com.fitnexus.api_gateway.user.service.impl;

import com.fitnexus.api_gateway.user.service.UserService;
import com.fitnexus.api_gateway.user.service.dto.UserRequest;
import com.fitnexus.api_gateway.user.service.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final WebClient userServiceWebClient;

    public Mono<Boolean> userValidation(String userId) {
        return userServiceWebClient.get()
                .uri("/api/users/{userId}/validate", userId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new RuntimeException("User not found for userId: " + userId));
                    } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        return Mono.error(new RuntimeException("Invalid UserId"));
                    }
                    return Mono.error(e);
                });
    }

    @Override
    public Mono<UserResponse> registerUser(UserRequest userRequest) {
        log.info("Calling user-service with payload: {}", userRequest);

        return userServiceWebClient.post()
                .uri("/api/users/register")
                .bodyValue(userRequest)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("User-service 4xx error: {}", errorBody);
                                    return Mono.error(new ResponseStatusException(
                                            response.statusCode(), errorBody
                                    ));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("User-service 5xx error: {}", errorBody);
                                    return Mono.error(new ResponseStatusException(
                                            response.statusCode(), errorBody
                                    ));
                                })
                )
                .bodyToMono(UserResponse.class);
    }


}
