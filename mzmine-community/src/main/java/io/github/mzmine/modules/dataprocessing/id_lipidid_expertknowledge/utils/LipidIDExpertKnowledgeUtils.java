package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils;


import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
/*
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;*/

public class LipidIDExpertKnowledgeUtils {
    public static List<DetectedAdduct> findAdductsPos(List<ExpertKnowledge> adductsISF, FeatureListRow row, double mzTolerance) {
        List<DetectedAdduct> foundAdducts = new ArrayList<>();

        for (ExpertKnowledge a : adductsISF) {
            if (Math.abs(a.getMz() - row.getAverageMZ()) <= mzTolerance) {
                foundAdducts.add(new DetectedAdduct(a.getName(), row.getAverageMZ(), row.getMaxHeight(), row.getAverageRT()));
                System.out.println("---Found: "+a.getName()+" on row: "+row.getAverageMZ());
            }
        }
        return foundAdducts;
    }

    //TODO reglas
    /*7public static List<Lipid> findLipids(List<DetectedAdduct> found){
        KieServices ks = KieServices.Factory.get();
        KieContainer kContainer = ks.getKieClasspathContainer();
        KieSession kSession = kContainer.newKieSession();

        List<Lipid> detectedLipids = new ArrayList<>();
        kSession.setGlobal("detectedLipids", detectedLipids);

        // Insert each adduct as a fact in Drools
        for (CommonAdductPositive adduct : found) {
            kSession.insert(adduct);
        }

        kSession.fireAllRules();
        kSession.dispose();

        return detectedLipids;
    }*/
}
