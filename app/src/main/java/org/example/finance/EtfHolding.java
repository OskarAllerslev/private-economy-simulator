package org.example.finance;

import org.example.tax.InvestmentTaxType;

/**
 * Represents an equity ETF holding (e.g. LYPS / Lyxor Core MSCI World UCITS ETF).
 * Tracks number of units, current unit price, and GAK (Gennemsnitlig Anskaffelseskurs).
 */
public class EtfHolding implements Asset {
    private final String name;
    private final InvestmentTaxType taxType;
    private double units;
    private double unitPrice;
    private double averagePurchasePrice; // GAK (Gennemsnitlig Anskaffelseskurs)
    private double yearStartValuation;   // Used for annual mark-to-market (lagerbeskatning)

    public EtfHolding(String name, double units, double unitPrice, double averagePurchasePrice, InvestmentTaxType taxType) {
        this.name = name;
        this.units = units;
        this.unitPrice = unitPrice;
        this.averagePurchasePrice = averagePurchasePrice;
        this.taxType = taxType;
        this.yearStartValuation = units * unitPrice;
    }

    public static EtfHolding ofLyps(double initialUnits, double initialPrice, InvestmentTaxType taxType) {
        return new EtfHolding("LYPS", initialUnits, initialPrice, initialPrice, taxType);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getCurrentValue() {
        return units * unitPrice;
    }

    public double getUnits() {
        return units;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getAveragePurchasePrice() {
        return averagePurchasePrice;
    }

    public InvestmentTaxType getTaxType() {
        return taxType;
    }

    public double getCostBasis() {
        return units * averagePurchasePrice;
    }

    public double getUnrealizedGain() {
        return getCurrentValue() - getCostBasis();
    }

    /**
     * Executes a purchase of additional ETF units and recalculates GAK.
     *
     * @param unitsToBuy count of units purchased
     * @param executionPrice purchase price per unit
     */
    public void buy(double unitsToBuy, double executionPrice) {
        if (unitsToBuy <= 0.0) {
            return;
        }
        double totalCostBefore = this.units * this.averagePurchasePrice;
        double addedCost = unitsToBuy * executionPrice;
        this.units += unitsToBuy;
        this.averagePurchasePrice = (this.units > 0.0) ? (totalCostBefore + addedCost) / this.units : executionPrice;
        this.unitPrice = executionPrice;
    }

    /**
     * Executes a purchase by investing a specific currency amount (DKK).
     *
     * @param amountDkk total investment amount
     * @param executionPrice price per unit
     * @return number of units bought
     */
    public double buyAmount(double amountDkk, double executionPrice) {
        if (amountDkk <= 0.0 || executionPrice <= 0.0) {
            return 0.0;
        }
        double unitsToBuy = amountDkk / executionPrice;
        buy(unitsToBuy, executionPrice);
        return unitsToBuy;
    }

    /**
     * Sells a specified number of units and returns realized capital gain/loss.
     *
     * @param unitsToSell units to liquidate
     * @param executionPrice price per unit
     * @return realized capital gain (can be negative for loss)
     */
    public double sell(double unitsToSell, double executionPrice) {
        if (unitsToSell <= 0.0) {
            return 0.0;
        }
        double actualUnits = Math.min(unitsToSell, this.units);
        double realizedGain = actualUnits * (executionPrice - this.averagePurchasePrice);
        this.units -= actualUnits;
        this.unitPrice = executionPrice;
        if (this.units <= 0.0) {
            this.units = 0.0;
            this.averagePurchasePrice = 0.0;
        }
        return realizedGain;
    }

    /**
     * Liquidates units to raise a targeted cash amount.
     *
     * @param cashTarget cash amount needed
     * @param executionPrice current unit price
     * @return actual cash raised
     */
    public double sellForCash(double cashTarget, double executionPrice) {
        if (cashTarget <= 0.0 || executionPrice <= 0.0 || this.units <= 0.0) {
            return 0.0;
        }
        double unitsNeeded = cashTarget / executionPrice;
        double unitsToSell = Math.min(unitsNeeded, this.units);
        sell(unitsToSell, executionPrice);
        return unitsToSell * executionPrice;
    }

    /**
     * Updates the current market price of the unit.
     */
    public void updatePrice(double newPrice) {
        this.unitPrice = Math.max(0.0, newPrice);
    }

    /**
     * Computes the annual mark-to-market gain for lagerbeskatning and resets year-start valuation.
     */
    public double settleAnnualMarkToMarketGain() {
        double currentValuation = getCurrentValue();
        double gain = currentValuation - yearStartValuation;
        this.yearStartValuation = currentValuation;
        return gain;
    }

    /**
     * Creates a deep copy for simulation branching.
     */
    public EtfHolding copy() {
        EtfHolding copy = new EtfHolding(this.name, this.units, this.unitPrice, this.averagePurchasePrice, this.taxType);
        copy.yearStartValuation = this.yearStartValuation;
        return copy;
    }
}
