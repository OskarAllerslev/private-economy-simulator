package org.example.domain;

import org.example.finance.EtfHolding;
import org.example.finance.RealEstate;

/**
 * State vector of the household economy at discrete time step t (month).
 */
public class HouseholdState {
    private final int month;
    private double cash;
    private final EtfHolding etfHolding;
    private final RealEstate realEstate;
    private double currentShortRate;
    private double monthlyTakeHomeIncome;
    private double monthlyLivingExpenses;
    private double monthlyFinanceCost;
    private double monthlyInvestedAmount;

    public HouseholdState(
        int month,
        double cash,
        EtfHolding etfHolding,
        RealEstate realEstate,
        double currentShortRate
    ) {
        this.month = month;
        this.cash = cash;
        this.etfHolding = etfHolding;
        this.realEstate = realEstate;
        this.currentShortRate = currentShortRate;
    }

    /**
     * Total net worth (Samlet formue) = Cash + Liquid equities + Real estate equity.
     */
    public double getTotalNetWorth() {
        double etfVal = (etfHolding != null) ? etfHolding.getCurrentValue() : 0.0;
        double housingEquity = (realEstate != null) ? realEstate.getHomeEquity() : 0.0;
        return cash + etfVal + housingEquity;
    }

    /**
     * Liquid net worth (Likvid formue) = Cash + ETF holdings.
     */
    public double getLiquidNetWorth() {
        double etfVal = (etfHolding != null) ? etfHolding.getCurrentValue() : 0.0;
        return cash + etfVal;
    }

    /**
     * Outstanding mortgage debt balance.
     */
    public double getMortgageDebt() {
        if (realEstate == null || realEstate.getMortgage() == null) {
            return 0.0;
        }
        return realEstate.getMortgage().getPrincipal();
    }

    public int getMonth() {
        return month;
    }

    public double getCash() {
        return cash;
    }

    public void setCash(double cash) {
        this.cash = cash;
    }

    public EtfHolding getEtfHolding() {
        return etfHolding;
    }

    public RealEstate getRealEstate() {
        return realEstate;
    }

    public double getCurrentShortRate() {
        return currentShortRate;
    }

    public void setCurrentShortRate(double currentShortRate) {
        this.currentShortRate = currentShortRate;
    }

    public double getMonthlyTakeHomeIncome() {
        return monthlyTakeHomeIncome;
    }

    public void setMonthlyTakeHomeIncome(double monthlyTakeHomeIncome) {
        this.monthlyTakeHomeIncome = monthlyTakeHomeIncome;
    }

    public double getMonthlyLivingExpenses() {
        return monthlyLivingExpenses;
    }

    public void setMonthlyLivingExpenses(double monthlyLivingExpenses) {
        this.monthlyLivingExpenses = monthlyLivingExpenses;
    }

    public double getMonthlyFinanceCost() {
        return monthlyFinanceCost;
    }

    public void setMonthlyFinanceCost(double monthlyFinanceCost) {
        this.monthlyFinanceCost = monthlyFinanceCost;
    }

    public double getMonthlyInvestedAmount() {
        return monthlyInvestedAmount;
    }

    public void setMonthlyInvestedAmount(double monthlyInvestedAmount) {
        this.monthlyInvestedAmount = monthlyInvestedAmount;
    }

    /**
     * Creates a deep copy of this household state for path simulation.
     */
    public HouseholdState copy() {
        EtfHolding copiedEtf = (this.etfHolding != null) ? this.etfHolding.copy() : null;
        RealEstate copiedRealEstate = (this.realEstate != null) ? this.realEstate.copy() : null;
        HouseholdState copy = new HouseholdState(
            this.month,
            this.cash,
            copiedEtf,
            copiedRealEstate,
            this.currentShortRate
        );
        copy.monthlyTakeHomeIncome = this.monthlyTakeHomeIncome;
        copy.monthlyLivingExpenses = this.monthlyLivingExpenses;
        copy.monthlyFinanceCost = this.monthlyFinanceCost;
        copy.monthlyInvestedAmount = this.monthlyInvestedAmount;
        return copy;
    }
}
