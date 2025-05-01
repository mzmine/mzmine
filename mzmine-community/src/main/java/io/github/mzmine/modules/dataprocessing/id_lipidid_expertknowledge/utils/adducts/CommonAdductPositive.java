package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This enum contains the most common adducts found in lipids according to a CEMBIO paper:
 * Martínez, S., Fernández-García, M., Londoño-Osorio, S., Barbas, C., & Gradillas, A. (2024). Highly reliable LC-MS lipidomics database for efficient human plasma profiling based on NIST SRM 19501. Journal of Lipid Research, 65(11), 100671. https://doi.org/10.1016/j.jlr.2024.100671
 * It represents five common adducts for ESI+.
 */
public enum CommonAdductPositive implements ExpertKnowledge {
    // ESI(+)
    M_PLUS_H("[M+H]+", 1.007276),
    M_PLUS_NA("[M+Na]+", 22.989218),
    M_PLUS_K("[M+K]+", 38.963158),
    M_PLUS_C2H7N2("[M+C2H7N2]+", 59.04914),
    M_PLUS_NH4("[M+NH4]+", 18.033823);

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
     * Creates a new CommonAdductPositive with the specified info.
     * @param name The name of the adduct.
     * @param mz The m/z of the adduct.
     */
    CommonAdductPositive(String name, double mz) {
        this.name = name;
        this.mz = mz;
    }

    /**
     * Gets the ion form the name attribute. It returns whatever is between "[M" and "]".
     * Eg: "[M+H]+" --> "+H"
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
