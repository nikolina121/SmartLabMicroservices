package org.example.inventoryservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssueRequest {
    private Long inventoryItemId;
//    private Long reportedByUserId;
    private String issueCategory;
    private String description;
}
