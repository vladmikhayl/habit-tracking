package com.vladmikhayl.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmikhayl.auth.dto.request.RegisterRequest;
import com.vladmikhayl.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController underTest;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(underTest).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void canRegisterWithCorrectUsernameAndPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("username")
                .password("password")
                .build();

        doNothing().when(userService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(userService).register(argThat(req -> req.equals(request)));
    }

    @Test
    void failRegisterWithBlankUsername() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("")
                .password("password")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Username cannot be blank"));
                })
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any());
    }

    @Test
    void failRegisterWithTooShortUsername() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("ab")
                .password("password")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Username must contain from 3 to 30 characters"));
                })
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any());
    }

    @Test
    void failRegisterWithTooLongUsername() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("A".repeat(31))
                .password("password")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Username must contain from 3 to 30 characters"));
                })
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any());
    }

    @Test
    void failRegisterWithBlankPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("username")
                .password("")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Password cannot be blank"));
                })
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any());
    }

    @Test
    void failRegisterWithLooShortPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("username")
                .password("abcd")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Password must contain from 5 to 72 characters"));
                })
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any());
    }

    @Test
    void failRegisterWithLooLongPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("username")
                .password("A".repeat(73))
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    Exception exception = result.getResolvedException();
                    assertTrue(exception instanceof MethodArgumentNotValidException);
                    assertTrue(exception.getMessage().contains("Password must contain from 5 to 72 characters"));
                })
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any());
    }

}