package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params;

import java.util.ArrayList;
import java.util.List;

public enum SampleTypes {

    PLASMA ("Plasma"),
    BRAIN_TISSUE("Brain tissue"),
    BLOOD("Blood"),
    URINE("Urine"),
    LIVER_TISSUE("Liver tissue"),
    MUSCLE_TISSUE("Muscle tissue"),
    CEREBROSPINAL_FLUID("Cerebrospinal fluid"),
    SALIVA("Saliva"),
    KIDNEY_TISSUE("Kidney tissue"),
    LUNG_TISSUE("Lung tissue");

    //TODO add more

    private final String name;

    SampleTypes(String name){
        this.name = name;
    }

    public static List<SampleTypes> getListOfMobilePhases(){
        List<SampleTypes> list = new ArrayList<>();
        list.add(SampleTypes.PLASMA);
        list.add(SampleTypes.BRAIN_TISSUE);
        return list;
    }

    public String getName() {
        return name;
    }
}
