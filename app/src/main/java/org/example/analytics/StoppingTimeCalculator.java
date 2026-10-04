package org.example.analytics;

import java.util.Arrays;

/**
 * Computes statistics on stopping times (tau):
 * <pre>
 *   tau = inf { t >= 0 : Wealth_t >= Threshold }
 * </pre>
 * Evaluates the probability of reaching financial milestones (e.g. 500k DKK, 1M DKK, FIRE)
 * and calculates the conditional distribution (mean, median, P10, P90) of months required.
 */
public class StoppingTimeCalculator {

    /**
     * Analyzes stopping time distribution from an array of stopping times.
     * Negative values indicate the milestone was not achieved within the horizon.
     *
     * @param milestoneName name of the target milestone
     * @param stoppingTimes array of stopping times in months per path
     * @return statistical summary of stopping times
     */
    public static StoppingTimeSummary computeSummary(String milestoneName, double[] stoppingTimes) {
        if (stoppingTimes == null || stoppingTimes.length == 0) {
            return new StoppingTimeSummary(milestoneName, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0);
        }

        int totalPaths = stoppingTimes.length;
        double[] reachedTimes = Arrays.stream(stoppingTimes)
            .filter(t -> t >= 0.0)
            .sorted()
            .toArray();

        int reachedCount = reachedTimes.length;
        double probability = (double) reachedCount / totalPaths;

        if (reachedCount == 0) {
            return new StoppingTimeSummary(milestoneName, 0.0, -1.0, -1.0, -1.0, -1.0, 0, totalPaths);
        }

        double sum = 0.0;
        for (double t : reachedTimes) {
            sum += t;
        }
        double meanMonths = sum / reachedCount;

        double p10Months = percentile(reachedTimes, 0.10);
        double medianMonths = percentile(reachedTimes, 0.50);
        double p90Months = percentile(reachedTimes, 0.90);

        return new StoppingTimeSummary(
            milestoneName,
            probability,
            meanMonths,
            medianMonths,
            p10Months,
            p90Months,
            reachedCount,
            totalPaths
        );
    }

    private static double percentile(double[] sorted, double p) {
        if (sorted.length == 1) {
            return sorted[0];
        }
        double rank = p * (sorted.length - 1);
        int lowerIndex = (int) Math.floor(rank);
        int upperIndex = (int) Math.ceil(rank);
        double weight = rank - lowerIndex;
        return (1.0 - weight) * sorted[lowerIndex] + weight * sorted[upperIndex];
    }

    public record StoppingTimeSummary(
        String milestoneName,
        double probabilityOfReaching,
        double meanMonths,
        double medianMonths,
        double p10Months,
        double p90Months,
        int reachedCount,
        int totalPaths
    ) {
        public double meanYears() {
            return meanMonths >= 0 ? meanMonths / 12.0 : -1.0;
        }

        public double medianYears() {
            return medianMonths >= 0 ? medianMonths / 12.0 : -1.0;
        }
    }
}
