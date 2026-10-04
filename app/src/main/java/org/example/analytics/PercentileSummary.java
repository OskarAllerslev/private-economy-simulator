package org.example.analytics;

import org.example.domain.HouseholdState;
import org.example.domain.SimulationResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Calculates empirical percentiles (P10, P25, P50/median, P75, P90) and confidence bands
 * across simulated Monte Carlo trajectories.
 */
public class PercentileSummary {

    /**
     * Computes percentiles for a single array of samples.
     */
    public static PercentileBand computeBand(double[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Values array cannot be empty");
        }

        double[] sorted = values.clone();
        Arrays.sort(sorted);

        double p10 = quantile(sorted, 0.10);
        double p25 = quantile(sorted, 0.25);
        double p50 = quantile(sorted, 0.50);
        double p75 = quantile(sorted, 0.75);
        double p90 = quantile(sorted, 0.90);

        double sum = 0.0;
        for (double v : sorted) {
            sum += v;
        }
        double mean = sum / sorted.length;

        return new PercentileBand(p10, p25, p50, p75, p90, mean);
    }

    /**
     * Computes monthly confidence bands along the entire simulation horizon for any state metric.
     *
     * @param result          Monte Carlo simulation result
     * @param metricExtractor function extracting the metric of interest (e.g. HouseholdState::getTotalNetWorth)
     * @return list of monthly percentile points
     */
    public static List<MonthlyPercentilePoint> computeTrajectoryBands(
        SimulationResult result,
        ToDoubleFunction<HouseholdState> metricExtractor
    ) {
        int horizon = result.getHorizonMonths();
        int totalPaths = result.getTotalPaths();
        List<MonthlyPercentilePoint> trajectory = new ArrayList<>(horizon + 1);

        for (int m = 0; m <= horizon; m++) {
            double[] monthValues = new double[totalPaths];
            for (int p = 0; p < totalPaths; p++) {
                List<HouseholdState> path = result.getPath(p);
                int idx = Math.min(m, path.size() - 1);
                monthValues[p] = metricExtractor.applyAsDouble(path.get(idx));
            }
            PercentileBand band = computeBand(monthValues);
            trajectory.add(new MonthlyPercentilePoint(m, band));
        }

        return trajectory;
    }

    private static double quantile(double[] sorted, double q) {
        if (sorted.length == 1) {
            return sorted[0];
        }
        double rank = q * (sorted.length - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        double weight = rank - lower;
        return (1.0 - weight) * sorted[lower] + weight * sorted[upper];
    }

    public record PercentileBand(
        double p10,
        double p25,
        double p50,
        double p75,
        double p90,
        double mean
    ) {}

    public record MonthlyPercentilePoint(
        int month,
        PercentileBand band
    ) {
        public double year() {
            return month / 12.0;
        }
    }
}
