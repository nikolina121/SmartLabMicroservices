package org.example.authenticationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.authenticationservice.dto.AuthResponse;
import org.example.authenticationservice.dto.UpdateRoleRequest;
import org.example.authenticationservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/admin/users")
public class AdminUserController {

    private final AuthService authService;

    @PutMapping("/{id}/role")
    public ResponseEntity<AuthResponse> updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(authService.updateUserRole(id, request.getRole()));
    }
}
