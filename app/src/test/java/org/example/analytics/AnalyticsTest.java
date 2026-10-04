package org.example.analytics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsTest {

    @Test
    void testCrraUtilityLogCase() {
        // gamma = 1.0 -> ln(w)
        double w = Math.E;
        double u = UtilityAnalyzer.crraUtility(w, 1.0);
        assertEquals(1.0, u, 1e-6);

        double ce = UtilityAnalyzer.certaintyEquivalent(1.0, 1.0);
        assertEquals(Math.E, ce, 1e-6);
    }

    @Test
    void testCrraRiskAversionAndCertaintyEquivalent() {
        // Under risk aversion (gamma > 0), CE of a risky lottery is less than Expected Wealth
        double[] wealths = {500_000.0, 1_500_000.0}; // E[W] = 1,000,000
        double gamma = 2.0;

        var report = UtilityAnalyzer.evaluate(wealths, gamma);

        assertEquals(1_000_000.0, report.expectedWealth(), 1e-4);
        assertTrue(report.certaintyEquivalent() < report.expectedWealth());
        assertTrue(report.riskPremium() > 0.0);
    }

    @Test
    void testStoppingTimeCalculator() {
        // 5 paths: times are 12, 24, 36, -1, -1 (3 reached, 2 not reached)
        double[] taus = {12.0, 24.0, 36.0, -1.0, -1.0};
        var summary = StoppingTimeCalculator.computeSummary("Target 500k", taus);

        assertEquals(0.60, summary.probabilityOfReaching(), 1e-4);
        assertEquals(3, summary.reachedCount());
        assertEquals(5, summary.totalPaths());
        assertEquals(24.0, summary.meanMonths(), 1e-4);
        assertEquals(24.0, summary.medianMonths(), 1e-4);
        assertEquals(2.0, summary.meanYears(), 1e-4);
    }

    @Test
    void testPercentileSummaryBand() {
        double[] samples = new double[101];
        for (int i = 0; i <= 100; i++) {
            samples[i] = i; // 0 to 100
        }

        var band = PercentileSummary.computeBand(samples);

        assertEquals(50.0, band.p50(), 1e-4);
        assertEquals(10.0, band.p10(), 1e-4);
        assertEquals(90.0, band.p90(), 1e-4);
        assertEquals(50.0, band.mean(), 1e-4);
    }
}
