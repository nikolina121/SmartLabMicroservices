package org.example.authenticationservice.service;

import lombok.RequiredArgsConstructor;
import org.example.authenticationservice.dto.AuthResponse;
import org.example.authenticationservice.dto.LoginRequest;
import org.example.authenticationservice.dto.RegisterRequest;
import org.example.authenticationservice.exception.BusinessException;
import org.example.authenticationservice.model.Role;
import org.example.authenticationservice.model.User;
import org.example.authenticationservice.repository.RoleRepository;
import org.example.authenticationservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()) || userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Korisnicko ime ili e-mail vec postoje.");
        }

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new BusinessException("Uloga STUDENT ne postoji u bazi."));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.getRoles().add(studentRole);
        userRepository.save(user);
        return toResponse(user, "STUDENT");
    }

    @Transactional
    public AuthResponse updateUserRole(Long userId, String requestedRole) {
        String roleName = validateExplicitRole(requestedRole);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Korisnik ne postoji: " + userId));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessException("Uloga ne postoji u bazi: " + roleName));

        user.getRoles().clear();
        user.getRoles().add(role);
        userRepository.save(user);
        return toResponse(user, roleName);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("Pogresno korisnicko ime ili lozinka."));
        if (!Boolean.TRUE.equals(user.getEnabled()) || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Pogresno korisnicko ime ili lozinka.");
        }
        String role = user.getRoles().stream().map(Role::getName).findFirst().orElse("STUDENT");
        return toResponse(user, role);
    }

    private String validateExplicitRole(String role) {
        if (role == null || role.isBlank()) {
            throw new BusinessException("Rola je obavezna.");
        }
        String value = role.trim().toUpperCase();
        if ("USER".equals(value)) {
            value = "STUDENT";
        }
        if (!value.matches("STUDENT|ENGINEER|ADMIN")) {
            throw new BusinessException("Dozvoljene uloge su STUDENT, ENGINEER i ADMIN.");
        }
        return value;
    }

    private AuthResponse toResponse(User user, String role) {
        return new AuthResponse(jwtService.create(user), user.getId(), user.getUsername(), role);
    }
}
