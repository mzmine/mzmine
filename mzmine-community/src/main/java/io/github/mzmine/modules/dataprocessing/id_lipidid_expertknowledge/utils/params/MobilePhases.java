package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params;

import java.util.ArrayList;
import java.util.List;

/**
 * Mobile phases
 */
public enum MobilePhases {

    CH3COO ("Acetate"),
    NH4 ("Ammonium");

    //TODO add more


    private final String name;

    MobilePhases(String name){
        this.name = name;
    }

    public static List<MobilePhases> getListOfMobilePhases(){
        List<MobilePhases> list = new ArrayList<>();
        list.add(MobilePhases.CH3COO);
        list.add(MobilePhases.NH4);
        return list;
    }
}
