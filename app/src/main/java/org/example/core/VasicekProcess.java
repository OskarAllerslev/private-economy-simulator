package org.example.core;

/**
 * Vasicek short-rate model for interest rate dynamics (e.g. F-kort / variable mortgage rates):
 * <pre>
 *   dr_t = a * (b - r_t) * dt + sigma * dW_t
 * </pre>
 * where:
 * <ul>
 *   <li>a: speed of mean reversion</li>
 *   <li>b: long-term mean interest rate</li>
 *   <li>sigma: instantaneous volatility</li>
 * </ul>
 * The exact transition distribution conditional on r_t is:
 * <pre>
 *   E[r_{t+dt}] = r_t * exp(-a * dt) + b * (1 - exp(-a * dt))
 *   Var[r_{t+dt}] = (sigma^2 / (2 * a)) * (1 - exp(-2 * a * dt))
 * </pre>
 */
public record VasicekProcess(double a, double b, double sigma) implements StochasticProcess {

    public VasicekProcess {
        if (a <= 0.0) {
            throw new IllegalArgumentException("Mean reversion speed 'a' must be positive: " + a);
        }
        if (sigma < 0.0) {
            throw new IllegalArgumentException("Volatility sigma cannot be negative: " + sigma);
        }
    }

    @Override
    public double nextValue(double currentValue, double dt, double normalZ) {
        double expDecay = Math.exp(-a * dt);
        double conditionalMean = currentValue * expDecay + b * (1.0 - expDecay);
        double conditionalVariance = (sigma * sigma / (2.0 * a)) * (1.0 - Math.exp(-2.0 * a * dt));
        double conditionalStdDev = Math.sqrt(Math.max(0.0, conditionalVariance));
        return conditionalMean + conditionalStdDev * normalZ;
    }
}
