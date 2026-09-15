package org.example.rentalreservationservice.controller;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.rentalreservationservice.dto.*;
import org.example.rentalreservationservice.model.*;
import org.example.rentalreservationservice.service.RentalReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/rental")
@RequiredArgsConstructor
public class RentalReservationController {

    private final RentalReservationService rentalService;

    @PostMapping("/members")
    public ResponseEntity<LabMember> createMember(
            @RequestHeader("X-User-Id") Long externalUserId,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestBody @Valid CreateMemberRequest dto) {
        return new ResponseEntity<>(rentalService.createMember(externalUserId, roleHeader, dto), HttpStatus.CREATED);
    }

    @GetMapping("/members/me/notifications")
    public ResponseEntity<List<Notification>> getMyNotifications(@RequestHeader("X-User-Id") Long externalUserId) {
        LabMember member = rentalService.requireMemberByExternalUserId(externalUserId);
        return ResponseEntity.ok(rentalService.getNotificationsForMember(member.getId()));
    }

    @PostMapping("/requests")
    public ResponseEntity<ReservationRequest> createRequest(
            @RequestHeader("X-User-Id") Long externalUserId,
            @RequestBody @Valid CreateRequestDto dto) {
        LabMember member = rentalService.requireMemberByExternalUserId(externalUserId);
        return new ResponseEntity<>(rentalService.createReservationRequest(member.getId(), dto), HttpStatus.CREATED);
    }

    @GetMapping("/requests/mine")
    public ResponseEntity<List<ReservationRequest>> getMyRequests(@RequestHeader("X-User-Id") Long externalUserId) {
        LabMember member = rentalService.requireMemberByExternalUserId(externalUserId);
        return ResponseEntity.ok(rentalService.getRequestsForMember(member.getId()));
    }

    @PutMapping("/requests/{requestId}/cancel")
    public ResponseEntity<ReservationRequest> cancelRequest(
            @PathVariable Long requestId, @RequestBody(required = false) CancelRequestDto dto) {
        return ResponseEntity.ok(rentalService.cancelRequest(requestId, dto != null ? dto : new CancelRequestDto()));
    }

    @PutMapping("/requests/{requestId}/approve")
    public ResponseEntity<Reservation> approveRequest(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") Long engineerUserId,
            @RequestBody(required = false) ApproveRequestDto dto) {
        ApproveRequestDto body = dto != null ? dto : new ApproveRequestDto();
        if (body.getApprovedByUserId() == null) {
            body.setApprovedByUserId(engineerUserId);
        }
        return ResponseEntity.ok(rentalService.approveRequest(requestId, body));
    }

    @PutMapping("/requests/{requestId}/reject")
    public ResponseEntity<ReservationRequest> rejectRequest(
            @PathVariable Long requestId, @RequestBody @Valid RejectRequestDto dto) {
        return ResponseEntity.ok(rentalService.rejectRequest(requestId, dto));
    }

    @PutMapping("/reservations/{reservationId}/pickup")
    public ResponseEntity<BorrowRecord> pickupEquipment(
            @PathVariable Long reservationId,
            @RequestHeader("X-User-Id") Long engineerUserId,
            @RequestBody(required = false) PickupRequest dto) {
        PickupRequest body = dto != null ? dto : new PickupRequest();
        if (body.getIssuedByUserId() == null) {
            body.setIssuedByUserId(engineerUserId);
        }
        return ResponseEntity.ok(rentalService.pickupEquipment(reservationId, body));
    }

    @PutMapping("/reservations/{reservationId}/return")
    public ResponseEntity<BorrowRecord> returnEquipment(
            @PathVariable Long reservationId,
            @RequestHeader("X-User-Id") Long engineerUserId,
            @RequestBody ReturnRequest dto) {
        if (dto.getReceivedByUserId() == null) {
            dto.setReceivedByUserId(engineerUserId);
        }
        return ResponseEntity.ok(rentalService.returnEquipment(reservationId, dto));
    }

    @PutMapping("/reservations/{reservationId}/cancel")
    public ResponseEntity<Reservation> cancelReservation(
            @PathVariable Long reservationId, @RequestBody(required = false) CancelRequestDto dto) {
        return ResponseEntity.ok(rentalService.cancelReservation(reservationId, dto != null ? dto : new CancelRequestDto()));
    }

    @PutMapping("/penalties/{penaltyId}/pay")
    public ResponseEntity<Penalty> payPenalty(
            @PathVariable Long penaltyId, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(rentalService.payPenalty(penaltyId, userId));
    }

    @PostMapping("/admin/process-overdues")
    public ResponseEntity<Void> processOverdues() {
        rentalService.processOverdueBorrows();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/process-no-shows")
    public ResponseEntity<Void> processNoShows() {
        rentalService.expireNoShowReservations();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reports/active-borrows")
    @RateLimiter(name = "reports", fallbackMethod = "reportsFallback")
    public ResponseEntity<List<BorrowRecord>> getActiveBorrows() {
        return ResponseEntity.ok(rentalService.getActiveBorrows());
    }

    @GetMapping("/reports/overdue-borrows")
    @RateLimiter(name = "reports", fallbackMethod = "reportsFallback")
    public ResponseEntity<List<BorrowRecord>> getOverdueBorrows() {
        return ResponseEntity.ok(rentalService.getOverdueBorrows());
    }

    @GetMapping("/reports/members-in-debt")
    @RateLimiter(name = "reports", fallbackMethod = "penaltyReportsFallback")
    public ResponseEntity<List<Penalty>> getMembersInDebt() {
        return ResponseEntity.ok(rentalService.getMembersInDebt());
    }

    @GetMapping("/reports/most-requested")
    @RateLimiter(name = "reports", fallbackMethod = "demandReportsFallback")
    public ResponseEntity<List<ComponentDemandDto>> getMostRequested(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(rentalService.getMostRequestedComponents(limit));
    }

    public ResponseEntity<List<BorrowRecord>> reportsFallback(Exception exception) {
        return new ResponseEntity<>(Collections.emptyList(), HttpStatus.TOO_MANY_REQUESTS);
    }

    public ResponseEntity<List<Penalty>> penaltyReportsFallback(Exception exception) {
        return new ResponseEntity<>(Collections.emptyList(), HttpStatus.TOO_MANY_REQUESTS);
    }

    public ResponseEntity<List<ComponentDemandDto>> demandReportsFallback(int limit, Exception exception) {
        return new ResponseEntity<>(Collections.emptyList(), HttpStatus.TOO_MANY_REQUESTS);
    }
}
