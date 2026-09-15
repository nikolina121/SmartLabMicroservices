package org.example.inventoryservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveIssueRequest {
    private String notes;
    private String itemStatusAfterRepair;
}
