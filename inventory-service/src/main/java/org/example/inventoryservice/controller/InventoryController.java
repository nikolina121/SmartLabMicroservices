package org.example.inventoryservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.inventoryservice.dto.IssueRequest;
import org.example.inventoryservice.dto.ResolveIssueRequest;
import org.example.inventoryservice.dto.StatusRequest;
import org.example.inventoryservice.model.BoardSpecification;
import org.example.inventoryservice.model.Component;
import org.example.inventoryservice.model.EquipmentIssue;
import org.example.inventoryservice.model.InventoryItem;
import org.example.inventoryservice.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/components")
    public List<Component> components() {
        return inventoryService.findComponents();
    }

    @PostMapping("/components")
    public ResponseEntity<Component> addComponent(@RequestBody @Valid Component component) {
        return new ResponseEntity<>(inventoryService.saveComponent(component), HttpStatus.CREATED);
    }

    @GetMapping("/components/{id}/specification")
    public BoardSpecification specification(@PathVariable Long id) {
        return inventoryService.getSpecification(id);
    }

    @PostMapping("/components/{id}/specification")
    public ResponseEntity<BoardSpecification> addSpecification(@PathVariable Long id,
                                                               @RequestBody @Valid BoardSpecification specification) {
        return new ResponseEntity<>(inventoryService.saveSpecification(id, specification), HttpStatus.CREATED);
    }

    @GetMapping("/items")
    public List<InventoryItem> items() {
        return inventoryService.findItems();
    }

    @GetMapping("/items/{id}")
    public InventoryItem item(@PathVariable Long id) {
        return inventoryService.getItem(id);
    }

    @PostMapping("/items")
    public ResponseEntity<InventoryItem> addItem(@RequestBody @Valid InventoryItem item) {
        return new ResponseEntity<>(inventoryService.saveItem(item), HttpStatus.CREATED);
    }

    @PatchMapping("/items/{id}/status")
    public InventoryItem updateStatus(@PathVariable Long id, @RequestBody @Valid StatusRequest request) {
        return inventoryService.updateStatus(id, request.getOperationalStatus());
    }

    @GetMapping("/items/available/{componentId}")
    public List<InventoryItem> available(@PathVariable Long componentId) {
        return inventoryService.findReadyForUse(componentId);
    }

    @GetMapping("/issues")
    public List<EquipmentIssue> issues() {
        return inventoryService.findIssues();
    }

    @PostMapping("/issues")
    public ResponseEntity<EquipmentIssue> reportIssue(
            @RequestBody @Valid IssueRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return new ResponseEntity<>(inventoryService.reportIssue(request, userId), HttpStatus.CREATED);
    }

    @PatchMapping("/issues/{id}/resolve")
    public EquipmentIssue resolve(@PathVariable Long id, @RequestBody ResolveIssueRequest request) {
        return inventoryService.resolveIssue(id, request);
    }
}
