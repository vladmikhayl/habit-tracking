package com.vladmikhayl.auth.service;

import com.vladmikhayl.auth.entity.User;
import com.vladmikhayl.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InternalUserService {

    private final UserRepository userRepository;

    public String getUserLogin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        return user.getUsername();
    }

}
