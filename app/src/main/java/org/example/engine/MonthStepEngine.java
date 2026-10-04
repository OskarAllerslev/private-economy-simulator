package org.example.engine;

import org.example.core.GeometricBrownianMotion;
import org.example.core.VasicekProcess;
import org.example.domain.HouseholdState;
import org.example.domain.MarketParameters;
import org.example.domain.UserProfile;
import org.example.finance.EtfHolding;
import org.example.finance.Mortgage;
import org.example.finance.RealEstate;
import org.example.tax.DanishTaxEngine;

/**
 * State transition operator: t -> t + dt (1 month).
 * Updates asset market valuations, advances mortgage amortization, computes Danish income and investment taxes,
 * settles property taxes and maintenance, and allocates monthly net savings.
 */
public class MonthStepEngine {
    private static final double DT = 1.0 / 12.0; // 1 month

    private final UserProfile profile;
    private final MarketParameters marketParams;
    private final DanishTaxEngine taxEngine;
    private final GeometricBrownianMotion equityGbm;
    private final GeometricBrownianMotion housingGbm;
    private final VasicekProcess rateProcess;

    public MonthStepEngine(
        UserProfile profile,
        MarketParameters marketParams,
        DanishTaxEngine taxEngine
    ) {
        this.profile = profile;
        this.marketParams = marketParams;
        this.taxEngine = taxEngine;
        this.equityGbm = marketParams.createEquityProcess();
        this.housingGbm = marketParams.createHousingProcess();
        this.rateProcess = marketParams.createShortRateProcess();
    }

    /**
     * Executes a single month transition given standard normal shocks for equity, housing, and interest rates.
     *
     * @param currentState current state at month t
     * @param zEquity      standard normal variate for stock market
     * @param zHousing     standard normal variate for real estate market
     * @param zRate        standard normal variate for interest rate
     * @return new state at month t + 1
     */
    public HouseholdState step(
        HouseholdState currentState,
        double zEquity,
        double zHousing,
        double zRate
    ) {
        HouseholdState next = currentState.copy();
        int nextMonth = currentState.getMonth() + 1;

        // 1. Advance stochastic processes
        double nextShortRate = Math.max(0.0, rateProcess.nextValue(currentState.getCurrentShortRate(), DT, zRate));
        next.setCurrentShortRate(nextShortRate);

        EtfHolding etf = next.getEtfHolding();
        if (etf != null && etf.getUnitPrice() > 0.0) {
            double nextPrice = equityGbm.nextValue(etf.getUnitPrice(), DT, zEquity);
            etf.updatePrice(nextPrice);
        }

        RealEstate realEstate = next.getRealEstate();
        if (realEstate != null && realEstate.getCurrentValue() > 0.0) {
            double nextHouseValue = housingGbm.nextValue(realEstate.getCurrentValue(), DT, zHousing);
            realEstate.updateMarketValue(nextHouseValue);
        }

        // 2. Advance mortgage amortization
        double mortgagePaymentTotal = 0.0;
        double taxDeductibleInterest = 0.0;
        if (realEstate != null && realEstate.getMortgage() != null) {
            Mortgage.MonthlyPayment mPayment = realEstate.getMortgage().stepMonth(nextShortRate);
            mortgagePaymentTotal = mPayment.totalPayment();
            taxDeductibleInterest = mPayment.totalTaxDeductibleFinanceCosts();
        }

        // 3. Compute earned income adjusted for wage inflation and taxes
        double yearFraction = nextMonth / 12.0;
        double currentGrossSalary = profile.monthlyGrossSalary() * Math.pow(1.0 + profile.annualSalaryGrowthRate(), yearFraction);
        DanishTaxEngine.MonthlyTaxBreakdown taxBreakdown = taxEngine.calculateMonthlySalaryTax(currentGrossSalary, taxDeductibleInterest);
        double takeHomeSalary = taxBreakdown.netTakeHomePay();

        // 4. Property maintenance & property tax
        double maintenanceCost = (realEstate != null) ? realEstate.getMonthlyMaintenanceCost() : 0.0;
        double propertyTax = (realEstate != null) ? realEstate.getMonthlyPropertyTax() : 0.0;

        // 5. Living expenses (inflated with CPI)
        double currentLivingExpenses = profile.monthlyFixedExpenses() * Math.pow(1.0 + marketParams.inflationRate(), yearFraction);

        // 6. Annual investment tax settlement (mark-to-market / lagerbeskatning in December)
        double investmentTaxDue = 0.0;
        if (nextMonth % 12 == 0 && etf != null && etf.getTaxType().isMarkToMarket()) {
            double annualGain = etf.settleAnnualMarkToMarketGain();
            investmentTaxDue = taxEngine.calculateInvestmentTax(annualGain, etf.getTaxType());
        }

        // 7. Cash interest earned on cash balance
        double monthlyCashInterest = next.getCash() * (marketParams.riskFreeRate() / 12.0);
        double availableCash = next.getCash() + monthlyCashInterest + takeHomeSalary;

        // Total outlays
        double totalOutflow = currentLivingExpenses + mortgagePaymentTotal + maintenanceCost + propertyTax + investmentTaxDue;
        double netCashFlow = availableCash - totalOutflow;

        // 8. Cash allocation & Portfolio rebalancing
        double targetCashBuffer = profile.targetEmergencyFund();
        double investedThisMonth = 0.0;

        if (netCashFlow >= targetCashBuffer) {
            // Cash buffer fulfilled, invest excess in ETF
            double investableSurplus = netCashFlow - targetCashBuffer;
            next.setCash(targetCashBuffer);
            if (etf != null && investableSurplus > 0.0 && etf.getUnitPrice() > 0.0) {
                etf.buyAmount(investableSurplus, etf.getUnitPrice());
                investedThisMonth = investableSurplus;
            }
        } else if (netCashFlow >= 0.0) {
            // Buffer partially filled, no investing
            next.setCash(netCashFlow);
        } else {
            // Net cash deficit (netCashFlow < 0) -> liquidate ETF shares to cover deficit
            double deficit = -netCashFlow;
            next.setCash(0.0);
            if (etf != null && etf.getUnitPrice() > 0.0) {
                double liquidated = etf.sellForCash(deficit, etf.getUnitPrice());
                double remainingDeficit = deficit - liquidated;
                if (remainingDeficit > 0.0) {
                    // Overdraft / negative cash
                    next.setCash(-remainingDeficit);
                }
            } else {
                next.setCash(netCashFlow);
            }
        }

        // 9. Store tracking metrics on the state
        HouseholdState updatedState = new HouseholdState(
            nextMonth,
            next.getCash(),
            etf,
            realEstate,
            nextShortRate
        );
        updatedState.setMonthlyTakeHomeIncome(takeHomeSalary);
        updatedState.setMonthlyLivingExpenses(currentLivingExpenses);
        updatedState.setMonthlyFinanceCost(mortgagePaymentTotal);
        updatedState.setMonthlyInvestedAmount(investedThisMonth);

        return updatedState;
    }
}
