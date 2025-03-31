package com.vladmikhayl.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос на аутентификацию")
public class LoginRequest {

    @Schema(
            description = "Логин",
            example = "user",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @Schema(
            description = "Пароль",
            example = "password",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;

}
