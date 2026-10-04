package org.example.domain;

/**
 * User profile capturing personal finances, regular expenditure, and risk preferences.
 *
 * @param monthlyGrossSalary      monthly gross salary in DKK
 * @param monthlyFixedExpenses    fixed living costs (rent, food, insurances, etc., excluding mortgage)
 * @param annualSalaryGrowthRate  expected real or nominal annual wage drift (e.g. 0.02)
 * @param riskAversionGamma       CRRA relative risk aversion parameter gamma (e.g. 1.0 to 5.0)
 * @param targetEmergencyFund     minimum cash buffer desired before allocating to equities (DKK)
 */
public record UserProfile(
    double monthlyGrossSalary,
    double monthlyFixedExpenses,
    double annualSalaryGrowthRate,
    double riskAversionGamma,
    double targetEmergencyFund
) {
    public UserProfile {
        if (monthlyGrossSalary < 0.0) {
            throw new IllegalArgumentException("Monthly gross salary cannot be negative: " + monthlyGrossSalary);
        }
        if (monthlyFixedExpenses < 0.0) {
            throw new IllegalArgumentException("Monthly fixed expenses cannot be negative: " + monthlyFixedExpenses);
        }
        if (riskAversionGamma <= 0.0) {
            throw new IllegalArgumentException("Risk aversion gamma must be strictly positive: " + riskAversionGamma);
        }
        if (targetEmergencyFund < 0.0) {
            throw new IllegalArgumentException("Target emergency fund cannot be negative: " + targetEmergencyFund);
        }
    }

    public static UserProfile ofStandardDanishHousehold(double grossSalary, double fixedExpenses, double gamma) {
        return new UserProfile(
            grossSalary,
            fixedExpenses,
            0.02, // 2% annual salary progression
            gamma,
            fixedExpenses * 3.0 // 3 months living expenses emergency buffer
        );
    }
}
