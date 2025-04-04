package com.vladmikhayl.auth.service;

import com.vladmikhayl.auth.dto.RegisterRequest;
import com.vladmikhayl.auth.entity.User;
import com.vladmikhayl.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService underTest;

    @Test
    void canRegisterWithUniqueUsername() {
        RegisterRequest request = RegisterRequest.builder()
                .username("username")
                .password("password")
                .build();

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);

        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword");

        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

        underTest.register(request);

        verify(userRepository).save(userArgumentCaptor.capture());

        User capturedUser = userArgumentCaptor.getValue();

        assertThat(capturedUser.getUsername()).isEqualTo("username");
        assertThat(capturedUser.getPassword()).isEqualTo("hashedPassword");
    }

    @Test
    void failRegisterWhenThatUsernameIsTaken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("username")
                .password("password")
                .build();

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

        assertThatThrownBy(() -> underTest.register(request))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("Username already exists");

        verify(userRepository, never()).save(any());
    }

}