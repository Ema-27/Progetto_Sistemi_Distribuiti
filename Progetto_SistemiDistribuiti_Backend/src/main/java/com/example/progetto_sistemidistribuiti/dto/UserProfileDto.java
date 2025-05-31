package com.example.progetto_sistemidistribuiti.dto;

import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileDto {
    @NotBlank
    private String username;

    @NotBlank
    private String fullName;

    @NotBlank
    private String temporaryPassword;

    @Email
    private String email;

    @Version
    private Long version;
}

