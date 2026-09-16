package org.example.rentalreservationservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

//@FeignClient(name = "inventory-service", fallback = InventoryProxyFallback.class)
@FeignClient(name = "inventory-service", fallbackFactory = InventoryProxyFallbackFactory.class)
public interface InventoryProxy {

    @GetMapping("/inventory/items/{id}")
    InventoryItemDto getItem(@PathVariable("id") Long id);

    @GetMapping("/inventory/items/available/{componentId}")
    List<InventoryItemDto> getReadyForUse(@PathVariable("componentId") Long componentId);

    @PatchMapping("/inventory/items/{id}/status")
    InventoryItemDto updateStatus(@PathVariable("id") Long id, @RequestBody StatusRequestDto request);

    @PostMapping("/inventory/issues")
    void reportIssue(@RequestBody IssueRequestDto request);
}
