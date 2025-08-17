package com.fitnexus.api_gateway.user.service;

import com.fitnexus.api_gateway.user.service.dto.UserRequest;
import com.fitnexus.api_gateway.user.service.dto.UserResponse;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<Boolean> userValidation (String userId);

    Mono<UserResponse> registerUser(UserRequest userRequest);
}
