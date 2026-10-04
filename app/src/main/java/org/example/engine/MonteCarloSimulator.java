package org.example.engine;

import org.example.core.RandomGenerator;
import org.example.domain.HouseholdState;
import org.example.domain.MarketParameters;
import org.example.domain.SimulationResult;
import org.example.domain.UserProfile;
import org.example.tax.DanishTaxEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * High-performance Monte Carlo simulator leveraging Java 21 Project Loom virtual threads
 * for concurrent trajectory generation.
 */
public class MonteCarloSimulator {
    public static final String MILESTONE_500K = "Liquid_500k_DKK";
    public static final String MILESTONE_1M = "Liquid_1M_DKK";
    public static final String MILESTONE_FIRE = "Financial_Independence_FIRE";

    private final UserProfile profile;
    private final MarketParameters marketParams;
    private final DanishTaxEngine taxEngine;

    public MonteCarloSimulator(
        UserProfile profile,
        MarketParameters marketParams,
        DanishTaxEngine taxEngine
    ) {
        this.profile = profile;
        this.marketParams = marketParams;
        this.taxEngine = taxEngine;
    }

    /**
     * Executes N Monte Carlo simulation paths over H months concurrently using Virtual Threads.
     *
     * @param initialState initial state vector at month 0
     * @param pathCount    total number of stochastic trajectories to simulate
     * @param horizonMonths total simulation horizon in months (e.g. 240 for 20 years)
     * @param baseSeed     random seed for reproducibility
     * @return aggregate simulation result
     */
    public SimulationResult runSimulation(
        HouseholdState initialState,
        int pathCount,
        int horizonMonths,
        long baseSeed
    ) {
        RandomGenerator masterRng = RandomGenerator.create(baseSeed);
        List<Callable<PathExecutionResult>> tasks = new ArrayList<>(pathCount);

        for (int i = 0; i < pathCount; i++) {
            RandomGenerator pathRng = masterRng.split();
            tasks.add(() -> simulateSinglePath(initialState, horizonMonths, pathRng));
        }

        List<List<HouseholdState>> paths = new ArrayList<>(pathCount);
        double[] tau500k = new double[pathCount];
        double[] tau1M = new double[pathCount];
        double[] tauFire = new double[pathCount];

        // Execute using Project Loom Virtual Threads
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<PathExecutionResult>> futures = executor.invokeAll(tasks);
            for (int i = 0; i < pathCount; i++) {
                PathExecutionResult res = futures.get(i).get();
                paths.add(res.trajectory);
                tau500k[i] = res.tau500k;
                tau1M[i] = res.tau1M;
                tauFire[i] = res.tauFire;
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Monte Carlo simulation interrupted", e);
        }

        Map<String, double[]> stoppingTimes = new HashMap<>();
        stoppingTimes.put(MILESTONE_500K, tau500k);
        stoppingTimes.put(MILESTONE_1M, tau1M);
        stoppingTimes.put(MILESTONE_FIRE, tauFire);

        return new SimulationResult(paths, stoppingTimes, pathCount, horizonMonths);
    }

    private PathExecutionResult simulateSinglePath(
        HouseholdState initialState,
        int horizonMonths,
        RandomGenerator rng
    ) {
        MonthStepEngine engine = new MonthStepEngine(profile, marketParams, taxEngine);
        List<HouseholdState> trajectory = new ArrayList<>(horizonMonths + 1);

        HouseholdState current = initialState.copy();
        trajectory.add(current);

        double tau500k = -1.0;
        double tau1M = -1.0;
        double tauFire = -1.0;

        // Check if milestones are already met at month 0
        if (current.getLiquidNetWorth() >= 500_000.0) {
            tau500k = 0.0;
        }
        if (current.getLiquidNetWorth() >= 1_000_000.0) {
            tau1M = 0.0;
        }
        double fireTargetCapital = (profile.monthlyFixedExpenses() * 12.0) / 0.04; // 4% rule
        if (current.getLiquidNetWorth() >= fireTargetCapital) {
            tauFire = 0.0;
        }

        for (int m = 1; m <= horizonMonths; m++) {
            double zEquity = rng.nextGaussian();
            double zHousing = rng.nextGaussian();
            double zRate = rng.nextGaussian();

            current = engine.step(current, zEquity, zHousing, zRate);
            trajectory.add(current);

            double liquidWorth = current.getLiquidNetWorth();

            if (tau500k < 0.0 && liquidWorth >= 500_000.0) {
                tau500k = m;
            }
            if (tau1M < 0.0 && liquidWorth >= 1_000_000.0) {
                tau1M = m;
            }
            if (tauFire < 0.0 && liquidWorth >= fireTargetCapital) {
                tauFire = m;
            }
        }

        return new PathExecutionResult(trajectory, tau500k, tau1M, tauFire);
    }

    private record PathExecutionResult(
        List<HouseholdState> trajectory,
        double tau500k,
        double tau1M,
        double tauFire
    ) {}
}
