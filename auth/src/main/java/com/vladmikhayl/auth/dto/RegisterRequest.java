package com.vladmikhayl.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 30, message = "Username must contain from 3 to 30 characters")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 5, max = 72, message = "Password must contain from 5 to 72 characters")
    private String password;

}
