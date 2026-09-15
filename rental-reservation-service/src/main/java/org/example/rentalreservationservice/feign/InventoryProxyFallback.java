package org.example.rentalreservationservice.feign;

import org.example.rentalreservationservice.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryProxyFallback implements InventoryProxy {

    @Override
    public InventoryItemDto getItem(Long id) {
        throw unavailable();
    }

    @Override
    public List<InventoryItemDto> getReadyForUse(Long componentId) {
        throw unavailable();
    }

    @Override
    public InventoryItemDto updateStatus(Long id, StatusRequestDto request) {
        throw unavailable();
    }

    @Override
    public void reportIssue(IssueRequestDto request) {
        throw unavailable();
    }

    private BusinessException unavailable() {
        return new BusinessException("Inventory servis trenutno nije dostupan. Pokusajte ponovo.");
    }
}
