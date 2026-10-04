package org.example.finance;

/**
 * Represents a Danish mortgage (realkreditlån / boliglån).
 * Supports fixed rate loans (fast rente) and variable rate loans (F-kort / CITA)
 * with optional interest-only periods (afdragsfrihed) and administration fees (bidragssats).
 */
public class Mortgage {

    public enum MortgageType {
        FIXED_RATE,
        VARIABLE_F_KORT
    }

    private final MortgageType type;
    private double principal; // Restgæld
    private double annualInterestRate; // Nominal annual rate
    private final double annualAdministrationMargin; // Bidragssats
    private int remainingMonths; // Total remaining term in months
    private int interestOnlyMonthsRemaining; // Afdragsfrihed remaining months

    public Mortgage(
        MortgageType type,
        double principal,
        double annualInterestRate,
        double annualAdministrationMargin,
        int remainingMonths,
        int interestOnlyMonthsRemaining
    ) {
        this.type = type;
        this.principal = principal;
        this.annualInterestRate = annualInterestRate;
        this.annualAdministrationMargin = annualAdministrationMargin;
        this.remainingMonths = remainingMonths;
        this.interestOnlyMonthsRemaining = interestOnlyMonthsRemaining;
    }

    public static Mortgage createFixedRate(double principal, double fixedRate, double bidrag, int years, int interestOnlyYears) {
        return new Mortgage(
            MortgageType.FIXED_RATE,
            principal,
            fixedRate,
            bidrag,
            years * 12,
            interestOnlyYears * 12
        );
    }

    public static Mortgage createFKort(double principal, double initialRate, double bidrag, int years, int interestOnlyYears) {
        return new Mortgage(
            MortgageType.VARIABLE_F_KORT,
            principal,
            initialRate,
            bidrag,
            years * 12,
            interestOnlyYears * 12
        );
    }

    /**
     * Advances the mortgage by one month, optionally updating the interest rate if variable.
     *
     * @param currentMarketRate latest short/benchmark rate if variable
     * @return monthly payment breakdown
     */
    public MonthlyPayment stepMonth(double currentMarketRate) {
        if (principal <= 0.0 || remainingMonths <= 0) {
            remainingMonths = Math.max(0, remainingMonths - 1);
            return new MonthlyPayment(0.0, 0.0, 0.0, 0.0, 0.0);
        }

        if (type == MortgageType.VARIABLE_F_KORT) {
            this.annualInterestRate = Math.max(0.0, currentMarketRate);
        }

        double monthlyInterestRate = annualInterestRate / 12.0;
        double monthlyMarginRate = annualAdministrationMargin / 12.0;
        double totalMonthlyRate = monthlyInterestRate + monthlyMarginRate;

        double interestPayment = principal * monthlyInterestRate;
        double marginPayment = principal * monthlyMarginRate;

        double principalRepayment;
        double totalMonthlyPayment;

        if (interestOnlyMonthsRemaining > 0) {
            // Interest-only period (afdragsfrihed)
            principalRepayment = 0.0;
            totalMonthlyPayment = interestPayment + marginPayment;
            interestOnlyMonthsRemaining--;
        } else {
            // Amortization phase: calculate annuity payment over remaining months
            int amortizingMonthsRemaining = remainingMonths;
            if (totalMonthlyRate > 0.0) {
                double factor = Math.pow(1.0 + totalMonthlyRate, amortizingMonthsRemaining);
                totalMonthlyPayment = principal * (totalMonthlyRate * factor) / (factor - 1.0);
            } else {
                totalMonthlyPayment = principal / amortizingMonthsRemaining;
            }

            principalRepayment = Math.max(0.0, totalMonthlyPayment - (interestPayment + marginPayment));
            principalRepayment = Math.min(principalRepayment, principal);
        }

        principal = Math.max(0.0, principal - principalRepayment);
        remainingMonths--;

        return new MonthlyPayment(
            totalMonthlyPayment,
            interestPayment,
            marginPayment,
            principalRepayment,
            principal
        );
    }

    public MortgageType getType() {
        return type;
    }

    public double getPrincipal() {
        return principal;
    }

    public double getAnnualInterestRate() {
        return annualInterestRate;
    }

    public double getAnnualAdministrationMargin() {
        return annualAdministrationMargin;
    }

    public int getRemainingMonths() {
        return remainingMonths;
    }

    public int getInterestOnlyMonthsRemaining() {
        return interestOnlyMonthsRemaining;
    }

    public boolean isInterestOnlyActive() {
        return interestOnlyMonthsRemaining > 0;
    }

    public Mortgage copy() {
        return new Mortgage(
            this.type,
            this.principal,
            this.annualInterestRate,
            this.annualAdministrationMargin,
            this.remainingMonths,
            this.interestOnlyMonthsRemaining
        );
    }

    public record MonthlyPayment(
        double totalPayment,
        double interestAmount,
        double marginAmount,
        double principalRepayment,
        double remainingPrincipal
    ) {
        /**
         * Interest and administration fee are eligible for Danish tax deduction (rentefradrag).
         */
        public double totalTaxDeductibleFinanceCosts() {
            return interestAmount + marginAmount;
        }
    }
}
