package org.example.finance;

import org.example.tax.InvestmentTaxType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FinanceTest {

    @Test
    void testEtfHoldingGakAndTrades() {
        // Buy 100 units at 500 DKK -> GAK = 500
        EtfHolding etf = new EtfHolding("LYPS", 100.0, 500.0, 500.0, InvestmentTaxType.AKTIEINDKOMST_LAGER);
        assertEquals(50_000.0, etf.getCurrentValue());
        assertEquals(500.0, etf.getAveragePurchasePrice());

        // Buy another 100 units at 600 DKK -> total cost = 50,000 + 60,000 = 110,000 / 200 units -> GAK = 550
        etf.buy(100.0, 600.0);
        assertEquals(200.0, etf.getUnits());
        assertEquals(550.0, etf.getAveragePurchasePrice(), 1e-4);
        assertEquals(120_000.0, etf.getCurrentValue());
        assertEquals(10_000.0, etf.getUnrealizedGain());

        // Sell 50 units at 700 DKK -> realized gain = 50 * (700 - 550) = 7,500 DKK
        double realizedGain = etf.sell(50.0, 700.0);
        assertEquals(7_500.0, realizedGain, 1e-4);
        assertEquals(150.0, etf.getUnits());
        assertEquals(550.0, etf.getAveragePurchasePrice(), 1e-4); // GAK unchanged after sell
    }

    @Test
    void testMortgageAmortizationAndAfdragsfrihed() {
        // 2,000,000 DKK mortgage, 4% rate, 0.70% bidragssats, 30 years, 1 year (12 months) interest-only
        Mortgage mortgage = Mortgage.createFixedRate(2_000_000.0, 0.04, 0.007, 30, 1);
        assertTrue(mortgage.isInterestOnlyActive());

        // Month 1: interest-only -> principal repayment should be 0
        var p1 = mortgage.stepMonth(0.04);
        assertEquals(0.0, p1.principalRepayment(), 1e-4);
        assertEquals(2_000_000.0, p1.remainingPrincipal(), 1e-4);
        assertTrue(p1.totalPayment() > 0.0);
        assertEquals(11, mortgage.getInterestOnlyMonthsRemaining());

        // Step remaining 11 interest-only months
        for (int i = 0; i < 11; i++) {
            mortgage.stepMonth(0.04);
        }
        assertFalse(mortgage.isInterestOnlyActive());

        // Month 13: amortization begins -> principal repayment should be > 0
        var p13 = mortgage.stepMonth(0.04);
        assertTrue(p13.principalRepayment() > 0.0);
        assertTrue(p13.remainingPrincipal() < 2_000_000.0);
    }

    @Test
    void testRealEstateEquityAndCosts() {
        Mortgage mortgage = Mortgage.createFKort(2_000_000.0, 0.03, 0.007, 30, 0);
        RealEstate house = new RealEstate("House", 2_500_000.0, 0.01, 0.012, mortgage);

        assertEquals(500_000.0, house.getHomeEquity());
        assertEquals(0.80, house.getLoanToValue(), 1e-4);
        assertEquals((2_500_000.0 * 0.01) / 12.0, house.getMonthlyMaintenanceCost(), 1e-4);
        assertEquals((2_500_000.0 * 0.012) / 12.0, house.getMonthlyPropertyTax(), 1e-4);
    }
}
