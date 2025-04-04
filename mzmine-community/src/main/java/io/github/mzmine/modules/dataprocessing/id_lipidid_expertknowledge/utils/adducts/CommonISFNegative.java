package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts;

public enum CommonISFNegative implements ExpertKnowledge {
    // ESI(-) In-source fragmentation
    M_PLUS_CH3COO_MINUS_CH3COOCH3("[M+CH3COO-CH3COOCH3]-", -15.01090);

    /**
     * Specifies the formula
     */
    private final String name;

    /**
     * Specifies the m/z difference
     */
    private final double mz;

    CommonISFNegative(String name, double mz) {
        this.name = name;
        this.mz = mz;
    }

    /**
     * Gets a copy of the name
     * @return adduct name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Gets a copy of the m/z
     * @return adduct m/z
     */
    @Override
    public double getMz() {
        return mz;
    }
}
