package com.fintrac.service;

import com.fintrac.dto.InsightDTO;
import com.fintrac.model.*;
import com.fintrac.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsightServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private InsightService insightService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .password("encoded_password")
                .build();
        
        // Default mocks to avoid NPEs in helper methods
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(anyLong(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.findTransactionsForPeriod(anyLong(), any(), any()))
                .thenReturn(new ArrayList<>());
    }

    @Test
    void generateInsights_ReturnsSpendingPattern_WhenExpensesExist() {
        List<Object[]> categoryExpenses = new ArrayList<>();
        categoryExpenses.add(new Object[]{"Food", new BigDecimal("800.00")});
        categoryExpenses.add(new Object[]{"Transport", new BigDecimal("200.00")});
        when(transactionRepository.sumExpensesByCategory(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(categoryExpenses);
        
        when(transactionRepository.sumExpensesByMerchant(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new ArrayList<>());

        List<InsightDTO> insights = insightService.generateInsights(1L);

        assertNotNull(insights);
        // We expect at least the category alert if it exceeds 30%
        // Total is 1000, Food is 800 (80%), Transport is 200 (20%)
        assertFalse(insights.isEmpty());
        assertTrue(insights.stream().anyMatch(i -> i.getType().equals("CATEGORY_ALERT")));
    }

    @Test
    void generateInsights_ReturnsMerchantInsight_WhenSpendingExists() {
        List<Object[]> merchantExpenses = new ArrayList<>();
        merchantExpenses.add(new Object[]{"Restaurant", new BigDecimal("1500.00")});
        when(transactionRepository.sumExpensesByMerchant(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(merchantExpenses);
        
        when(transactionRepository.sumExpensesByCategory(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new ArrayList<>());

        List<InsightDTO> insights = insightService.generateInsights(1L);

        assertNotNull(insights);
        assertTrue(insights.stream().anyMatch(i -> i.getType().equals("MERCHANT")));
    }

    @Test
    void generateInsights_ReturnsEmptyList_WhenNoTransactions() {
        when(transactionRepository.sumExpensesByCategory(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new ArrayList<>());
        when(transactionRepository.sumExpensesByMerchant(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new ArrayList<>());

        List<InsightDTO> insights = insightService.generateInsights(1L);

        assertNotNull(insights);
    }

    @Test
    void generateInsights_DetectsSpendingSpike() {
        List<Transaction> transactions = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // Low average spending for 5 days
        for (int i = 1; i <= 5; i++) {
            transactions.add(Transaction.builder()
                    .amount(new BigDecimal("100.00"))
                    .date(today.minusDays(i))
                    .type(TransactionType.EXPENSE)
                    .build());
        }
        
        // Spike: 5000 (significantly > 3 * average)
        transactions.add(Transaction.builder()
                .amount(new BigDecimal("5000.00"))
                .date(today)
                .type(TransactionType.EXPENSE)
                .build());
        
        when(transactionRepository.findTransactionsForPeriod(anyLong(), any(), any()))
                .thenReturn(transactions);
        when(transactionRepository.sumExpensesByCategory(anyLong(), any(), any()))
                .thenReturn(new ArrayList<>());
        when(transactionRepository.sumExpensesByMerchant(anyLong(), any(), any()))
                .thenReturn(new ArrayList<>());

        List<InsightDTO> insights = insightService.generateInsights(1L);

        assertNotNull(insights);
        assertTrue(insights.stream().anyMatch(i -> i.getType().equals("SPIKE")));
    }
}