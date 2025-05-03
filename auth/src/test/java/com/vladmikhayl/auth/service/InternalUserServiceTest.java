package com.vladmikhayl.auth.service;

import com.vladmikhayl.auth.entity.User;
import com.vladmikhayl.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InternalUserService underTest;

    @Test
    void canGetUserLoginWhenUserExists() {
        Long userId = 10L;

        User existingUser = User.builder()
                .id(userId)
                .username("user10")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        String userLogin = underTest.getUserLogin(userId);

        assertThat(userLogin).isEqualTo("user10");
    }

    @Test
    void failGetUserLoginWhenUserDoesNotExist() {
        Long userId = 10L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getUserLogin(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Пользователь не найден");
    }

}