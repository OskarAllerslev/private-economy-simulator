package org.example.tax;

/**
 * Danish tax categorization for investment vehicles:
 * <ul>
 *   <li>{@link #ASK_17_LAGER}: Aktiesparekonto - 17% mark-to-market (lagerbeskatning).</li>
 *   <li>{@link #AKTIEINDKOMST_REALISATION}: Free funds - Realization principle (27% under threshold, 42% above).</li>
 *   <li>{@link #AKTIEINDKOMST_LAGER}: Free funds - Mark-to-market for ETFs on Skat's positive list (e.g. LYPS).</li>
 *   <li>{@link #KAPITALINDKOMST}: Capital income taxation.</li>
 * </ul>
 */
public enum InvestmentTaxType {
    /**
     * Aktiesparekonto (ASK): Flat 17% annual mark-to-market taxation.
     */
    ASK_17_LAGER(0.17, 0.17, 0.0, true),

    /**
     * Aktieindkomst realization principle: 27% up to progression threshold, 42% above.
     */
    AKTIEINDKOMST_REALISATION(0.27, 0.42, 61_000.0, false),

    /**
     * Aktieindkomst mark-to-market (lagerbeskatning): Typical for UCITS ETFs on Danish positive list (e.g. LYPS).
     * 27% up to progression threshold, 42% above, settled annually.
     */
    AKTIEINDKOMST_LAGER(0.27, 0.42, 61_000.0, true),

    /**
     * Kapitalindkomst: Flat / standard rate assumption (approx 37%).
     */
    KAPITALINDKOMST(0.37, 0.37, 0.0, true);

    private final double lowerRate;
    private final double higherRate;
    private final double progressionThreshold;
    private final boolean markToMarket;

    InvestmentTaxType(double lowerRate, double higherRate, double progressionThreshold, boolean markToMarket) {
        this.lowerRate = lowerRate;
        this.higherRate = higherRate;
        this.progressionThreshold = progressionThreshold;
        this.markToMarket = markToMarket;
    }

    public double getLowerRate() {
        return lowerRate;
    }

    public double getHigherRate() {
        return higherRate;
    }

    public double getProgressionThreshold() {
        return progressionThreshold;
    }

    public boolean isMarkToMarket() {
        return markToMarket;
    }
}
