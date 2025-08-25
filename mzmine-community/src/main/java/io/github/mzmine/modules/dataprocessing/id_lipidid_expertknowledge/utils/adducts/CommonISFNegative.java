package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This enum contains the most common in-source fragments found in lipids according to a CEMBIO paper:
 * Martínez, S., Fernández-García, M., Londoño-Osorio, S., Barbas, C., & Gradillas, A. (2024). Highly reliable LC-MS lipidomics database for efficient human plasma profiling based on NIST SRM 19501. Journal of Lipid Research, 65(11), 100671. https://doi.org/10.1016/j.jlr.2024.100671
 * It represents one common in-source fragment for ESI-.
 */
public enum CommonISFNegative implements ExpertKnowledge {
    // ESI(-) In-source fragmentation
    M_PLUS_CH3COO_MINUS_CH3COOCH3("[M+CH3COO-CH3COOCH3]-", -15.01090, -1);

    /**
     * String that represents the complete formula (eg: "[M+CH3COO-CH3COOCH3]-").
     */
    private final String name;

    /**
     * Double representing the mass difference between a molecule (M) and the adduct.
     * It accounts for the ions mass and charge.
     */
    private final double mz;

    /**
     * Int representing the charge of the adduct.
     */
    private final int charge;

    /**
     * Creates a new CommonISFNegative with the specified info.
     * @param name The name of the adduct.
     * @param mz The m/z of the adduct.
     */
    CommonISFNegative(String name, double mz, int charge) {
        this.name = name;
        this.mz = mz;
        this.charge = charge;
    }

    /**
     * Gets the ion form the name attribute. It returns whatever is between "[M" and "]".
     * Eg: "[M+H]+" --> "+H"
     * @return The ion from the adduct name.
     */
    @Override
    public String getName() {
        // Regex to capture patterns like: [M-H], [M+H], [M+CH3COO], [M+CH3COO+(CH3COONa)]
        Pattern pattern = Pattern.compile("\\[M([\\+\\-]?[A-Za-z0-9]+(?:\\([A-Za-z0-9]+\\))?(?:[\\+\\-]?[A-Za-z0-9]+(?:\\([A-Za-z0-9]+\\))?)*\\)])");
        Matcher matcher = pattern.matcher(name);

        if (matcher.find()) {
            return matcher.group(1);  // Extract the part after [M and before ]
        }

        return "";  // Return this if pattern is not found
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

    /**
     * Gets the charge value.
     * @return The charge of the adduct.
     */
    public int getCharge() {
        return charge;
    }

    /**
     * Extracts the molecule multiplier from an adduct string.
     * Examples:
     *   [M+H]+     -> 1
     *   [2M+Na]+   -> 2
     *   [3M+CH3COO]- -> 3
     *
     * @return multiplier as integer (default 1 if not specified)
     */
    public int getMoleculeMultiplier() {
        if (this.name == null || this.name.isEmpty()) return 1;

        // Regex: optional digits before 'M' inside brackets
        Pattern pattern = Pattern.compile("\\[(\\d*)M");
        Matcher matcher = pattern.matcher(this.name);

        if (matcher.find()) {
            String digits = matcher.group(1);
            if (digits.isEmpty()) return 1; // default 1 if no number
            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException e) {
                return 1; // fallback
            }
        }
        return 1; // default if pattern not found
    }
}
