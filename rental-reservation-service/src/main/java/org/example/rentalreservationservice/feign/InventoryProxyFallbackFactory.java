package org.example.rentalreservationservice.feign;

import lombok.extern.slf4j.Slf4j;
import org.example.rentalreservationservice.exception.BusinessException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class InventoryProxyFallbackFactory implements FallbackFactory<InventoryProxy> {

    @Override
    public InventoryProxy create(Throwable cause) {
        log.error("Poziv ka inventory-service nije uspeo: {}", cause.toString(), cause);
        return new InventoryProxy() {
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
        };
    }
}
