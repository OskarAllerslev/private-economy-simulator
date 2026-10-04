package org.example.finance;

/**
 * Represents a residential property (Ejerbolig).
 * Tracks property valuation, home equity (friværdi), maintenance costs (vedligehold),
 * and Danish property taxes (ejendomsværdiskat and grundskyld).
 */
public class RealEstate implements Asset {
    private final String name;
    private double marketValue;
    private final double annualMaintenanceRate; // e.g. 0.01 (1.0% per year)
    private final double annualPropertyTaxRate;   // ejendomsskat + grundskyld (e.g. 0.011)
    private Mortgage mortgage;

    public RealEstate(
        String name,
        double initialMarketValue,
        double annualMaintenanceRate,
        double annualPropertyTaxRate,
        Mortgage mortgage
    ) {
        this.name = name;
        this.marketValue = initialMarketValue;
        this.annualMaintenanceRate = annualMaintenanceRate;
        this.annualPropertyTaxRate = annualPropertyTaxRate;
        this.mortgage = mortgage;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getCurrentValue() {
        return marketValue;
    }

    /**
     * Home equity (friværdi) = Current market value - remaining mortgage debt.
     */
    public double getHomeEquity() {
        double debt = (mortgage != null) ? mortgage.getPrincipal() : 0.0;
        return marketValue - debt;
    }

    /**
     * Loan-to-Value (LTV / belåningsgrad) = Mortgage debt / Current market value.
     */
    public double getLoanToValue() {
        if (marketValue <= 0.0) {
            return 0.0;
        }
        double debt = (mortgage != null) ? mortgage.getPrincipal() : 0.0;
        return debt / marketValue;
    }

    public double getMonthlyMaintenanceCost() {
        return (marketValue * annualMaintenanceRate) / 12.0;
    }

    public double getMonthlyPropertyTax() {
        return (marketValue * annualPropertyTaxRate) / 12.0;
    }

    public void updateMarketValue(double newMarketValue) {
        this.marketValue = Math.max(0.0, newMarketValue);
    }

    public Mortgage getMortgage() {
        return mortgage;
    }

    public void setMortgage(Mortgage mortgage) {
        this.mortgage = mortgage;
    }

    public double getAnnualMaintenanceRate() {
        return annualMaintenanceRate;
    }

    public double getAnnualPropertyTaxRate() {
        return annualPropertyTaxRate;
    }

    public RealEstate copy() {
        Mortgage copiedMortgage = (this.mortgage != null) ? this.mortgage.copy() : null;
        return new RealEstate(
            this.name,
            this.marketValue,
            this.annualMaintenanceRate,
            this.annualPropertyTaxRate,
            copiedMortgage
        );
    }
}
