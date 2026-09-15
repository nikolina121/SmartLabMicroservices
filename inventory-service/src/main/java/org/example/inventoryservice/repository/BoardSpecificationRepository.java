package org.example.inventoryservice.repository;

import org.example.inventoryservice.model.BoardSpecification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardSpecificationRepository extends JpaRepository<BoardSpecification, Long> {
}
