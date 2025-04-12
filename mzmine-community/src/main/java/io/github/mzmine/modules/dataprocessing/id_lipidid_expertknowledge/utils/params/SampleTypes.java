package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params;

import java.util.ArrayList;
import java.util.List;

/**
 * Sample types
 */
public enum SampleTypes {

    PLASMA ("Plasma"),
    BLOOD("Blood"),
    CEREBROSPINAL_FLUID("Cerebrospinal fluid");

    private final String name;

    SampleTypes(String name){
        this.name = name;
    }

    public static List<SampleTypes> getListOfMobilePhases(){
        List<SampleTypes> list = new ArrayList<>();
        list.add(SampleTypes.PLASMA);
        list.add(SampleTypes.BLOOD);
        list.add(SampleTypes.CEREBROSPINAL_FLUID);
        return list;
    }

    public String getName() {
        return name;
    }
}
