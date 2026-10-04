package org.example.tax;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DanishTaxEngineTest {

    @Test
    void testAmBidragCalculation() {
        DanishTaxEngine tax = new DanishTaxEngine();
        double gross = 50_000.0;
        double am = tax.calculateAmBidrag(gross);
        assertEquals(4_000.0, am, 1e-4);
    }

    @Test
    void testMonthlySalaryTaxBreakdown() {
        DanishTaxEngine tax = new DanishTaxEngine();
        double gross = 45_000.0;
        double monthlyInterest = 5_000.0;

        var breakdown = tax.calculateMonthlySalaryTax(gross, monthlyInterest);

        assertEquals(45_000.0, breakdown.grossSalary());
        assertEquals(3_600.0, breakdown.amBidrag(), 1e-4); // 8% of 45,000
        assertTrue(breakdown.interestDeductionRelief() > 0.0);
        assertTrue(breakdown.netTakeHomePay() > 0.0);
        assertTrue(breakdown.netTakeHomePay() < breakdown.grossSalary());
    }

    @Test
    void testInvestmentTaxation() {
        DanishTaxEngine tax = new DanishTaxEngine();

        // 1. ASK: flat 17%
        double askGain = 10_000.0;
        double askTax = tax.calculateInvestmentTax(askGain, InvestmentTaxType.ASK_17_LAGER);
        assertEquals(1_700.0, askTax, 1e-4);

        // 2. Aktieindkomst: 27% under 61,000 DKK, 42% over
        double smallGain = 50_000.0;
        double smallTax = tax.calculateInvestmentTax(smallGain, InvestmentTaxType.AKTIEINDKOMST_REALISATION);
        assertEquals(50_000.0 * 0.27, smallTax, 1e-4);

        double largeGain = 100_000.0;
        double largeTax = tax.calculateInvestmentTax(largeGain, InvestmentTaxType.AKTIEINDKOMST_LAGER);
        double expectedLargeTax = (61_000.0 * 0.27) + (39_000.0 * 0.42);
        assertEquals(expectedLargeTax, largeTax, 1e-4);

        // 3. Loss -> 0 tax
        double lossTax = tax.calculateInvestmentTax(-5_000.0, InvestmentTaxType.ASK_17_LAGER);
        assertEquals(0.0, lossTax, 1e-4);
    }
}
