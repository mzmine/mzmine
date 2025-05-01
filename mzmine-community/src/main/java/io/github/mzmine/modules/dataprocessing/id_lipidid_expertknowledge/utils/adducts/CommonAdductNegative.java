package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This enum contains the most common adducts found in lipids according to a CEMBIO paper:
 * Martínez, S., Fernández-García, M., Londoño-Osorio, S., Barbas, C., & Gradillas, A. (2024). Highly reliable LC-MS lipidomics database for efficient human plasma profiling based on NIST SRM 19501. Journal of Lipid Research, 65(11), 100671. https://doi.org/10.1016/j.jlr.2024.100671
 * It represents nine common adducts for ESI-.
 */
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
    M_PLUS_CL("[M+Cl]-", 34.96885);

    /**
     * String that represents the complete formula (eg: "[M+Cl]-").
     */
    private final String name;

    /**
     * Double representing the mass difference between a molecule (M) and the adduct.
     * It accounts for the ions mass and charge.
     */
    private final double mz;

    /**
     * Creates a new CommonAdductNegative with the specified info.
     * @param name The name of the adduct.
     * @param mz The m/z of the adduct.
     */
    CommonAdductNegative(String name, double mz) {
        this.name = name;
        this.mz = mz;
    }

    /**
     * Gets the ion form the name attribute. It returns whatever is between "[M" and "]".
     * Eg: "[M+Cl]-" --> "+Cl"
     * @return The ion from the adduct name.
     */
    @Override
    public String getName() {
        Pattern pattern = Pattern.compile("\\[M([\\+\\-]?[A-Za-z0-9]+(?:\\([A-Za-z0-9]+\\))?(?:[\\+\\-]?[A-Za-z0-9]+(?:\\([A-Za-z0-9]+\\))?)*\\)])");
        Matcher matcher = pattern.matcher(name);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    /**
     * Gets a copy of the m/z value.
     * @return The m/z value of the adduct.
     */
    @Override
    public double getMz() {
        return mz;
    }

    /**
     * Gets the complete name attribute.
     * @return The adducts name.
     */
    public String getCompleteName(){ return this.name; };
}
