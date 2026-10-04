package org.example.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StochasticProcessTest {

    @Test
    void testGeometricBrownianMotionDeterministicDrift() {
        double mu = 0.08;
        double sigma = 0.0; // Zero volatility -> purely deterministic exponential growth
        GeometricBrownianMotion gbm = new GeometricBrownianMotion(mu, sigma);

        double s0 = 100.0;
        double dt = 1.0;
        double s1 = gbm.nextValue(s0, dt, 0.0);

        assertEquals(s0 * Math.exp(mu * dt), s1, 1e-6);
    }

    @Test
    void testGeometricBrownianMotionStochasticDiffusion() {
        double mu = 0.05;
        double sigma = 0.20;
        GeometricBrownianMotion gbm = new GeometricBrownianMotion(mu, sigma);

        double s0 = 100.0;
        double dt = 1.0 / 12.0;

        double sUp = gbm.nextValue(s0, dt, 1.0);
        double sDown = gbm.nextValue(s0, dt, -1.0);

        assertTrue(sUp > sDown);
        assertTrue(sUp > 0);
        assertTrue(sDown > 0);
    }

    @Test
    void testVasicekMeanReversion() {
        double a = 0.5; // Reversion speed
        double b = 0.04; // Long term mean 4%
        double sigma = 0.0; // Deterministic reversion test
        VasicekProcess vasicek = new VasicekProcess(a, b, sigma);

        double r0 = 0.10; // Starts at 10%
        double dt = 1.0;
        double r1 = vasicek.nextValue(r0, dt, 0.0);

        // Should revert towards 0.04
        assertTrue(r1 < r0);
        assertTrue(r1 > b);
    }

    @Test
    void testRandomGeneratorDistribution() {
        RandomGenerator rng = RandomGenerator.create(12345L);
        int sampleCount = 50_000;
        double sum = 0.0;
        double sumSq = 0.0;

        for (int i = 0; i < sampleCount; i++) {
            double z = rng.nextGaussian();
            sum += z;
            sumSq += z * z;
        }

        double mean = sum / sampleCount;
        double variance = (sumSq / sampleCount) - (mean * mean);

        // Standard Normal Z ~ N(0, 1)
        assertEquals(0.0, mean, 0.03, "Sample mean should be close to 0");
        assertEquals(1.0, variance, 0.05, "Sample variance should be close to 1");
    }
}
