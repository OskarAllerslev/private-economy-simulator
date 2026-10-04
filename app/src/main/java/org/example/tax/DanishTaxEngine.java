package org.example.tax;

/**
 * Calculation engine for Danish personal and investment taxation:
 * <ul>
 *   <li>Arbejdsmarkedsbidrag (AM-bidrag: 8%)</li>
 *   <li>A-skat with municipal tax, bottom tax, personal allowance, and top-bracket tax (topskat)</li>
 *   <li>Mortgage interest tax deduction (rentefradrag)</li>
 *   <li>Investment tax (Aktiesparekonto 17% flat vs. Aktieindkomst 27%/42%)</li>
 * </ul>
 */
public class DanishTaxEngine {

    public static final double DEFAULT_AM_BIDRAG_RATE = 0.08;
    public static final double DEFAULT_MUNICIPAL_TAX_RATE = 0.2490; // Average Danish municipal tax
    public static final double DEFAULT_BOTTOM_TAX_RATE = 0.1206;    // Bundskat
    public static final double DEFAULT_TOP_TAX_RATE = 0.15;         // Topskat
    public static final double DEFAULT_TOP_TAX_THRESHOLD_ANNUAL = 640_100.0; // Topskattegrænse after AM
    public static final double DEFAULT_PERSONAL_ALLOWANCE_ANNUAL = 51_600.0; // Personfradrag
    public static final double DEFAULT_INTEREST_DEDUCTION_HIGH_RATE = 0.336; // Under 50k kr
    public static final double DEFAULT_INTEREST_DEDUCTION_LOW_RATE = 0.256;  // Above 50k kr
    public static final double DEFAULT_INTEREST_DEDUCTION_THRESHOLD = 50_000.0;

    private final double municipalTaxRate;
    private final double bottomTaxRate;
    private final double topTaxRate;
    private final double topTaxThresholdAnnual;
    private final double personalAllowanceAnnual;

    public DanishTaxEngine() {
        this(
            DEFAULT_MUNICIPAL_TAX_RATE,
            DEFAULT_BOTTOM_TAX_RATE,
            DEFAULT_TOP_TAX_RATE,
            DEFAULT_TOP_TAX_THRESHOLD_ANNUAL,
            DEFAULT_PERSONAL_ALLOWANCE_ANNUAL
        );
    }

    public DanishTaxEngine(
        double municipalTaxRate,
        double bottomTaxRate,
        double topTaxRate,
        double topTaxThresholdAnnual,
        double personalAllowanceAnnual
    ) {
        this.municipalTaxRate = municipalTaxRate;
        this.bottomTaxRate = bottomTaxRate;
        this.topTaxRate = topTaxRate;
        this.topTaxThresholdAnnual = topTaxThresholdAnnual;
        this.personalAllowanceAnnual = personalAllowanceAnnual;
    }

    /**
     * Calculates 8% AM-bidrag on gross employment income.
     */
    public double calculateAmBidrag(double grossIncome) {
        return Math.max(0.0, grossIncome * DEFAULT_AM_BIDRAG_RATE);
    }

    /**
     * Calculates monthly net earned income after AM-bidrag, A-skat, and interest deductions.
     *
     * @param monthlyGrossSalary gross monthly salary
     * @param monthlyInterestExpense monthly mortgage interest and fees eligible for rentefradrag
     * @return record with gross salary, AM-bidrag, A-skat, interest tax savings, and take-home pay
     */
    public MonthlyTaxBreakdown calculateMonthlySalaryTax(double monthlyGrossSalary, double monthlyInterestExpense) {
        double amBidrag = calculateAmBidrag(monthlyGrossSalary);
        double taxableIncomeBase = monthlyGrossSalary - amBidrag;

        double monthlyPersonalAllowance = personalAllowanceAnnual / 12.0;
        double monthlyBaseTaxable = Math.max(0.0, taxableIncomeBase - monthlyPersonalAllowance);

        // Combined municipal + bottom tax rate
        double baseTaxRate = municipalTaxRate + bottomTaxRate;
        double baseTax = monthlyBaseTaxable * baseTaxRate;

        // Top tax calculation (annualized rate check)
        double annualizedTaxable = taxableIncomeBase * 12.0;
        double annualTopTax = 0.0;
        if (annualizedTaxable > topTaxThresholdAnnual) {
            annualTopTax = (annualizedTaxable - topTaxThresholdAnnual) * topTaxRate;
        }
        double monthlyTopTax = annualTopTax / 12.0;

        // Interest tax deduction (rentefradrag)
        double monthlyInterestSavings = calculateMonthlyInterestTaxRelief(monthlyInterestExpense);

        double totalMonthlyTax = Math.max(0.0, amBidrag + baseTax + monthlyTopTax - monthlyInterestSavings);
        double netTakeHomePay = monthlyGrossSalary - totalMonthlyTax;

        return new MonthlyTaxBreakdown(
            monthlyGrossSalary,
            amBidrag,
            baseTax + monthlyTopTax,
            monthlyInterestSavings,
            totalMonthlyTax,
            netTakeHomePay
        );
    }

    /**
     * Calculates monthly tax relief from mortgage interest payments.
     */
    public double calculateMonthlyInterestTaxRelief(double monthlyInterest) {
        if (monthlyInterest <= 0.0) {
            return 0.0;
        }
        double annualInterest = monthlyInterest * 12.0;
        double highBracketPart = Math.min(annualInterest, DEFAULT_INTEREST_DEDUCTION_THRESHOLD);
        double lowBracketPart = Math.max(0.0, annualInterest - DEFAULT_INTEREST_DEDUCTION_THRESHOLD);

        double annualTaxRelief = (highBracketPart * DEFAULT_INTEREST_DEDUCTION_HIGH_RATE)
            + (lowBracketPart * DEFAULT_INTEREST_DEDUCTION_LOW_RATE);

        return annualTaxRelief / 12.0;
    }

    /**
     * Calculates tax on investment returns for a given investment type.
     *
     * @param annualReturn taxable gain in DKK
     * @param taxType tax scheme
     * @return tax liability in DKK (returns 0 if gain is negative)
     */
    public double calculateInvestmentTax(double annualReturn, InvestmentTaxType taxType) {
        if (annualReturn <= 0.0) {
            return 0.0;
        }

        return switch (taxType) {
            case ASK_17_LAGER -> annualReturn * taxType.getLowerRate();
            case AKTIEINDKOMST_REALISATION, AKTIEINDKOMST_LAGER -> {
                double threshold = taxType.getProgressionThreshold();
                if (annualReturn <= threshold) {
                    yield annualReturn * taxType.getLowerRate();
                } else {
                    double lowTax = threshold * taxType.getLowerRate();
                    double highTax = (annualReturn - threshold) * taxType.getHigherRate();
                    yield lowTax + highTax;
                }
            }
            case KAPITALINDKOMST -> annualReturn * taxType.getLowerRate();
        };
    }

    public record MonthlyTaxBreakdown(
        double grossSalary,
        double amBidrag,
        double aSkat,
        double interestDeductionRelief,
        double totalTaxPaid,
        double netTakeHomePay
    ) {}
}
