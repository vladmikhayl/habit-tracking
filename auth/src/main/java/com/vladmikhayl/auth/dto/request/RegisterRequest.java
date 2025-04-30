package com.vladmikhayl.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Запрос на регистрацию")
public class RegisterRequest {

    @NotBlank(message = "Логин не может быть пустым")
    @Size(min = 3, max = 30, message = "Логин должен содержать от 3 до 30 символов")
    @Schema(
            description = "Логин",
            example = "user",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 5, max = 72, message = "Пароль должен содержать от 5 до 72 символов")
    @Schema(
            description = "Пароль",
            example = "password",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;

}
