package org.example.finance;

/**
 * Interface representing a financial or real asset owned by a household.
 */
public interface Asset {
    /**
     * Returns the human-readable identifier or ticker for the asset.
     */
    String getName();

    /**
     * Returns the current market valuation of the asset in DKK.
     */
    double getCurrentValue();
}
