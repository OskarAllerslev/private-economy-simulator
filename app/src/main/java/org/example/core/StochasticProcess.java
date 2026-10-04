package org.example.core;

/**
 * Interface representing a 1D continuous-time stochastic process.
 */
public interface StochasticProcess {
    /**
     * Advances the process by time increment dt given a standard normal random variate Z ~ N(0, 1).
     *
     * @param currentValue current value of the process X_t
     * @param dt           time increment (e.g. 1.0 / 12.0 for 1 month)
     * @param normalZ      standard normal variate drawn from N(0, 1)
     * @return next value X_{t+dt}
     */
    double nextValue(double currentValue, double dt, double normalZ);
}
