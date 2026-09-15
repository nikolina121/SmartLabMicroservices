package org.example.rentalreservationservice.service;

import lombok.RequiredArgsConstructor;
import org.example.rentalreservationservice.config.SmartLabProperties;
import org.example.rentalreservationservice.dto.*;
import org.example.rentalreservationservice.exception.BusinessException;
import org.example.rentalreservationservice.feign.InventoryItemDto;
import org.example.rentalreservationservice.feign.InventoryProxy;
import org.example.rentalreservationservice.feign.IssueRequestDto;
import org.example.rentalreservationservice.feign.StatusRequestDto;
import org.example.rentalreservationservice.model.*;
import org.example.rentalreservationservice.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentalReservationService {

    private final SmartLabProperties properties;
    private final ReservationRequestRepository requestRepository;
    private final ReservationRepository reservationRepository;
    private final LabMemberRepository memberRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final PenaltyRepository penaltyRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final ReservationEventRepository eventRepository;
    private final NotificationRepository notificationRepository;
    private final InventoryProxy inventoryProxy;

    @Transactional
    public ReservationRequest createReservationRequest(Long memberId, CreateRequestDto dto) {
        LabMember member = requireMember(memberId);
        assertMemberCanReserve(member);

        if (!dto.getEnd().isAfter(dto.getStart())) {
            throw new BusinessException("Kraj perioda mora biti posle pocetka.");
        }
        if (dto.getStart().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Termin ne moze poceti u proslosti.");
        }
        if (dto.getQuantity() < 1) {
            throw new BusinessException("Kolicina mora biti najmanje 1.");
        }

        List<InventoryItemDto> freeItems = findFreeItemsForComponent(
                dto.getComponentId(), dto.getStart(), dto.getEnd());
        if (freeItems.size() < dto.getQuantity()) {
            throw new BusinessException(
                    "Nema dovoljno slobodnih uredjaja u statusu READY_FOR_USE za trazeni termin.");
        }

        ReservationRequest request = new ReservationRequest();
        request.setMemberId(member.getId());
        request.setRequestedComponentId(dto.getComponentId());
        request.setRequestedStart(dto.getStart());
        request.setRequestedEnd(dto.getEnd());
        request.setQuantity(dto.getQuantity());
        request.setPurpose(dto.getPurpose());
        request.setStatus("PENDING");

        ReservationRequest saved = requestRepository.save(request);
        notify(member.getId(), "REQUEST_CREATED",
                "Vas zahtev #" + saved.getId() + " je primljen i ceka odobrenje inzenjera.");
        return saved;
    }

    public List<ReservationRequest> getRequestsForMember(Long memberId) {
        return requestRepository.findByMemberId(memberId);
    }

    public LabMember requireMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Clan sa id " + memberId + " ne postoji."));
    }

    public LabMember requireMemberByExternalUserId(Long externalUserId) {
        return memberRepository.findByExternalUserId(externalUserId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Ne postoji profil clana laboratorije za korisnika " + externalUserId + "."));
    }

    private void assertMemberCanReserve(LabMember member) {
        autoLiftExpiredBlock(member);

        if ("BLOCKED".equals(member.getMembershipStatus()) || "SUSPENDED".equals(member.getMembershipStatus())) {
            throw new BusinessException("Clan je trenutno " + member.getMembershipStatus()
                    + (member.getBlockReason() != null ? " (" + member.getBlockReason() + ")" : "") + ".");
        }
        if (!memberBlockRepository.findByMemberIdAndLiftedAtIsNull(member.getId()).isEmpty()) {
            throw new BusinessException("Clan ima aktivnu blokadu.");
        }
        if (penaltyRepository.existsByMemberIdAndStatus(member.getId(), "UNPAID")) {
            throw new BusinessException("Clan ima neplacene penale; rezervacija nije moguca dok se ne izmire.");
        }
        if (borrowRecordRepository.countOverdueForMember(member.getId()) > 0) {
            throw new BusinessException("Clan ima aktivno zakasnelo zaduzenje; nova rezervacija nije moguca.");
        }
        long maxActive = isEngineer(member)
                ? properties.getLimits().getEngineerActiveBorrows()
                : properties.getLimits().getStudentActiveBorrows();
        if (borrowRecordRepository.countActiveOrOverdueForMember(member.getId()) >= maxActive) {
            throw new BusinessException("Clan je dostigao maksimalan broj aktivnih zaduzenja (" + maxActive + ").");
        }
    }

    private void autoLiftExpiredBlock(LabMember member) {
        if ("BLOCKED".equals(member.getMembershipStatus())
                && member.getBlockedUntil() != null
                && member.getBlockedUntil().isBefore(LocalDateTime.now())) {
            member.setMembershipStatus("ACTIVE");
            member.setBlockedUntil(null);
            member.setBlockReason(null);
            memberRepository.save(member);
            memberBlockRepository.findByMemberIdAndLiftedAtIsNull(member.getId()).forEach(block -> {
                block.setLiftedAt(LocalDateTime.now());
                memberBlockRepository.save(block);
            });
        }
    }

    private List<InventoryItemDto> findFreeItemsForComponent(Long componentId, LocalDateTime start, LocalDateTime end) {
        return inventoryProxy.getReadyForUse(componentId).stream()
                .filter(item -> "READY_FOR_USE".equals(item.getOperationalStatus()))
                .filter(item -> reservationRepository.findOverlaps(item.getId(), start, end).isEmpty())
                .collect(Collectors.toList());
    }

    @Transactional
    public Reservation approveRequest(Long requestId, ApproveRequestDto dto) {
        ReservationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Zahtev sa id " + requestId + " ne postoji."));

        if (!"PENDING".equals(request.getStatus()) && !"PROCESSING".equals(request.getStatus())) {
            throw new BusinessException("Zahtev nije u stanju koje dozvoljava odobravanje (trenutno: "
                    + request.getStatus() + ").");
        }

        LabMember member = requireMember(request.getMemberId());
        assertMemberCanReserve(member);

        InventoryItemDto item;
        if (dto.getInventoryItemId() != null) {
            item = inventoryProxy.getItem(dto.getInventoryItemId());
            if (item == null || !"READY_FOR_USE".equals(item.getOperationalStatus())) {
                throw new BusinessException("Izabrani uredjaj nije spreman za koriscenje.");
            }
            if (!reservationRepository.findOverlaps(
                    item.getId(), request.getRequestedStart(), request.getRequestedEnd()).isEmpty()) {
                throw new BusinessException("Izabrani uredjaj je vec rezervisan u tom terminu.");
            }
        } else {
            item = findFreeItemsForComponent(
                    request.getRequestedComponentId(), request.getRequestedStart(), request.getRequestedEnd())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Nema slobodnog uredjaja za odobravanje ovog zahteva."));
        }

        Reservation reservation = new Reservation();
        reservation.setRequestId(request.getId());
        reservation.setMemberId(request.getMemberId());
        reservation.setInventoryItemId(item.getId());
        reservation.setReservationStart(request.getRequestedStart());
        reservation.setReservationEnd(request.getRequestedEnd());
        reservation.setPickupDeadline(resolvePickupDeadline(member, request.getRequestedStart(), request.getRequestedEnd()));
        reservation.setStatus("APPROVED");
        reservation.setApprovedByUserId(dto.getApprovedByUserId());
        reservation.setApprovedAt(LocalDateTime.now());
        Reservation saved = reservationRepository.save(reservation);

        request.setStatus("APPROVED");
        requestRepository.save(request);

        logEvent(saved.getId(), "APPROVED", dto.getApprovedByUserId(),
                "Zahtev odobren, dodeljen uredjaj #" + item.getId());
        notify(request.getMemberId(), "REQUEST_APPROVED",
                "Vas zahtev #" + request.getId() + " je odobren. Preuzmite uredjaj do "
                        + saved.getPickupDeadline() + ".");
        return saved;
    }

    LocalDateTime resolvePickupDeadline(LabMember member, LocalDateTime start, LocalDateTime end) {
        int hours = isEngineer(member)
                ? properties.getLimits().getEngineerPickupHours()
                : properties.getLimits().getStudentPickupHours();
        LocalDateTime proposed = start.plusHours(hours);
        return proposed.isAfter(end) ? end : proposed;
    }

    @Transactional
    public ReservationRequest rejectRequest(Long requestId, RejectRequestDto dto) {
        ReservationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Zahtev sa id " + requestId + " ne postoji."));

        if (!"PENDING".equals(request.getStatus()) && !"PROCESSING".equals(request.getStatus())) {
            throw new BusinessException("Zahtev nije u stanju koje dozvoljava odbijanje (trenutno: "
                    + request.getStatus() + ").");
        }

        request.setStatus("REJECTED");
        request.setRejectionReason(dto.getReason());
        ReservationRequest saved = requestRepository.save(request);

        notify(request.getMemberId(), "REQUEST_REJECTED",
                "Vas zahtev #" + request.getId() + " je odbijen. Razlog: " + dto.getReason());
        return saved;
    }

    @Transactional
    public BorrowRecord pickupEquipment(Long reservationId, PickupRequest dto) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Rezervacija sa id " + reservationId + " ne postoji."));

        if (!"APPROVED".equals(reservation.getStatus())) {
            throw new BusinessException("Rezervacija nije u stanju APPROVED (trenutno: "
                    + reservation.getStatus() + ").");
        }
        if (reservation.getPickupDeadline() != null && LocalDateTime.now().isAfter(reservation.getPickupDeadline())) {
            throw new BusinessException("Rok za preuzimanje uredjaja je istekao.");
        }

        LabMember member = requireMember(reservation.getMemberId());
        LocalDateTime dueDate = dto.getDueDate() != null
                ? dto.getDueDate()
                : resolveDueDate(member, reservation.getReservationEnd());
        if (dueDate.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Rok vracanja ne moze biti u proslosti.");
        }

        BorrowRecord record = new BorrowRecord();
        record.setReservationId(reservation.getId());
        record.setDueDate(dueDate);
        record.setConditionBefore(dto.getConditionBefore());
        record.setIssuedByUserId(dto.getIssuedByUserId());
        record.setStatus("ACTIVE");
        BorrowRecord savedRecord = borrowRecordRepository.save(record);

        reservation.setStatus("ACTIVE");
        reservationRepository.save(reservation);

        updateInventoryStatus(reservation.getInventoryItemId(), "IN_USE");

        logEvent(reservation.getId(), "PICKED_UP", dto.getIssuedByUserId(),
                "Uredjaj preuzet, rok vracanja " + savedRecord.getDueDate());
        notify(reservation.getMemberId(), "PICKED_UP",
                "Uredjaj je zaduzen. Rok za vracanje: " + savedRecord.getDueDate());
        return savedRecord;
    }

    LocalDateTime resolveDueDate(LabMember member, LocalDateTime reservationEnd) {
        if (isEngineer(member)) {
            return reservationEnd.plusDays(properties.getLimits().getEngineerExtraDueDays());
        }
        return reservationEnd;
    }

    @Transactional
    public BorrowRecord returnEquipment(Long reservationId, ReturnRequest dto) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Rezervacija sa id " + reservationId + " ne postoji."));

        BorrowRecord record = borrowRecordRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Zaduzenje za rezervaciju " + reservationId + " ne postoji."));

        if (!"ACTIVE".equals(record.getStatus()) && !"OVERDUE".equals(record.getStatus())) {
            throw new BusinessException("Zaduzenje nije aktivno (trenutno: " + record.getStatus() + ").");
        }

        LocalDateTime now = LocalDateTime.now();
        record.setReturnedAt(now);
        record.setConditionAfter(dto.getConditionAfter());
        record.setReturnNotes(dto.getReturnNotes());
        record.setReceivedByUserId(dto.getReceivedByUserId());
        record.setStatus(dto.isDamaged() ? "DAMAGED" : "RETURNED");

        applyLatePenaltyIfAny(reservation, record, now);
        borrowRecordRepository.save(record);

        reservation.setStatus("COMPLETED");
        reservationRepository.save(reservation);

        if (dto.isDamaged()) {
            IssueRequestDto issue = new IssueRequestDto();
            issue.setInventoryItemId(reservation.getInventoryItemId());
            issue.setReportedByUserId(dto.getReceivedByUserId());
            issue.setIssueCategory(dto.getIssueCategory() != null ? dto.getIssueCategory() : "PHYSICAL_DAMAGE");
            issue.setDescription(dto.getReturnNotes() != null ? dto.getReturnNotes() : "Uredjaj vracen ostecen.");
            inventoryProxy.reportIssue(issue);
            updateInventoryStatus(reservation.getInventoryItemId(), "PENDING_DIAGNOSTICS");
        } else {
            updateInventoryStatus(reservation.getInventoryItemId(), "READY_FOR_USE");
        }

        logEvent(reservation.getId(), "RETURNED", dto.getReceivedByUserId(),
                dto.isDamaged() ? "Uredjaj vracen ostecen." : "Uredjaj vracen ispravan.");
        notify(reservation.getMemberId(), "RETURNED", "Uredjaj je evidentiran kao vracen.");
        return record;
    }

    private void applyLatePenaltyIfAny(Reservation reservation, BorrowRecord record, LocalDateTime now) {
        if (!now.isAfter(record.getDueDate())) {
            return;
        }
        long daysLate = Math.max(1, ChronoUnit.DAYS.between(record.getDueDate().toLocalDate(), now.toLocalDate()));
        upsertLatePenalty(reservation, record, daysLate);
    }

    private void upsertLatePenalty(Reservation reservation, BorrowRecord record, long daysLate) {
        BigDecimal amount = properties.getPenalty().getDailyRate().multiply(BigDecimal.valueOf(daysLate));
        record.setPenaltyAmount(amount);

        Penalty penalty = penaltyRepository
                .findFirstByBorrowRecordIdAndStatus(record.getId(), "UNPAID")
                .orElseGet(Penalty::new);
        boolean isNew = penalty.getId() == null;
        penalty.setMemberId(reservation.getMemberId());
        penalty.setBorrowRecordId(record.getId());
        penalty.setAmount(amount);
        penalty.setReason("Kasnjenje (" + daysLate + " dan(a)).");
        penalty.setStatus("UNPAID");
        penaltyRepository.save(penalty);

        if (isNew) {
            memberRepository.findById(reservation.getMemberId()).ifPresent(member -> {
                member.setPenaltyPoints(member.getPenaltyPoints() + (int) daysLate);
                memberRepository.save(member);
            });
        }

        notify(reservation.getMemberId(), "PENALTY_ISSUED",
                "Zaduzen je penal od " + amount + " zbog kasnjenja od " + daysLate + " dan(a).");
    }

    @Transactional
    public ReservationRequest cancelRequest(Long requestId, CancelRequestDto dto) {
        ReservationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Zahtev sa id " + requestId + " ne postoji."));

        if (List.of("REJECTED", "CANCELLED", "EXPIRED", "APPROVED").contains(request.getStatus())) {
            throw new BusinessException("Zahtev se vise ne moze otkazati (trenutno: " + request.getStatus() + ").");
        }
        request.setStatus("CANCELLED");
        request.setRejectionReason(dto.getReason());
        return requestRepository.save(request);
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId, CancelRequestDto dto) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Rezervacija sa id " + reservationId + " ne postoji."));

        if (!"APPROVED".equals(reservation.getStatus())) {
            throw new BusinessException("Samo rezervacija koja ceka preuzimanje moze biti otkazana (trenutno: "
                    + reservation.getStatus() + ").");
        }

        reservation.setStatus("CANCELLED");
        reservation.setCancelledAt(LocalDateTime.now());
        reservation.setCancellationReason(dto.getReason());
        Reservation saved = reservationRepository.save(reservation);

        requestRepository.findById(reservation.getRequestId()).ifPresent(request -> {
            request.setStatus("CANCELLED");
            request.setRejectionReason(dto.getReason());
            requestRepository.save(request);
        });

        logEvent(saved.getId(), "CANCELLED", null, dto.getReason());
        notify(reservation.getMemberId(), "RESERVATION_CANCELLED",
                "Rezervacija #" + reservationId + " je otkazana.");
        return saved;
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void expireNoShowReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expired = reservationRepository.findByStatusAndPickupDeadlineBefore("APPROVED", now);
        for (Reservation reservation : expired) {
            reservation.setStatus("NO_SHOW");
            reservationRepository.save(reservation);
            requestRepository.findById(reservation.getRequestId()).ifPresent(request -> {
                request.setStatus("EXPIRED");
                requestRepository.save(request);
            });
            logEvent(reservation.getId(), "NO_SHOW", null, "Rok za preuzimanje istekao bez preuzimanja.");
            notify(reservation.getMemberId(), "NO_SHOW",
                    "Niste preuzeli uredjaj u roku, rezervacija #" + reservation.getId() + " je otkazana.");
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void processOverdueBorrows() {
        LocalDateTime now = LocalDateTime.now();

        List<BorrowRecord> justOverdue = borrowRecordRepository.findByStatusAndDueDateBefore("ACTIVE", now);
        for (BorrowRecord record : justOverdue) {
            record.setStatus("OVERDUE");
            borrowRecordRepository.save(record);
        }

        List<BorrowRecord> overdue = borrowRecordRepository.findByStatus("OVERDUE");
        for (BorrowRecord record : overdue) {
            long daysLate = Math.max(1, ChronoUnit.DAYS.between(record.getDueDate().toLocalDate(), now.toLocalDate()));
            reservationRepository.findById(record.getReservationId()).ifPresent(reservation -> {
                upsertLatePenalty(reservation, record, daysLate);
                borrowRecordRepository.save(record);
                handleBlockIfLateEnough(reservation, daysLate);
            });
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusHours(properties.getLimits().getReminderHours());
        List<BorrowRecord> upcoming = borrowRecordRepository.findByStatusAndDueDateBetween("ACTIVE", now, until);
        for (BorrowRecord record : upcoming) {
            reservationRepository.findById(record.getReservationId()).ifPresent(reservation -> {
                if (!notificationRepository.existsByMemberIdAndTypeAndCreatedAtAfter(
                        reservation.getMemberId(), "DUE_REMINDER", now.toLocalDate().atStartOfDay())) {
                    notify(reservation.getMemberId(), "DUE_REMINDER",
                            "Podsetnik: rok vracanja uredjaja je " + record.getDueDate() + ".");
                }
            });
        }
    }

    private void handleBlockIfLateEnough(Reservation reservation, long daysLate) {
        if (daysLate < properties.getPenalty().getBlockAfterDaysLate()) {
            return;
        }
        memberRepository.findById(reservation.getMemberId()).ifPresent(member -> {
            if ("BLOCKED".equals(member.getMembershipStatus())) {
                return;
            }
            member.setMembershipStatus("BLOCKED");
            member.setBlockReason("Kasnjenje sa vracanjem opreme preko "
                    + properties.getPenalty().getBlockAfterDaysLate() + " dana.");
            memberRepository.save(member);

            MemberBlock block = new MemberBlock();
            block.setMemberId(member.getId());
            block.setReason(member.getBlockReason());
            memberBlockRepository.save(block);

            notify(member.getId(), "MEMBER_BLOCKED", member.getBlockReason());
        });
    }

    public List<BorrowRecord> getActiveBorrows() {
        return borrowRecordRepository.findByStatus("ACTIVE");
    }

    public List<BorrowRecord> getOverdueBorrows() {
        return borrowRecordRepository.findByStatus("OVERDUE");
    }

    public List<Penalty> getMembersInDebt() {
        return penaltyRepository.findByStatus("UNPAID");
    }

    public List<ComponentDemandDto> getMostRequestedComponents(int limit) {
        return requestRepository.mostRequestedComponents().stream()
                .map(row -> new ComponentDemandDto((Long) row[0], (Long) row[1]))
                .sorted(Comparator.comparing(ComponentDemandDto::getRequestCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Notification> getNotificationsForMember(Long memberId) {
        return notificationRepository.findByMemberId(memberId);
    }

    @Transactional
    public Penalty payPenalty(Long penaltyId, Long waivedOrReceivedByUserId) {
        Penalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new NoSuchElementException("Penal sa id " + penaltyId + " ne postoji."));
        if (!"UNPAID".equals(penalty.getStatus())) {
            throw new BusinessException("Penal nije u statusu UNPAID.");
        }
        penalty.setStatus("PAID");
        penalty.setPaidAt(LocalDateTime.now());
        Penalty saved = penaltyRepository.save(penalty);
        notify(penalty.getMemberId(), "PENALTY_PAID", "Penal #" + penaltyId + " je evidentiran kao placen.");
        return saved;
    }

    private void notify(Long memberId, String type, String message) {
        Notification n = new Notification();
        n.setMemberId(memberId);
        n.setType(type);
        n.setMessage(message);
        notificationRepository.save(n);
    }

    private void logEvent(Long reservationId, String eventType, Long actorUserId, String message) {
        ReservationEvent event = new ReservationEvent();
        event.setReservationId(reservationId);
        event.setEventType(eventType);
        event.setActorUserId(actorUserId);
        event.setMessage(message);
        eventRepository.save(event);
    }

    private void updateInventoryStatus(Long inventoryItemId, String status) {
        StatusRequestDto request = new StatusRequestDto();
        request.setOperationalStatus(status);
        inventoryProxy.updateStatus(inventoryItemId, request);
    }

    @Transactional
    public LabMember createMember(Long externalUserId, String rolesHeader, CreateMemberRequest dto) {
        if (memberRepository.findByExternalUserId(externalUserId).isPresent()) {
            throw new BusinessException("Vec postoji profil clana laboratorije za ovog korisnika.");
        }

        LabMember member = new LabMember();
        member.setExternalUserId(externalUserId);
        member.setFirstName(dto.getFirstName());
        member.setLastName(dto.getLastName());
        member.setMemberCode(dto.getMemberCode() != null ? dto.getMemberCode() : "M-" + externalUserId);
        member.setMemberType(resolveMemberType(rolesHeader));
        return memberRepository.save(member);
    }

    private String resolveMemberType(String rolesHeader) {
        if (rolesHeader == null) {
            return "STUDENT";
        }
        boolean isEngineer = java.util.Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equals("ENGINEER") || r.equals("ADMIN"));
        return isEngineer ? "ENGINEER" : "STUDENT";
    }

    private boolean isEngineer(LabMember member) {
        return "ENGINEER".equals(member.getMemberType());
    }
}
