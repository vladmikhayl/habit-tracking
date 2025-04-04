package com.vladmikhayl.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Запрос на регистрацию")
public class RegisterRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 30, message = "Username must contain from 3 to 30 characters")
    @Schema(
            description = "Логин",
            example = "user",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 5, max = 72, message = "Password must contain from 5 to 72 characters")
    @Schema(
            description = "Пароль",
            example = "password",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;

}
