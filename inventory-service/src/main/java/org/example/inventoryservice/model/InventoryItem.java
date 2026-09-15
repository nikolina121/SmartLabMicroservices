package org.example.inventoryservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "itemTag je obavezan")
    @Column(name = "item_tag", nullable = false, unique = true, length = 50)
    private String itemTag;

    @NotNull(message = "componentId je obavezan")
    @Column(name = "component_id", nullable = false)
    private Long componentId;

    @Column(name = "firmware_version", length = 30)
    private String firmwareVersion = "v1.0.0";

    @NotBlank(message = "storageLocation je obavezan")
    @Column(name = "storage_location", nullable = false, length = 100)
    private String storageLocation;

    @Column(name = "operational_status", nullable = false, length = 30)
    private String operationalStatus = "READY_FOR_USE";

    @Column(name = "condition_rating", nullable = false)
    private Integer conditionRating = 5;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "last_inspected_at")
    private LocalDateTime lastInspectedAt;
}
