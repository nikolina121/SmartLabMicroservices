package org.example.authenticationservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authenticationservice.model.Role;
import org.example.authenticationservice.repository.RoleRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RoleDataInitializer {

    @Bean
    ApplicationRunner seedRoles(RoleRepository roleRepository) {
        return args -> {
            seed(roleRepository, "STUDENT", "Student laboratorije");
            seed(roleRepository, "ENGINEER", "Laboratorijski inzenjer");
            seed(roleRepository, "ADMIN", "Administrator sistema");
        };
    }

    private void seed(RoleRepository roleRepository, String name, String description) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            roleRepository.save(role);
            log.info("Uloga {} je ubacena u bazu.", name);
        }
    }
}
