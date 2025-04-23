package com.vladmikhayl.e2e.scenario;

import com.vladmikhayl.e2e.BaseE2ETest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AuthFlowTests extends BaseE2ETest {

    @Test
    void testTryingToDoRequestWithWrongToken() {
        assertThatThrownBy(() -> habitHelper.getAllUserHabitsAtDay("wrong-token", TODAY_DATE_STR))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    void testTryingToLoginWithWrongPassword() {
        // Создается юзер 1 с рандомным логином
        String userLogin1 = "user_" + UUID.randomUUID().toString().substring(0, 20);
        authHelper.register(userLogin1, "12345");
        assertThatThrownBy(() -> authHelper.login(userLogin1, "wrong-password"))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

}
