package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts;

/**
 * Interface used by the different adducts and ISF enums containing common methods.
 */
public interface ExpertKnowledge {
    /**
     * This method gets the m/z values.
     * @return m/z double value.
     */
    double getMz();

    /**
     * This method gets the adduct ion (eg: "+Cl").
     * @return ion in String format.
     */
    String getName();

    /**
     * This method gets the comple name attribute (eg: "[M+Cl]-").
     * @return complete name in String format.
     */
    String getCompleteName();

    /**
     * This method gets the charge value.
     * @return charge int value.
     */
    int getCharge();

    /**
     * This method gets the molecule multiplier (eg: [2M+Na]+ -- 2)
     * @return
     */
    int getMoleculeMultiplier();
}
