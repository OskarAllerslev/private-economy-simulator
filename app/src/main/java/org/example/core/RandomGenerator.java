package org.example.core;

import java.util.SplittableRandom;

/**
 * Random number generator optimized for Monte Carlo simulations.
 * Provides high-speed pseudo-random generation with standard normal variates (Z ~ N(0, 1))
 * via Box-Muller transformation or Marsaglia polar method.
 */
public class RandomGenerator {
    private final SplittableRandom internalPrng;
    private double nextGaussianStore;
    private boolean haveNextGaussianStore = false;

    public RandomGenerator() {
        this.internalPrng = new SplittableRandom();
    }

    public RandomGenerator(long seed) {
        this.internalPrng = new SplittableRandom(seed);
    }

    public static RandomGenerator create() {
        return new RandomGenerator();
    }

    public static RandomGenerator create(long seed) {
        return new RandomGenerator(seed);
    }

    /**
     * Splits this generator to create an independent child generator for concurrent tasks / paths.
     */
    public RandomGenerator split() {
        return new RandomGenerator(internalPrng.split().nextLong());
    }

    /**
     * Returns a uniformly distributed double value between 0.0 (inclusive) and 1.0 (exclusive).
     */
    public double nextDouble() {
        return internalPrng.nextDouble();
    }

    /**
     * Returns a standard normal random variate Z ~ N(0, 1).
     * Implements Box-Muller polar transformation with cached second variate.
     */
    public synchronized double nextGaussian() {
        if (haveNextGaussianStore) {
            haveNextGaussianStore = false;
            return nextGaussianStore;
        }

        double v1;
        double v2;
        double s;
        do {
            v1 = 2.0 * internalPrng.nextDouble() - 1.0; // between -1.0 and 1.0
            v2 = 2.0 * internalPrng.nextDouble() - 1.0; // between -1.0 and 1.0
            s = v1 * v1 + v2 * v2;
        } while (s >= 1.0 || s == 0.0);

        double multiplier = Math.sqrt(-2.0 * Math.log(s) / s);
        nextGaussianStore = v2 * multiplier;
        haveNextGaussianStore = true;
        return v1 * multiplier;
    }

    /**
     * Returns a normal random variate with specified mean and standard deviation.
     */
    public double nextGaussian(double mean, double stdDev) {
        return mean + stdDev * nextGaussian();
    }
}
