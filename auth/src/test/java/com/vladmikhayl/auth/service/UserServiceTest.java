package com.vladmikhayl.auth.service;

import com.vladmikhayl.auth.dto.request.LoginRequest;
import com.vladmikhayl.auth.dto.request.RegisterRequest;
import com.vladmikhayl.auth.dto.response.AuthResponse;
import com.vladmikhayl.auth.entity.User;
import com.vladmikhayl.auth.repository.UserRepository;
import com.vladmikhayl.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

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
                .hasMessage("Этот логин уже занят");

        verify(userRepository, never()).save(any());
    }

    @Test
    void canLoginWithCorrectCredentials() {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("user")
                .password("password")
                .build();

        User existingUser = User.builder()
                .username("user")
                .passwordHash("hashedPassword")
                .id(42L)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));

        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(existingUser));

        when(jwtTokenProvider.generateToken("user", 42L)).thenReturn("mocked-jwt-token");

        AuthResponse authResponse = underTest.login(loginRequest);

        assertThat(authResponse.getToken()).isEqualTo("mocked-jwt-token");
    }

    @Test
    void failLoginWithWrongCredentials() {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("user")
                .password("wrong_password")
                .build();

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> underTest.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Неверный логин или пароль");
    }

}