package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts;

public enum CommonAdductNegative implements ExpertKnowledge {
    // ESI(-) adducts
    M_MINUS_H("[M-H]-", -1.007276),
    M_MINUS_H_PLUS_CH3COONa("[M-H+(CH3COONa)]-", 80.995794),
    M_PLUS_CH3COO("[M+CH3COO]-", 59.01385),
    M_PLUS_CH3COO_PLUS_CH3COONa("[M+CH3COO+(CH3COONa)]-", 141.01692),
    M_PLUS_CH3COO_PLUS_2CH3COONa("[M+CH3COO+(CH3COONa)2]-", 223.01999),
    M_PLUS_CH3COO_PLUS_3CH3COONa("[M+CH3COO+(CH3COONa)3]-", 305.02306),
    M_PLUS_HCOO("[M+HCOO]-", 45.01740),
    M_PLUS_HCOO_PLUS_CH3COONa("[M+HCOO+(CH3COONa)]-", 127.02047),
    M_PLUS_CL("[M+Cl]-", 34.96885),
    //TODO quitar esto, es una prueba solo
    PRUEBA("Prueba", 164.9206);

    /**
     * Specifies the formula
     */
    private final String name;

    /**
     * Specifies the m/z difference
     */
    private final double mz;

    CommonAdductNegative(String name, double mz) {
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
