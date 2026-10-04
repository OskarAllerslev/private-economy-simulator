# Private Economy Simulator

A Java 21 simulation modeling the dynamics of a private Danish household economy using Monte Carlo simulation, Project Loom virtual threads, Danish tax logic, and stochastic processes.

## Architecture & Project Structure

The codebase is organized under `app/src/main/java/org/example/`:

```
app/src/main/java/org/example/
├── App.java                           # Entry point / CLI demonstrator
├── core/                              # Mathematics & Stochastic Processes
│   ├── StochasticProcess.java         # Interface for 1D continuous-time stochastic processes
│   ├── GeometricBrownianMotion.java   # GBM (stocks e.g. LYPS ETF, housing valuations)
│   ├── VasicekProcess.java            # Interest rate dynamics (variable mortgage rates)
│   └── RandomGenerator.java           # High-performance PRNG & standard normal N(0,1) variates
├── domain/                            # Economic State & Domain Models
│   ├── UserProfile.java               # Household profile (income, expenses, risk aversion gamma)
│   ├── MarketParameters.java          # Macro parameters (mu, sigma, risk-free rate, inflation)
│   ├── HouseholdState.java            # State vector at discrete month step t
│   └── SimulationResult.java          # Aggregate trajectories, stopping times, and percentiles
├── finance/                           # Financial Assets & Liabilities
│   ├── Asset.java                     # Common asset interface
│   ├── EtfHolding.java                # LYPS ETF equity holding (units, GAK, buy/sell)
│   ├── Mortgage.java                  # Danish mortgage (Fixed rate, F-kort, afdragsfrihed)
│   └── RealEstate.java                # Residential property (friværdi, maintenance, property taxes)
├── tax/                               # Danish Taxation Engine
│   ├── DanishTaxEngine.java           # AM-bidrag (8%), A-skat, rentefradrag, ASK & stock taxes
│   └── InvestmentTaxType.java         # Lagerbeskatning (ASK 17%), Aktieindkomst (27/42%)
├── engine/                            # Simulation Kernels
│   ├── MonthStepEngine.java           # Transition operator t -> t + dt (1 month)
│   └── MonteCarloSimulator.java       # Parallelized execution via Project Loom virtual threads
└── analytics/                         # Decision Support & Analytics
    ├── UtilityAnalyzer.java           # CRRA utility function & Certainty Equivalent (CE)
    ├── StoppingTimeCalculator.java    # Calculation of tau (e.g. 500k milestone, FIRE)
    └── PercentileSummary.java         # P10, P50 (median), P90 confidence bands
```

## Prerequisites

- Java 21 or higher

## Getting Started

### Run the Application

Using the Gradle wrapper:

**On Windows:**
```powershell
.\gradlew.bat run
```

**On Linux / macOS:**
```bash
./gradlew run
```

### Build and Test

```bash
.\gradlew.bat build
```

To run tests:

```bash
.\gradlew.bat test
```
