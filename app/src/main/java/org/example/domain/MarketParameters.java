package org.example.domain;

import org.example.core.GeometricBrownianMotion;
import org.example.core.VasicekProcess;

/**
 * Global macroeconomic and capital market parameters.
 *
 * @param equityExpectedReturn  annual expected return (drift mu) for stocks/ETF (e.g. 0.07 = 7%)
 * @param equityVolatility      annual volatility (sigma) for stocks/ETF (e.g. 0.16 = 16%)
 * @param housingExpectedGrowth annual expected growth for real estate (e.g. 0.03 = 3%)
 * @param housingVolatility     annual volatility for real estate (e.g. 0.08 = 8%)
 * @param riskFreeRate          annual deposit interest rate on cash balances (e.g. 0.02 = 2%)
 * @param inflationRate         annual expected inflation rate (e.g. 0.02 = 2%)
 * @param vasicekSpeed          Vasicek mean reversion parameter 'a' (e.g. 0.20)
 * @param vasicekLongTermRate   Vasicek long term equilibrium rate 'b' (e.g. 0.03 = 3%)
 * @param vasicekVolatility     Vasicek rate volatility 'sigma' (e.g. 0.015 = 1.5%)
 */
public record MarketParameters(
    double equityExpectedReturn,
    double equityVolatility,
    double housingExpectedGrowth,
    double housingVolatility,
    double riskFreeRate,
    double inflationRate,
    double vasicekSpeed,
    double vasicekLongTermRate,
    double vasicekVolatility
) {
    public static MarketParameters createStandard() {
        return new MarketParameters(
            0.075, // Equity mu: 7.5% nominal return
            0.160, // Equity sigma: 16% volatility
            0.035, // Housing mu: 3.5% nominal growth
            0.080, // Housing sigma: 8% volatility
            0.020, // Risk-free cash deposit: 2.0%
            0.020, // Inflation: 2.0%
            0.200, // Vasicek a: speed of mean reversion
            0.030, // Vasicek b: 3.0% long-term short rate
            0.012  // Vasicek sigma: 1.2% short rate vol
        );
    }

    public GeometricBrownianMotion createEquityProcess() {
        return new GeometricBrownianMotion(equityExpectedReturn, equityVolatility);
    }

    public GeometricBrownianMotion createHousingProcess() {
        return new GeometricBrownianMotion(housingExpectedGrowth, housingVolatility);
    }

    public VasicekProcess createShortRateProcess() {
        return new VasicekProcess(vasicekSpeed, vasicekLongTermRate, vasicekVolatility);
    }
}
