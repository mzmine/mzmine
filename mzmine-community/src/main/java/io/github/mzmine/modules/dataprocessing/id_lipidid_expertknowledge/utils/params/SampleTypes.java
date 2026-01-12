package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params;

import java.util.ArrayList;
import java.util.List;

/**
 * This enumeration represents three common sample types.
 */
public enum SampleTypes {

    PLASMA ("Plasma"),
    BLOOD("Blood"),
    CEREBROSPINAL_FLUID("Cerebrospinal fluid"),
    URINE("Urine"),
    SERUM("Serum"),
    Saliva("Saliva"),
    FECES("Feces/Stool"),
    BREATH("Breath/Exhaled air"),
    AMNIOTIC_FLUID("Amniotic fluid");

    /**
     * Name of the sample type.
     */
    private final String name;

    /**
     * Creates a new SampleType object with the specified info.
     * @param name The name of the new SampleType.
     */
    SampleTypes(String name){
        this.name = name;
    }

    /**
     * Gets a list of all the sample types to put in the setup dialog for the user to choose.
     * @return The complete list of all the sample types.
     */
    public static List<SampleTypes> getListOfSampleTypes(){
        List<SampleTypes> list = new ArrayList<>();
        list.add(SampleTypes.PLASMA);
        list.add(SampleTypes.BLOOD);
        list.add(SampleTypes.CEREBROSPINAL_FLUID);
        return list;
    }

    /**
     * Gtes the name of the SampleType.
     * @return The name of the sample type.
     */
    public String getName() {
        return name;
    }
}
