package com.vladmikhayl.e2e.scenario;

import com.vladmikhayl.e2e.BaseE2ETest;
import org.junit.jupiter.api.Test;

public class CreateHabitFlowTest extends BaseE2ETest {

    @Test
    void canCreateHabit() {
        authHelper.register("user1", "12345");
        String token = authHelper.login("user1", "12345").getToken();

        System.out.println(token);
    }

}
