package org.example.analytics;

import org.example.domain.SimulationResult;

/**
 * Economic utility analysis based on Constant Relative Risk Aversion (CRRA) preferences:
 * <pre>
 *   u(w) = (w^(1 - gamma)) / (1 - gamma)   for gamma != 1
 *   u(w) = ln(w)                           for gamma == 1
 * </pre>
 * Computes Expected Utility E[u(W)], Certainty Equivalent (CE), and the Risk Premium:
 * <pre>
 *   Risk Premium = E[W] - CE
 * </pre>
 */
public class UtilityAnalyzer {

    /**
     * Evaluates the CRRA utility for a single wealth value.
     */
    public static double crraUtility(double wealth, double gamma) {
        double safeWealth = Math.max(1e-4, wealth); // Protect against non-positive wealth
        if (Math.abs(gamma - 1.0) < 1e-6) {
            return Math.log(safeWealth);
        }
        return Math.pow(safeWealth, 1.0 - gamma) / (1.0 - gamma);
    }

    /**
     * Inverts the CRRA utility function to find the Certainty Equivalent (CE).
     */
    public static double certaintyEquivalent(double expectedUtility, double gamma) {
        if (Math.abs(gamma - 1.0) < 1e-6) {
            return Math.exp(expectedUtility);
        }
        double inner = (1.0 - gamma) * expectedUtility;
        if (inner <= 0.0) {
            return 0.0;
        }
        return Math.pow(inner, 1.0 / (1.0 - gamma));
    }

    /**
     * Conducts comprehensive utility analysis across an array of terminal wealth realizations.
     */
    public static UtilityReport evaluate(double[] wealths, double gamma) {
        if (wealths == null || wealths.length == 0) {
            throw new IllegalArgumentException("Wealth array cannot be null or empty");
        }

        double sumWealth = 0.0;
        double sumUtility = 0.0;

        for (double w : wealths) {
            sumWealth += w;
            sumUtility += crraUtility(w, gamma);
        }

        double expectedWealth = sumWealth / wealths.length;
        double expectedUtility = sumUtility / wealths.length;
        double ce = certaintyEquivalent(expectedUtility, gamma);
        double riskPremium = expectedWealth - ce;

        return new UtilityReport(expectedWealth, expectedUtility, ce, riskPremium, gamma);
    }

    /**
     * Evaluates utility directly on simulation results.
     */
    public static UtilityReport evaluateTerminal(SimulationResult result, double gamma) {
        return evaluate(result.getTerminalNetWorths(), gamma);
    }

    public record UtilityReport(
        double expectedWealth,
        double expectedUtility,
        double certaintyEquivalent,
        double riskPremium,
        double gamma
    ) {}
}
