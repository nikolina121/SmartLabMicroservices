package org.example.inventoryservice.service;

import lombok.RequiredArgsConstructor;
import org.example.inventoryservice.dto.IssueRequest;
import org.example.inventoryservice.dto.ResolveIssueRequest;
import org.example.inventoryservice.exception.BusinessException;
import org.example.inventoryservice.model.BoardSpecification;
import org.example.inventoryservice.model.Component;
import org.example.inventoryservice.model.EquipmentIssue;
import org.example.inventoryservice.model.InventoryItem;
import org.example.inventoryservice.repository.BoardSpecificationRepository;
import org.example.inventoryservice.repository.ComponentRepository;
import org.example.inventoryservice.repository.EquipmentIssueRepository;
import org.example.inventoryservice.repository.InventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final Set<String> ITEM_STATUSES = Set.of(
            "READY_FOR_USE", "IN_USE", "PENDING_DIAGNOSTICS", "IN_REPAIR", "DECOMMISSIONED");
    private static final Set<String> ISSUE_CATEGORIES = Set.of(
            "PIN_DAMAGE", "FIRMWARE_CORRUPTION", "POWER_FAULT", "PHYSICAL_DAMAGE", "MISSING_PART");

    private final ComponentRepository componentRepository;
    private final BoardSpecificationRepository specificationRepository;
    private final InventoryItemRepository itemRepository;
    private final EquipmentIssueRepository issueRepository;

    public List<Component> findComponents() {
        return componentRepository.findAll();
    }

    public Component saveComponent(Component component) {
        return componentRepository.save(component);
    }
    public BoardSpecification saveSpecification(Long componentId, BoardSpecification incoming) {
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new NoSuchElementException("Komponenta ne postoji."));

        return specificationRepository.findById(componentId)
                .map(existing -> copySpecificationFields(incoming, existing))
                .orElseGet(() -> {
                    // Nova specifikacija: NE postavljati componentId ručno - @MapsId ga
                    // izvodi iz ove asocijacije pri persist()-u (videti napomenu u modelu).
                    incoming.setComponent(component);
                    return specificationRepository.save(incoming);
                });
    }

    /** Prepisuje sadržaj specifikacije na već postojeći (managed) entitet i čuva ga - pravi UPDATE. */
    private BoardSpecification copySpecificationFields(BoardSpecification from, BoardSpecification to) {
        to.setCpuArchitecture(from.getCpuArchitecture());
        to.setClockSpeedMhz(from.getClockSpeedMhz());
        to.setFlashMemoryKb(from.getFlashMemoryKb());
        to.setSramKb(from.getSramKb());
        to.setOperatingVoltage(from.getOperatingVoltage());
        to.setDigitalPinsCount(from.getDigitalPinsCount());
        to.setAnalogPinsCount(from.getAnalogPinsCount());
        to.setTimerCount(from.getTimerCount());
        to.setPwmChannelsCount(from.getPwmChannelsCount());
        to.setSpiCount(from.getSpiCount());
        to.setI2cCount(from.getI2cCount());
        to.setUartCount(from.getUartCount());
        to.setHasWifi(from.getHasWifi());
        to.setHasBluetooth(from.getHasBluetooth());
        return specificationRepository.save(to);
    }

    public boolean specificationExists(Long componentId) {
        return specificationRepository.existsById(componentId);
    }
//    public BoardSpecification saveSpecification(Long componentId, BoardSpecification specification) {
//        Component component = componentRepository.findById(componentId)
//                .orElseThrow(() -> new NoSuchElementException("Komponenta ne postoji."));
//        specification.setComponent(component);
//        return specificationRepository.save(specification);
//    }

    public BoardSpecification getSpecification(Long componentId) {
        return specificationRepository.findById(componentId)
                .orElseThrow(() -> new NoSuchElementException("Specifikacija ne postoji."));
    }

    public List<InventoryItem> findItems() {
        return itemRepository.findAll();
    }

    public InventoryItem getItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Primerak ne postoji."));
    }

    public InventoryItem saveItem(InventoryItem item) {
        if (item.getOperationalStatus() == null) {
            item.setOperationalStatus("READY_FOR_USE");
        }
        validateStatus(item.getOperationalStatus());
        componentRepository.findById(item.getComponentId())
                .orElseThrow(() -> new NoSuchElementException("Komponenta ne postoji."));
        return itemRepository.save(item);
    }

    public List<InventoryItem> findReadyForUse(Long componentId) {
        return itemRepository.findByComponentIdAndOperationalStatus(componentId, "READY_FOR_USE");
    }

    @Transactional
    public InventoryItem updateStatus(Long id, String status) {
        validateStatus(status);
        InventoryItem item = getItem(id);
        item.setOperationalStatus(status);
        item.setLastInspectedAt(LocalDateTime.now());
        return itemRepository.save(item);
    }

    @Transactional
    public EquipmentIssue reportIssue(IssueRequest request, Long reportedByUserId) {
        if (request.getIssueCategory() == null || !ISSUE_CATEGORIES.contains(request.getIssueCategory())) {
            throw new BusinessException("Nepoznata kategorija kvara.");
        }
        InventoryItem item = getItem(request.getInventoryItemId());
        EquipmentIssue issue = new EquipmentIssue();
        issue.setInventoryItemId(item.getId());
        issue.setReportedByUserId(reportedByUserId);
        issue.setIssueCategory(request.getIssueCategory());
        String description = request.getDescription() == null || request.getDescription().isBlank()
                ? "Prijava kvara bez dodatnog opisa."
                : request.getDescription();
        issue.setDescription(description);
        issue.setStatus("OPEN");
        issueRepository.save(issue);
        item.setOperationalStatus("PENDING_DIAGNOSTICS");
        itemRepository.save(item);
        return issue;
    }

    @Transactional
    public EquipmentIssue resolveIssue(Long id, ResolveIssueRequest request) {
        EquipmentIssue issue = issueRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Prijava kvara ne postoji."));
        issue.setStatus("RESOLVED");
        issue.setResolutionNotes(request.getNotes());
        issue.setResolvedAt(LocalDateTime.now());
        String nextStatus = request.getItemStatusAfterRepair() == null
                ? "READY_FOR_USE"
                : request.getItemStatusAfterRepair();
        updateStatus(issue.getInventoryItemId(), nextStatus);
        return issueRepository.save(issue);
    }

    public List<EquipmentIssue> findIssues() {
        return issueRepository.findAll();
    }

    private void validateStatus(String status) {
        if (status == null || !ITEM_STATUSES.contains(status)) {
            throw new BusinessException("Nepoznato operativno stanje uredjaja.");
        }
    }
}
