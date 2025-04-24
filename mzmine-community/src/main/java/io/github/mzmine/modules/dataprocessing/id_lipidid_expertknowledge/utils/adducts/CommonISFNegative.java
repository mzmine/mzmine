package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This enum contains the most common in-source fragments found in lipids according to a CEMBIO paper:
 * Martínez, S., Fernández-García, M., Londoño-Osorio, S., Barbas, C., & Gradillas, A. (2024). Highly reliable LC-MS lipidomics database for efficient human plasma profiling based on NIST SRM 19501. Journal of Lipid Research, 65(11), 100671. https://doi.org/10.1016/j.jlr.2024.100671
 * It has two attributes, the name (formula) and the m/z difference.
 */
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
     * Gets the ion. Eg: [M+H]+ --> +H
     * @return adduct name
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
     * Gets a copy of the m/z
     * @return adduct m/z
     */
    @Override
    public double getMz() {
        return mz;
    }

    /**
     * Returns a copy of the name. Eg: [M+H]+
     * @return
     */
    public String getCompleteName(){ return this.name; };
}
