package org.example.inventoryservice.repository;

import org.example.inventoryservice.model.EquipmentIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentIssueRepository extends JpaRepository<EquipmentIssue, Long> {

    List<EquipmentIssue> findByInventoryItemId(Long inventoryItemId);
}
