package com.fintrac.service;

import com.fintrac.dto.TransactionDTO;
import com.fintrac.model.*;
import com.fintrac.repository.CategoryRepository;
import com.fintrac.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthService authService;

    @Mock
    private BudgetService budgetService;

    @Mock
    private AlertService alertService;

    @Mock
    private InsightService insightService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Category testCategory;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .password("encoded_password")
                .build();

        testCategory = Category.builder()
                .id(1L)
                .name("Food")
                .icon("utensils")
                .color("#FF5722")
                .type(TransactionType.EXPENSE)
                .isDefault(true)
                .build();

        testTransaction = Transaction.builder()
                .id(1L)
                .user(testUser)
                .category(testCategory)
                .title("Lunch")
                .amount(new BigDecimal("500.00"))
                .date(LocalDate.now())
                .type(TransactionType.EXPENSE)
                .paymentType(PaymentType.PERSONAL)
                .description("Lunch description")
                .merchantName("Restaurant")
                .isEmergency(false)
                .build();
    }

    @Test
    void createTransaction_SavesExpense_AndTriggersBudgetUpdate() {
        TransactionDTO inputDTO = TransactionDTO.builder()
                .title("Lunch")
                .categoryId(1L)
                .amount(new BigDecimal("500.00"))
                .date(LocalDate.now())
                .type(TransactionType.EXPENSE)
                .paymentType("PERSONAL")
                .description("Lunch description")
                .merchantName("Restaurant")
                .isEmergency(false)
                .build();

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        TransactionDTO result = transactionService.createTransaction(inputDTO);

        assertNotNull(result);
        assertEquals(new BigDecimal("500.00"), result.getAmount());
        verify(budgetService).updateBudgetSpending(eq(1L), any(LocalDate.class), any(BigDecimal.class), anyBoolean());
        verify(alertService).checkBudgetAlerts(eq(1L));
        verify(insightService).generateInsights(eq(1L));
    }

    @Test
    void getTransactionById_ReturnsTransaction_WhenExists() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        TransactionDTO result = transactionService.getTransactionById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(new BigDecimal("500.00"), result.getAmount());
    }

    @Test
    void getTransactionById_ThrowsException_WhenNotFound() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> transactionService.getTransactionById(999L));
    }

    @Test
    void getAllTransactions_ReturnsUserTransactions() {
        List<Transaction> transactions = Arrays.asList(testTransaction);
        when(authService.getCurrentUserId()).thenReturn(1L);
        when(transactionRepository.findByUserIdOrderByDateDesc(1L))
                .thenReturn(transactions);

        List<TransactionDTO> result = transactionService.getAllTransactions();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("500.00"), result.get(0).getAmount());
    }

    @Test
    void getTransactionsByDateRange_ReturnsFilteredTransactions() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        List<Transaction> transactions = Arrays.asList(testTransaction);
        when(authService.getCurrentUserId()).thenReturn(1L);
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(1L, startDate, endDate))
                .thenReturn(transactions);

        List<TransactionDTO> result = transactionService.getTransactionsByDateRange(startDate, endDate);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getTransactionsByType_ReturnsFilteredTransactions() {
        List<Transaction> transactions = Arrays.asList(testTransaction);
        when(authService.getCurrentUserId()).thenReturn(1L);
        when(transactionRepository.findByUserIdAndTypeOrderByDateDesc(1L, TransactionType.EXPENSE))
                .thenReturn(transactions);

        List<TransactionDTO> result = transactionService.getTransactionsByType(TransactionType.EXPENSE);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void deleteTransaction_RestoresBudget() {
        when(authService.getCurrentUserId()).thenReturn(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        doNothing().when(transactionRepository).delete(testTransaction);

        transactionService.deleteTransaction(1L);

        verify(transactionRepository).delete(testTransaction);
        verify(budgetService).recalculateBudgetOnDelete(
                eq(1L),
                any(LocalDate.class),
                any(BigDecimal.class),
                anyBoolean()
        );
    }
}