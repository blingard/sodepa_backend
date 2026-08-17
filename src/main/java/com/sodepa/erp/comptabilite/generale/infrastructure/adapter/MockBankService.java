package com.sodepa.erp.comptabilite.generale.infrastructure.adapter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MockBankService {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MockBankTransaction {
        private LocalDate dateTransaction;
        private String libelle;
        private BigDecimal montant;
    }

    public List<MockBankTransaction> fetchTransactions(String banqueNom, LocalDate dateReleve) {
        // Generate mock bank transactions for demonstration
        List<MockBankTransaction> transactions = new ArrayList<>();
        
        // Let's assume we have 3 mock transactions
        transactions.add(new MockBankTransaction(
                dateReleve.minusDays(5),
                "VIR ENTRANT CLIENT SODEPA",
                new BigDecimal("500000.00")
        ));
        transactions.add(new MockBankTransaction(
                dateReleve.minusDays(3),
                "ACHAT MATERIEL BUREAU SARL",
                new BigDecimal("-150000.00")
        ));
        transactions.add(new MockBankTransaction(
                dateReleve.minusDays(1),
                "FRAIS TENUE DE COMPTE",
                new BigDecimal("-5000.00")
        ));

        return transactions;
    }
}
