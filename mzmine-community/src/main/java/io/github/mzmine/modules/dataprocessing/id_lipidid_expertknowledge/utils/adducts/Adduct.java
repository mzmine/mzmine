package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents the user input adducts in the .txt files.
 */
public class Adduct implements ExpertKnowledge {
    private final String name;
    private final double mz;
    private final int charge;

    public Adduct(String name, double mz, int charge) {
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
        Pattern pattern = Pattern.compile("\\[M([\\+\\-]?[A-Za-z0-9]+(?:\\([A-Za-z0-9]+\\))?(?:[\\+\\-]?[A-Za-z0-9]+(?:\\([A-Za-z0-9]+\\))?)*\\)])");
        Matcher matcher = pattern.matcher(name);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    @Override
    public double getMz() {
        return mz;
    }

    public int getCharge() {
        return charge;
    }

    /**
     * Gets the complete name attribute.
     * @return The adducts name.
     */
    public String getCompleteName(){ return this.name; };

    @Override
    public String toString() {
        return name + " (m/z " + mz + ", charge " + charge + ")";
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
