package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts;

/**
 * Interface used by the different adducts and ISF enums
 */
public interface ExpertKnowledge {
    double getMz();
    String getName();
    double calculateNeutralMass(double observedMz);
    double expectedMz(double neutralMass);
    String getCompleteName();
}
