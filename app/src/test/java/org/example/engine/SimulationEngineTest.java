package org.example.engine;

import org.example.domain.HouseholdState;
import org.example.domain.MarketParameters;
import org.example.domain.SimulationResult;
import org.example.domain.UserProfile;
import org.example.finance.EtfHolding;
import org.example.finance.Mortgage;
import org.example.finance.RealEstate;
import org.example.tax.DanishTaxEngine;
import org.example.tax.InvestmentTaxType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulationEngineTest {

    @Test
    void testSingleMonthStepTransitions() {
        UserProfile profile = new UserProfile(40_000.0, 12_000.0, 0.02, 2.0, 30_000.0);
        MarketParameters marketParams = MarketParameters.createStandard();
        DanishTaxEngine taxEngine = new DanishTaxEngine();

        EtfHolding etf = new EtfHolding("LYPS", 100.0, 500.0, 500.0, InvestmentTaxType.AKTIEINDKOMST_LAGER);
        Mortgage mortgage = Mortgage.createFKort(1_500_000.0, 0.03, 0.007, 30, 0);
        RealEstate house = new RealEstate("House", 2_000_000.0, 0.01, 0.011, mortgage);

        HouseholdState s0 = new HouseholdState(0, 30_000.0, etf, house, 0.03);

        MonthStepEngine engine = new MonthStepEngine(profile, marketParams, taxEngine);
        HouseholdState s1 = engine.step(s0, 0.0, 0.0, 0.0);

        assertEquals(1, s1.getMonth());
        assertTrue(s1.getTotalNetWorth() > 0.0);
        assertTrue(s1.getMonthlyTakeHomeIncome() > 0.0);
        assertTrue(s1.getMortgageDebt() < s0.getMortgageDebt(), "Mortgage debt should decrease after amortization");
    }

    @Test
    void testMonteCarloSimulatorRunsAndReproduces() {
        UserProfile profile = UserProfile.ofStandardDanishHousehold(45_000.0, 15_000.0, 2.0);
        MarketParameters marketParams = MarketParameters.createStandard();
        DanishTaxEngine taxEngine = new DanishTaxEngine();

        EtfHolding etf = new EtfHolding("LYPS", 100.0, 500.0, 500.0, InvestmentTaxType.AKTIEINDKOMST_LAGER);
        HouseholdState s0 = new HouseholdState(0, 45_000.0, etf, null, 0.03);

        MonteCarloSimulator simulator = new MonteCarloSimulator(profile, marketParams, taxEngine);

        int paths = 200;
        int months = 24; // 2 years
        long seed = 999L;

        SimulationResult r1 = simulator.runSimulation(s0, paths, months, seed);
        SimulationResult r2 = simulator.runSimulation(s0, paths, months, seed);

        assertEquals(paths, r1.getTotalPaths());
        assertEquals(months, r1.getHorizonMonths());

        // Deterministic reproduction given the same seed
        double[] term1 = r1.getTerminalNetWorths();
        double[] term2 = r2.getTerminalNetWorths();
        assertArrayEquals(term1, term2, 1e-6);
    }
}
