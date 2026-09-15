package org.example.inventoryservice.repository;

import org.example.inventoryservice.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByComponentIdAndOperationalStatus(Long componentId, String operationalStatus);
}
