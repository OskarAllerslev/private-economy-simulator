package org.example.domain;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the complete results of a Monte Carlo simulation.
 * Contains individual household trajectories, milestone stopping times (tau),
 * and aggregated summary statistics.
 */
public class SimulationResult {
    private final List<List<HouseholdState>> paths;
    private final Map<String, double[]> stoppingTimes;
    private final int totalPaths;
    private final int horizonMonths;

    public SimulationResult(
        List<List<HouseholdState>> paths,
        Map<String, double[]> stoppingTimes,
        int totalPaths,
        int horizonMonths
    ) {
        this.paths = Collections.unmodifiableList(paths);
        this.stoppingTimes = Collections.unmodifiableMap(stoppingTimes);
        this.totalPaths = totalPaths;
        this.horizonMonths = horizonMonths;
    }

    public List<List<HouseholdState>> getPaths() {
        return paths;
    }

    public List<HouseholdState> getPath(int index) {
        return paths.get(index);
    }

    public Map<String, double[]> getAllStoppingTimes() {
        return stoppingTimes;
    }

    public double[] getStoppingTimes(String milestoneName) {
        return stoppingTimes.get(milestoneName);
    }

    public int getTotalPaths() {
        return totalPaths;
    }

    public int getHorizonMonths() {
        return horizonMonths;
    }

    /**
     * Extracts total net worth across all paths at a specific month.
     */
    public double[] getNetWorthsAtMonth(int month) {
        double[] values = new double[totalPaths];
        for (int i = 0; i < totalPaths; i++) {
            List<HouseholdState> path = paths.get(i);
            int m = Math.min(month, path.size() - 1);
            values[i] = path.get(m).getTotalNetWorth();
        }
        return values;
    }

    /**
     * Extracts liquid net worth across all paths at a specific month.
     */
    public double[] getLiquidNetWorthsAtMonth(int month) {
        double[] values = new double[totalPaths];
        for (int i = 0; i < totalPaths; i++) {
            List<HouseholdState> path = paths.get(i);
            int m = Math.min(month, path.size() - 1);
            values[i] = path.get(m).getLiquidNetWorth();
        }
        return values;
    }

    /**
     * Extracts terminal total net worth at the end of the simulation horizon.
     */
    public double[] getTerminalNetWorths() {
        return getNetWorthsAtMonth(horizonMonths);
    }

    /**
     * Extracts terminal liquid net worth at the end of the simulation horizon.
     */
    public double[] getTerminalLiquidWorths() {
        return getLiquidNetWorthsAtMonth(horizonMonths);
    }
}
