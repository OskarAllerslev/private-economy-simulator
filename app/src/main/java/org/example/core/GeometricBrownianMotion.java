package org.example.core;

/**
 * Geometric Brownian Motion (GBM) model:
 * <pre>
 *   dS_t = mu * S_t * dt + sigma * S_t * dW_t
 * </pre>
 * Exact analytic solution for discrete steps:
 * <pre>
 *   S_{t+dt} = S_t * exp((mu - 0.5 * sigma^2) * dt + sigma * sqrt(dt) * Z)
 * </pre>
 * where Z ~ N(0, 1). Typically used for equity indices/ETFs (e.g. LYPS) and real estate values.
 *
 * @param mu    annual expected drift (return)
 * @param sigma annual volatility (standard deviation)
 */
public record GeometricBrownianMotion(double mu, double sigma) implements StochasticProcess {

    public GeometricBrownianMotion {
        if (sigma < 0.0) {
            throw new IllegalArgumentException("Volatility sigma cannot be negative: " + sigma);
        }
    }

    @Override
    public double nextValue(double currentValue, double dt, double normalZ) {
        if (currentValue <= 0.0) {
            return 0.0;
        }
        double drift = (mu - 0.5 * sigma * sigma) * dt;
        double diffusion = sigma * Math.sqrt(dt) * normalZ;
        return currentValue * Math.exp(drift + diffusion);
    }
}
