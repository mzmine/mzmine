package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This enum contains the most common adducts found in lipids according to a CEMBIO paper:
 * Martínez, S., Fernández-García, M., Londoño-Osorio, S., Barbas, C., & Gradillas, A. (2024). Highly reliable LC-MS lipidomics database for efficient human plasma profiling based on NIST SRM 19501. Journal of Lipid Research, 65(11), 100671. https://doi.org/10.1016/j.jlr.2024.100671
 * It has two attributes, the name (formula) and the m/z difference.
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
     * Specifies the name
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
