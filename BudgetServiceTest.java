package com.fintrac.service;

import com.fintrac.dto.BudgetDTO;
import com.fintrac.model.*;
import com.fintrac.repository.BudgetRepository;
import com.fintrac.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private BudgetService budgetService;

    private User testUser;
    private Budget testBudget;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .password("encoded_password")
                .build();

        testBudget = Budget.builder()
                .id(1L)
                .user(testUser)
                .month("2025-01")
                .initialBudget(new BigDecimal("5000.00"))
                .adjustedBudget(new BigDecimal("5000.00"))
                .emergencySpent(BigDecimal.ZERO)
                .totalSpent(new BigDecimal("1500.00"))
                .build();
    }

    @Test
    void getBudgetByMonth_ReturnsBudgetDTO_WhenBudgetExists() {
        when(authService.getCurrentUserId()).thenReturn(1L);
        when(budgetRepository.findByUserIdAndMonth(1L, "2025-01"))
                .thenReturn(Optional.of(testBudget));

        BudgetDTO result = budgetService.getBudgetByMonth("2025-01");

        assertNotNull(result);
        assertEquals("2025-01", result.getMonth());
        assertEquals(new BigDecimal("5000.00"), result.getInitialBudget());
        assertEquals(new BigDecimal("1500.00"), result.getTotalSpent());
    }

    @Test
    void getBudgetByMonth_ReturnsNewBudget_WhenNoBudgetExists() {
        when(authService.getCurrentUserId()).thenReturn(1L);
        when(budgetRepository.findByUserIdAndMonth(1L, "2025-01"))
                .thenReturn(Optional.empty());

        BudgetDTO result = budgetService.getBudgetByMonth("2025-01");

        assertNotNull(result);
        assertEquals("2025-01", result.getMonth());
    }

    @Test
    void updateBudgetSpending_IncreasesTotalSpent_ForNormalExpense() {
        when(budgetRepository.findByUserIdAndMonth(1L, "2025-01"))
                .thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        budgetService.updateBudgetSpending(1L, LocalDate.of(2025, 1, 15), new BigDecimal("500.00"), false);

        verify(budgetRepository).save(argThat(budget ->
                budget.getTotalSpent().compareTo(new BigDecimal("2000.00")) == 0
        ));
    }

    @Test
    void updateBudgetSpending_IncreasesEmergencySpent_WhenEmergencyFlagTrue() {
        when(budgetRepository.findByUserIdAndMonth(1L, "2025-01"))
                .thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        budgetService.updateBudgetSpending(1L, LocalDate.of(2025, 1, 15), new BigDecimal("500.00"), true);

        verify(budgetRepository).save(argThat(budget ->
                budget.getEmergencySpent().compareTo(new BigDecimal("500.00")) == 0
        ));
    }

    @Test
    void recalculateBudgetSpending_UpdatesTotals_WhenExpenseModified() {
        when(budgetRepository.findByUserIdAndMonth(1L, "2025-01"))
                .thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        budgetService.recalculateBudgetSpending(
                1L,
                LocalDate.of(2025, 1, 15),
                new BigDecimal("500.00"),
                false,
                new BigDecimal("700.00"),
                false
        );

        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void recalculateBudgetOnDelete_SubtractsFromTotalSpent() {
        when(budgetRepository.findByUserIdAndMonth(1L, "2025-01"))
                .thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        budgetService.recalculateBudgetOnDelete(1L, LocalDate.of(2025, 1, 15), new BigDecimal("500.00"), false);

        verify(budgetRepository).save(argThat(budget ->
                budget.getTotalSpent().compareTo(new BigDecimal("1000.00")) == 0
        ));
    }
}