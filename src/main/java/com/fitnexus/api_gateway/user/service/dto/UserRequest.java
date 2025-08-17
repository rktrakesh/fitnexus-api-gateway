package com.fitnexus.api_gateway.user.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data@Builder
public class UserRequest {

    @Email(message = "Invalid Email format")
    @NotBlank(message = "Email must not be empty, provide email details.")
    private String email;

    private String keycloakId;

    @NotBlank(message = "Password field must not be empty")
    @Size(min = 6, message = "Password must be more than 6 characters.")
    private String password;

    @NotBlank(message = "First Name must not be empty")
    private String firstName;

    private String lastName;

}
