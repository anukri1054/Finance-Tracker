package com.fintrac.service;

import com.fintrac.dto.AlertDTO;
import com.fintrac.dto.BudgetDTO;
import com.fintrac.model.Alert;
import com.fintrac.model.AlertType;
import com.fintrac.model.User;
import com.fintrac.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AuthService authService;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private AlertService alertService;

    private User testUser;
    private Alert testAlert;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .password("encoded_password")
                .build();

        testAlert = Alert.builder()
                .id(1L)
                .user(testUser)
                .type(AlertType.WARNING)
                .message("Test alert message")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getUnreadAlerts_ReturnsAlerts_WhenUnreadExist() {
        when(authService.getCurrentUserId()).thenReturn(1L);
        when(alertRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(testAlert));

        List<AlertDTO> result = alertService.getUnreadAlerts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test alert message", result.get(0).getMessage());
    }

    @Test
    void getAllAlerts_ReturnsAllAlerts() {
        when(authService.getCurrentUserId()).thenReturn(1L);
        when(alertRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(testAlert));

        List<AlertDTO> result = alertService.getAllAlerts();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getUnreadCount_ReturnsCorrectCount() {
        when(authService.getCurrentUserId()).thenReturn(1L);
        when(alertRepository.countUnreadByUserId(1L)).thenReturn(5L);

        long count = alertService.getUnreadCount();

        assertEquals(5L, count);
    }

    @Test
    void markAllAsRead_CallsRepository() {
        when(authService.getCurrentUserId()).thenReturn(1L);
        doNothing().when(alertRepository).markAllAsReadByUserId(1L);

        alertService.markAllAsRead();

        verify(alertRepository).markAllAsReadByUserId(1L);
    }
}