package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params;

import java.util.ArrayList;
import java.util.List;

/**
 * This enumeration represents two of the most common mobile phases used in mass spectrometry.
 */
public enum MobilePhases {

    CH3COO ("Acetate"),
    NH4 ("Ammonium"),
    CH3OH("Metanol"),
    CH3CN("Acetonitrile"),
    HCOO("Formate");


    /**
     * Name of the mobile phase.
     */
    private final String name;

    /**
     * Creates a new MobilePhase object with the specified info.
     * @param name The name of the new MobilePhase.
     */
    MobilePhases(String name){
        this.name = name;
    }

    /**
     * Gets a list of all the mobile phases to put in the setup dialog for the user to choose.
     * @return The complete list of all the mobile phases.
     */
    public static List<MobilePhases> getListOfMobilePhases(){
        List<MobilePhases> list = new ArrayList<>();
        list.add(MobilePhases.CH3COO);
        list.add(MobilePhases.NH4);
        list.add(MobilePhases.CH3CN);
        list.add(MobilePhases.CH3OH);
        return list;
    }

}
