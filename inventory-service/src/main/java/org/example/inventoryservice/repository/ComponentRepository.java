package org.example.inventoryservice.repository;

import org.example.inventoryservice.model.Component;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComponentRepository extends JpaRepository<Component, Long> {
}
