package com.fasol.unit.service;

import com.fasol.domain.entity.TrialRequest;
import com.fasol.domain.enums.TrialRequestStatus;
import com.fasol.dto.request.TrialRequestDto;
import com.fasol.dto.response.TrialRequestResponse;
import com.fasol.exception.ResourceNotFoundException;
import com.fasol.repository.TrialRequestRepository;
import com.fasol.service.impl.TrialRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrialRequestServiceTest {

    @Mock TrialRequestRepository repository;

    @InjectMocks TrialRequestServiceImpl service;

    private TrialRequest request;
    private UUID requestId;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        request = TrialRequest.builder()
                .id(requestId)
                .firstName("Мария")
                .lastName("Смирнова")
                .phone("+79001234567")
                .wantsWhatsapp(true)
                .status(TrialRequestStatus.NEW)
                .build();
    }

    @Test
    @DisplayName("create — сохраняет заявку со статусом NEW")
    void create_savesWithStatusNew() {
        TrialRequestDto dto = TrialRequestDto.builder()
                .firstName("Мария")
                .lastName("Смирнова")
                .phone("+79001234567")
                .wantsWhatsapp(true)
                .build();

        when(repository.save(any())).thenReturn(request);

        TrialRequestResponse result = service.create(dto);

        assertThat(result.getFirstName()).isEqualTo("Мария");
        assertThat(result.getStatus()).isEqualTo(TrialRequestStatus.NEW);
        assertThat(result.isWantsWhatsapp()).isTrue();
    }

    @Test
    @DisplayName("getAll — возвращает список всех заявок")
    void getAll_returnsList() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(request));

        List<TrialRequestResponse> result = service.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPhone()).isEqualTo("+79001234567");
    }

    @Test
    @DisplayName("updateStatus — меняет статус и заметки")
    void updateStatus_changesStatusAndNotes() {
        when(repository.findById(requestId)).thenReturn(Optional.of(request));
        when(repository.save(any())).thenReturn(request);

        TrialRequestResponse result = service.updateStatus(
                requestId, TrialRequestStatus.CONTACTED, "Позвонили, договорились");

        assertThat(request.getStatus()).isEqualTo(TrialRequestStatus.CONTACTED);
        assertThat(request.getNotes()).isEqualTo("Позвонили, договорились");
    }

    @Test
    @DisplayName("updateStatus — бросает ResourceNotFoundException если заявка не найдена")
    void updateStatus_notFound_throws() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(
                UUID.randomUUID(), TrialRequestStatus.CONTACTED, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateStatus — не меняет notes если передан null")
    void updateStatus_nullNotes_keepsOldNotes() {
        request.setNotes("Старая заметка");
        when(repository.findById(requestId)).thenReturn(Optional.of(request));
        when(repository.save(any())).thenReturn(request);

        service.updateStatus(requestId, TrialRequestStatus.SCHEDULED, null);

        assertThat(request.getNotes()).isEqualTo("Старая заметка");
    }
}
