package org.example;

import org.example.analytics.PercentileSummary;
import org.example.analytics.StoppingTimeCalculator;
import org.example.analytics.UtilityAnalyzer;
import org.example.domain.HouseholdState;
import org.example.domain.MarketParameters;
import org.example.domain.SimulationResult;
import org.example.domain.UserProfile;
import org.example.engine.MonteCarloSimulator;
import org.example.finance.EtfHolding;
import org.example.finance.Mortgage;
import org.example.finance.RealEstate;
import org.example.tax.DanishTaxEngine;
import org.example.tax.InvestmentTaxType;

import java.util.Locale;

/**
 * Main entry point and CLI for the Danish Private Economy Simulator.
 */
public class App {

    public String getGreeting() {
        return "Private Economy Simulator (Danish Household Model) Initialized.";
    }

    private static String formatDkk(double amount) {
        return String.format(Locale.GERMANY, "%,.0f DKK", amount);
    }

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("              DANISH PRIVATE ECONOMY SIMULATOR (MONTE CARLO)              ");
        System.out.println("==========================================================================");

        // 1. Setup User Profile
        // Gross income: 45,000 DKK/month, living costs: 15,000 DKK/month, risk aversion gamma = 2.0
        UserProfile profile = new UserProfile(
            45_000.0,
            15_000.0,
            0.02, // 2% wage drift
            2.0,  // CRRA gamma
            50_000.0 // 50k emergency cash buffer target
        );

        // 2. Setup Market Parameters
        MarketParameters marketParams = MarketParameters.createStandard();

        // 3. Setup Initial Balance Sheet
        // LYPS ETF: 100,000 DKK invested at 500 DKK/unit (200 units) under Aktieindkomst lagerbeskatning
        EtfHolding initialEtf = new EtfHolding("LYPS", 200.0, 500.0, 500.0, InvestmentTaxType.AKTIEINDKOMST_LAGER);

        // Boliglån: 2.400.000 DKK F-kort realkreditlån, 30 år, 10 år afdragsfrihed, bidragssats 0.70%
        Mortgage initialMortgage = Mortgage.createFKort(
            2_400_000.0,
            0.030, // 3.0% initial short rate
            0.007, // 0.70% bidragssats
            30,
            10     // 10 years interest-only
        );

        // Ejerbolig: 3.000.000 DKK vurdering
        RealEstate initialHouse = new RealEstate(
            "Ejerbolig",
            3_000_000.0,
            0.010, // 1% annual maintenance
            0.011, // Ejendomsskat + grundskyld
            initialMortgage
        );

        HouseholdState initialState = new HouseholdState(
            0,
            50_000.0, // 50k cash buffer
            initialEtf,
            initialHouse,
            0.030
        );

        DanishTaxEngine taxEngine = new DanishTaxEngine();

        System.out.println("\n[1] INITIAL HOUSEHOLD BALANCE SHEET (t = 0):");
        System.out.printf("  - Kontantbuffer:          %s%n", formatDkk(initialState.getCash()));
        System.out.printf("  - LYPS ETF Portefolje:    %s (%s andele @ %s kr)%n",
            formatDkk(initialEtf.getCurrentValue()),
            (int) initialEtf.getUnits(),
            (int) initialEtf.getUnitPrice());
        System.out.printf("  - Ejerbolig vaerdi:       %s%n", formatDkk(initialHouse.getCurrentValue()));
        System.out.printf("  - Realkredit restgaeld:   %s (%s, 10 aar afdragsfrihed)%n",
            formatDkk(initialMortgage.getPrincipal()),
            initialMortgage.getType());
        System.out.printf("  - Frivaerdi (bolig):      %s (LTV: %.1f%%)%n",
            formatDkk(initialHouse.getHomeEquity()),
            initialHouse.getLoanToValue() * 100.0);
        System.out.printf("  - Likvid Formue:          %s%n", formatDkk(initialState.getLiquidNetWorth()));
        System.out.printf("  - Samlet Nettoformue:     %s%n", formatDkk(initialState.getTotalNetWorth()));

        // 4. Run Monte Carlo Simulation using Project Loom
        int paths = 2_500;
        int years = 20;
        int horizonMonths = years * 12;
        System.out.printf("%n[2] RUNNING MONTE CARLO SIMULATION (%d paths, %d aar) via Project Loom...%n", paths, years);

        long startTime = System.currentTimeMillis();
        MonteCarloSimulator simulator = new MonteCarloSimulator(profile, marketParams, taxEngine);
        SimulationResult result = simulator.runSimulation(initialState, paths, horizonMonths, 42L);
        long elapsedMs = System.currentTimeMillis() - startTime;
        System.out.printf("  Execution completed in %d ms.%n", elapsedMs);

        // 5. Milestone Analysis (Stopping Times tau)
        System.out.println("\n[3] FINANCIAL MILESTONES & STOPPING TIMES (tau):");
        printStoppingTime(result, MonteCarloSimulator.MILESTONE_500K, "Ram 500.000 DKK likvid formue");
        printStoppingTime(result, MonteCarloSimulator.MILESTONE_1M, "Ram 1.000.000 DKK likvid formue");
        printStoppingTime(result, MonteCarloSimulator.MILESTONE_FIRE, "Finansiel Uafhaengighed (FIRE / 4%-regel)");

        // 6. Net Worth Progression Percentiles
        System.out.println("\n[4] TOTAL NET WORTH CONFIDENCE BANDS (DKK):");
        System.out.println("  Aar  |      P10 (Bear)      |     P50 (Median)     |      P90 (Bull)      |     Gennemsnit");
        System.out.println("-----------------------------------------------------------------------------------------");

        int[] sampleYears = {1, 3, 5, 10, 15, 20};
        for (int y : sampleYears) {
            int month = y * 12;
            double[] nwAtMonth = result.getNetWorthsAtMonth(month);
            var band = PercentileSummary.computeBand(nwAtMonth);
            System.out.printf("  Aar %-2d| %20s | %20s | %20s | %18s%n",
                y,
                formatDkk(band.p10()),
                formatDkk(band.p50()),
                formatDkk(band.p90()),
                formatDkk(band.mean()));
        }

        // 7. Liquid Net Worth Progression Percentiles
        System.out.println("\n[5] LIQUID NET WORTH (CASH + LYPS ETF) CONFIDENCE BANDS:");
        System.out.println("  Aar  |      P10 (Bear)      |     P50 (Median)     |      P90 (Bull)      |     Gennemsnit");
        System.out.println("-----------------------------------------------------------------------------------------");
        for (int y : sampleYears) {
            int month = y * 12;
            double[] liquidAtMonth = result.getLiquidNetWorthsAtMonth(month);
            var band = PercentileSummary.computeBand(liquidAtMonth);
            System.out.printf("  Aar %-2d| %20s | %20s | %20s | %18s%n",
                y,
                formatDkk(band.p10()),
                formatDkk(band.p50()),
                formatDkk(band.p90()),
                formatDkk(band.mean()));
        }

        // 8. Utility & Certainty Equivalent Analysis
        System.out.println("\n[6] CRRA UTILITY & CERTAINTY EQUIVALENT (gamma = " + profile.riskAversionGamma() + "):");
        var utilityReport = UtilityAnalyzer.evaluateTerminal(result, profile.riskAversionGamma());
        System.out.printf("  - Forventet Slutformue E[W]:     %s%n", formatDkk(utilityReport.expectedWealth()));
        System.out.printf("  - Certainty Equivalent (CE):     %s%n", formatDkk(utilityReport.certaintyEquivalent()));
        System.out.printf("  - Risikopraemie (E[W] - CE):     %s (%.1f%% af E[W])%n",
            formatDkk(utilityReport.riskPremium()),
            (utilityReport.riskPremium() / utilityReport.expectedWealth()) * 100.0);

        System.out.println("\n==========================================================================");
        System.out.println("                       SIMULATION COMPLETED SUCCESSFULLY                  ");
        System.out.println("==========================================================================");
    }

    private static void printStoppingTime(SimulationResult result, String milestoneKey, String label) {
        double[] times = result.getStoppingTimes(milestoneKey);
        if (times == null) {
            return;
        }
        var summary = StoppingTimeCalculator.computeSummary(label, times);
        if (summary.probabilityOfReaching() > 0.0) {
            System.out.printf("  * %-40s -> Sandsynlighed: %5.1f%% | Median: %4.1f aar (P10: %4.1f aar, P90: %4.1f aar)%n",
                label,
                summary.probabilityOfReaching() * 100.0,
                summary.medianYears(),
                summary.p10Months() / 12.0,
                summary.p90Months() / 12.0
            );
        } else {
            System.out.printf("  * %-40s -> Ikke opnaaet inden for tidshorisonten%n", label);
        }
    }
}
