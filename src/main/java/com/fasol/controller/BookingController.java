package com.fasol.controller;

import com.fasol.dto.request.BookingRequest;
import com.fasol.dto.response.BookingResponse;
import com.fasol.security.CurrentUser;
import com.fasol.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<BookingResponse> create(
            @CurrentUser UUID studentId,
            @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(studentId, request));
    }

    @DeleteMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('STUDENT','MANAGER','ADMIN')")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID bookingId,
            @CurrentUser UUID currentUserId) {
        bookingService.cancelBooking(bookingId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<BookingResponse>> myBookings(@CurrentUser UUID studentId) {
        return ResponseEntity.ok(bookingService.getStudentBookings(studentId));
    }
}
