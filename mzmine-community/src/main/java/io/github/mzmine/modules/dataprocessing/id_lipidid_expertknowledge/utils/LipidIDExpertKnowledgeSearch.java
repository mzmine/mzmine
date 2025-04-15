package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils;


import com.lowagie.text.Row;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.features.FeatureListRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidAnnotationResolver;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.*;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids.FoundLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids.Lipid;
import org.kie.api.KieServices;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieSession;

public class LipidIDExpertKnowledgeSearch {
    public static List<FoundAdduct> findAdducts(List<ExpertKnowledge> adductsISF, RowGroup group, double mzTolerance) {
        List<FoundAdduct> foundAdducts = new ArrayList<>();

        //Find all m/z and intensities for the group (individual rows in feature list)
        List<Double> mzList = new ArrayList<>();
        List<Float> intensityList = new ArrayList<>();
        List<Float> rtList = new ArrayList<>();
        List<FeatureListRow> annotatedFeatures = new ArrayList<>();

        for (FeatureListRow groupRow : group.getRows()) {
            //Store annotated features to use to get neutral mass
            if (!groupRow.getLipidMatches().isEmpty()) {
                annotatedFeatures.add(groupRow);
            }
            mzList.add(groupRow.getAverageMZ());
            intensityList.add(groupRow.getMaxHeight());
            rtList.add(groupRow.getAverageRT());
            System.out.println("------mz: " + groupRow.getAverageMZ() + " Intensity: " + groupRow.getMaxHeight() + " group: " + groupRow.getGroupID());
        }
        //Get the last annotation (most recent one) from the annotated features
        //Get the 1st annotated feature
        //TODO change this to see which one I get, in case there are 2+

        //This is case 1: there are annotated lipids
        if (!annotatedFeatures.isEmpty()) {
            System.out.println("-----G: " + group.getGroupID() + " has lipid matches: " + annotatedFeatures.size());
            IonizationType annotatedAdduct = annotatedFeatures.getFirst().getLipidMatches().getLast().getIonizationType();
            //TODO preguntar esto
            /*List<IonizationType> allAdductsAnnotated = new ArrayList<>();
            for (FeatureListRow row : annotatedFeatures){
                if (!row.getLipidMatches().isEmpty()) {

                }
            }*/

            String adductName = annotatedAdduct.getAdductName(); //Returns name. eg: [M+H]+
            Double adductMZ = 0.00;
            Double neutralMass = 0.00;
            for (ExpertKnowledge myAdduct : adductsISF) {
                if (myAdduct.getCompleteName().equals(adductName)) {
                    adductMZ = myAdduct.getMz();
                    // Add the found adduct to the list based on the class type for the charge
                    if (myAdduct.getClass().equals(CommonAdductNegative.class) || myAdduct.getClass().equals(CommonISFNegative.class)) {
                        foundAdducts.add(new FoundAdduct(myAdduct.getCompleteName(), annotatedFeatures.getFirst().getAverageMZ(), annotatedFeatures.getFirst().getMaxHeight(), annotatedFeatures.getFirst().getAverageRT(), -1));
                    } else if (myAdduct.getClass().equals(CommonAdductPositive.class) || myAdduct.getClass().equals(CommonISFPositive.class)) {
                        foundAdducts.add(new FoundAdduct(myAdduct.getCompleteName(), annotatedFeatures.getFirst().getAverageMZ(), annotatedFeatures.getFirst().getMaxHeight(), annotatedFeatures.getFirst().getAverageRT(), 1));
                    }
                    neutralMass = annotatedFeatures.getFirst().getAverageMZ() - adductMZ;
                    System.out.println("-------M: " + neutralMass + " for mz: " + annotatedFeatures.getFirst().getAverageMZ());
                    break;
                }
            }
            //Search the adducts in my list of m/z
            for (ExpertKnowledge myAdduct : adductsISF) {
                if (myAdduct.getCompleteName() == adductName) continue;

                double minRange = (neutralMass + myAdduct.getMz()) - mzTolerance;
                double maxRange = (neutralMass + myAdduct.getMz()) + mzTolerance;

                for (int i = 0; i < mzList.size(); i++) {
                    double mz = mzList.get(i); //Get mz value from the list
                    float intensity = intensityList.get(i); // Get the intensity from the list
                    float rt = rtList.get(i); //Get the RT from the list

                    if (mz > minRange && mz < maxRange) { //Found it in the list
                        System.out.println("---Found: " + myAdduct.getCompleteName());
                        // Add the found adduct to the list based on the class type for the charge
                        if (myAdduct.getClass().equals(CommonAdductNegative.class) || myAdduct.getClass().equals(CommonISFNegative.class)) {
                            foundAdducts.add(new FoundAdduct(myAdduct.getCompleteName(), mz, intensity, rt, -1));
                        } else if (myAdduct.getClass().equals(CommonAdductPositive.class) || myAdduct.getClass().equals(CommonISFPositive.class)) {
                            foundAdducts.add(new FoundAdduct(myAdduct.getCompleteName(), mz, intensity, rt, 1));
                        }
                    }
                }
            }
            //Case 2: There are no annotated lipids
        } else {
            System.out.println("---G: " + group.getGroupID() + " does NOT have lipid matches");

            for (int i = 0; i < mzList.size(); i++) {
                List<FoundAdduct> tempAdducts = new ArrayList<>();
                Double neutralMass = 0.00;

                for (ExpertKnowledge myHypotheticalAdduct : adductsISF) {
                    neutralMass = mzList.get(i) - myHypotheticalAdduct.getMz();
                    // Add the found adduct to the list based on the class type for the charge
                    if (myHypotheticalAdduct.getClass().equals(CommonAdductNegative.class) || myHypotheticalAdduct.getClass().equals(CommonISFNegative.class)) {
                        FoundAdduct candidate = new FoundAdduct(myHypotheticalAdduct.getCompleteName(), mzList.get(i), intensityList.get(i), rtList.get(i), -1);
                        if (!tempAdducts.contains(candidate)) {
                            tempAdducts.add(candidate);
                        }
                    } else if (myHypotheticalAdduct.getClass().equals(CommonAdductPositive.class) || myHypotheticalAdduct.getClass().equals(CommonISFPositive.class)) {
                        FoundAdduct candidate = new FoundAdduct(myHypotheticalAdduct.getCompleteName(), mzList.get(i), intensityList.get(i), rtList.get(i), 1);
                        if (!tempAdducts.contains(candidate)) {
                            tempAdducts.add(candidate);
                        }
                    }
                    for (ExpertKnowledge myAdduct : adductsISF) {
                        if (myAdduct == myHypotheticalAdduct) continue;

                        double minRange = (neutralMass + myAdduct.getMz()) - mzTolerance;
                        double maxRange = (neutralMass + myAdduct.getMz()) + mzTolerance;

                        for (int j = 0; j < mzList.size(); j++) {
                            double mz = mzList.get(j); //Get mz value from the list
                            float intensity = intensityList.get(j); // Get the intensity from the list
                            float rt = rtList.get(j); //Get the RT from the list

                            if (mz > minRange && mz < maxRange) { //Found it in the list
                                // Add the found adduct to the list based on the class type for the charge
                                if (myAdduct.getClass().equals(CommonAdductNegative.class) || myAdduct.getClass().equals(CommonISFNegative.class)) {
                                    FoundAdduct candidate = new FoundAdduct(myAdduct.getCompleteName(), mzList.get(j), intensityList.get(j), rtList.get(j), -1);
                                    if (!tempAdducts.contains(candidate)) {
                                        tempAdducts.add(candidate);
                                    }
                                } else if (myAdduct.getClass().equals(CommonAdductPositive.class) || myAdduct.getClass().equals(CommonISFPositive.class)) {
                                    FoundAdduct candidate = new FoundAdduct(myAdduct.getCompleteName(), mzList.get(j), intensityList.get(j), rtList.get(j), 1);
                                    if (!tempAdducts.contains(candidate)) {
                                        tempAdducts.add(candidate);
                                    }
                                }
                            }
                        }
                    }
                    if (tempAdducts.size() > foundAdducts.size()) {
                        foundAdducts.clear();
                        foundAdducts.addAll(tempAdducts);
                        tempAdducts.clear();
                    }
                    tempAdducts.clear();
                }
            }
        }
        // Comprobar si foundAdducts contiene todos los aductos de ExpertKnowledge
        if (foundAdducts.size() == adductsISF.size()) {
            System.out.println("No adducts were found");
            foundAdducts = null;
        } else {
            System.out.println("Found adducts");
            for (FoundAdduct fa : foundAdducts) System.out.print(fa.getAdductName() + "/" + fa.getMzFeature() + " ; ");
        }
        return foundAdducts;
    }

    public static void findLipidsPositive(RowGroup group, List<FoundAdduct> found) {
        // Find matching lipids based on detected adducts, re-direct to drl file depending on LipidMatched
        List<FoundLipid> detectedLipids = new ArrayList<>();

        for (FeatureListRow row : group.getRows()) {
            List<MatchedLipid> lipidMatched = row.getLipidMatches();
            detectedLipids.clear();

            if (!lipidMatched.isEmpty()) {
                for (MatchedLipid lipid : lipidMatched) {
                    FoundLipid lipid_ExpertKnowledge = new FoundLipid(lipid);
                    String name = lipid.getLipidAnnotation().getLipidClass().getName();
                    String abbr = lipid.getLipidAnnotation().getLipidClass().getAbbr();
                    String path = null;

                    if (abbr.equals("CAR")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.CAR);
                        path = "rules_id_lipid_expert_knowledge/positive/CAR_PositiveCheck.drl";
                    } else if (abbr.equals("CE")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.CE);
                        path = "rules_id_lipid_expert_knowledge/positive/CE_PositiveCheck.drl";
                    } else if (abbr.equals("Cer")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.Cer);
                        path = "rules_id_lipid_expert_knowledge/positive/Cer_PositiveCheck.drl";
                    } else if (abbr.equals("Chol")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.Chol);
                        path = "rules_id_lipid_expert_knowledge/positive/Chol_PositiveCheck.drl";
                    } else if (abbr.equals("DG")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.DG);
                        path = "rules_id_lipid_expert_knowledge/positive/DG_PositiveCheck.drl";
                    } else if (abbr.equals("HexCer")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.HexCer);
                        path = "rules_id_lipid_expert_knowledge/positive/HexCer_PositiveCheck.drl";
                    } else if (abbr.equals("LPC") && name.equals("Monoalkylglycerophosphocholines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.LPC_OP);
                        path = "rules_id_lipid_expert_knowledge/positive/LPC_OP_PositiveCheck.drl";
                    } else if (abbr.equals("LPC") && name.equals("Monoacylglycerophosphocholines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.LPC);
                        path = "rules_id_lipid_expert_knowledge/positive/LPC_PositiveCheck.drl";
                    } else if (abbr.equals("LPE") && name.equals("Monoalkylglycerophosphoethanolamines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.LPE_OP);
                        path = "rules_id_lipid_expert_knowledge/positive/LPE_OP_PositiveCheck.drl";
                    } else if (abbr.equals("LPE") && name.equals("Monoacylglycerophosphoethanolamines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.LPE);
                        path = "rules_id_lipid_expert_knowledge/positive/LPE_PositiveCheck.drl";
                    } else if (abbr.equals("LPI")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.LPI);
                        path = "rules_id_lipid_expert_knowledge/positive/LPI_PositiveCheck.drl";
                    } else if (abbr.equals("PE") && !name.equals("Diacylglycerophosphoethanolamines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.PE_OP);
                        path = "rules_id_lipid_expert_knowledge/positive/PE_OP_PositiveCheck.drl";
                    } else if (abbr.equals("PE") && name.equals("Diacylglycerophosphoethanolamines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.PE);
                        path = "rules_id_lipid_expert_knowledge/positive/PE_PositiveCheck.drl";
                    } else if (abbr.equals("PI")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.PI);
                        path = "rules_id_lipid_expert_knowledge/positive/PI_PositiveCheck.drl";
                    } else if (abbr.equals("SM")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.SM);
                        path = "rules_id_lipid_expert_knowledge/positive/SM_PositiveCheck.drl";
                    } else if (abbr.equals("TG")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.TG);
                        path = "rules_id_lipid_expert_knowledge/positive/TG_PositiveCheck.drl";
                    } else if (abbr.equals("CAR")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.CAR);
                        path = "rules_id_lipid_expert_knowledge/positive/CAR_PositiveCheck.drl";
                    } else if (abbr.equals("PC") && name.equals("Diacylglycerophosphocholines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.PC);
                        path = "rules_id_lipid_expert_knowledge/positive/PC_PositiveCheck.drl";
                    } else if (abbr.equals("PC") && (name.equals("Alkylacylglycerophosphocholines") || name.equals("Dialkylglycerophosphocholines"))) {
                        lipid_ExpertKnowledge.setLipid(Lipid.PC_OP);
                        path = "rules_id_lipid_expert_knowledge/positive/PC_OP_PositiveCheck.drl";
                    }

                    detectedLipids.add(lipid_ExpertKnowledge);

                    if (path != null) {
                        // Initialize KieServices
                        KieServices ks = KieServices.Factory.get();
                        // Create the KieFileSystem
                        var kfs = ks.newKieFileSystem();
                        Resource resource = ks.getResources().newClassPathResource(path);
                        kfs.write("src/main/resources/" + path, resource);

                        var kBuilder = ks.newKieBuilder(kfs);
                        kBuilder.buildAll();

                        var kContainer = ks.newKieContainer(kBuilder.getKieModule().getReleaseId());
                        KieSession kSession = kContainer.newKieSession();

                        kSession.setGlobal("detectedLipids", detectedLipids);
                        kSession.setGlobal("lipid", lipid_ExpertKnowledge);

                        for (FoundAdduct adduct : found) {
                            //TODO quitar
                            System.out.println("--------------ADDUCTS: " + adduct.getAdductName() + " iIntensity: " + adduct.getIntensity());
                            kSession.insert(adduct);
                        }

                        kSession.fireAllRules();
                        kSession.dispose();
                        //Only lipids that have been matched to a rule are added to detectedLipids
                        System.out.println("------------LIPID: " + detectedLipids.getLast().getLipid().getName() + " row: " + row.getAverageMZ() + " group: " + group.getGroupID());
                    }

                }
                if (!detectedLipids.isEmpty()){
                    for (FoundLipid foundLipid : detectedLipids) {
                        if (foundLipid != null) {
                            row.addLipidValidation(foundLipid);
                        }
                    }
                }
            }
        }
    }

    public static void findLipidsNegative(RowGroup group, List<FoundAdduct> found) {
        // Find matching lipids based on detected adducts, re-direct to drl file depending on LipidMatched
        List<FoundLipid> detectedLipids = new ArrayList<>();
        for (FeatureListRow row : group.getRows()) {
            List<MatchedLipid> lipidMatched = row.getLipidMatches();
            detectedLipids.clear();

            if (!lipidMatched.isEmpty()) {
                for (MatchedLipid lipid : lipidMatched) {
                    FoundLipid lipid_ExpertKnowledge = new FoundLipid(lipid);
                    String abbr = lipid.getLipidAnnotation().getLipidClass().getAbbr();
                    String name = lipid.getLipidAnnotation().getLipidClass().getName();
                    String path = null;

                    if (abbr.equals("Cer")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.Cer);
                        path = "rules_id_lipid_expert_knowledge/negative/Cer_NegativeCheck.drl";
                    } else if (abbr.equals("Chol")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.Chol);
                        path = "rules_id_lipid_expert_knowledge/negative/Chol_NegativeCheck.drl";
                    } else if (abbr.equals("DG")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.DG);
                        path = "rules_id_lipid_expert_knowledge/negative/DG_NegativeCheck.drl";
                    } else if (abbr.equals("HexCer")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.HexCer);
                        path = "rules_id_lipid_expert_knowledge/negative/HexCer_NegativeCheck.drl";
                    } else if (abbr.equals("LPC") && name.equals("Monoalkylglycerophosphocholines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.LPC_OP);
                        path = "rules_id_lipid_expert_knowledge/negative/LPC_OP_NegativeCheck.drl";
                    } else if (abbr.equals("LPC") && name.equals("Monoacylglycerophosphocholines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.LPC);
                        path = "rules_id_lipid_expert_knowledge/negative/LPC_NegativeCheck.drl";
                    } else if (abbr.equals("LPE") && name.equals("Monoalkylglycerophosphoethanolamines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.LPE_OP);
                        path = "rules_id_lipid_expert_knowledge/negative/LPE_OP_NegativeCheck.drl";
                    } else if (abbr.equals("LPE") && name.equals("Monoacylglycerophosphoethanolamines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.LPE);
                        path = "rules_id_lipid_expert_knowledge/negative/LPE_NegativeCheck.drl";
                    } else if (abbr.equals("LPI")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.LPI);
                        path = "rules_id_lipid_expert_knowledge/negative/LPI_NegativeCheck.drl";
                    } else if (abbr.equals("PE") && !name.equals("Diacylglycerophosphoethanolamines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.PE_OP);
                        path = "rules_id_lipid_expert_knowledge/negative/PE_OP_NegativeCheck.drl";
                    } else if (abbr.equals("PE") && name.equals("Diacylglycerophosphoethanolamines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.PE);
                        path = "rules_id_lipid_expert_knowledge/negative/PE_NegativeCheck.drl";
                    } else if (abbr.equals("PI")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.PI);
                        path = "rules_id_lipid_expert_knowledge/negative/PI_NegativeCheck.drl";
                    } else if (abbr.equals("SM")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.SM);
                        path = "rules_id_lipid_expert_knowledge/negative/SM_NegativeCheck.drl";
                    } else if (abbr.equals("PC") && name.equals("Diacylglycerophosphocholines")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.PC);
                        path = "rules_id_lipid_expert_knowledge/negative/PC_NegativeCheck.drl";
                    } else if (abbr.equals("PC") && (name.equals("Alkylacylglycerophosphocholines") || name.equals("Dialkylglycerophosphocholines"))) {
                        lipid_ExpertKnowledge.setLipid(Lipid.PC_OP);
                        path = "rules_id_lipid_expert_knowledge/negative/PC_OP_NegativeCheck.drl";
                    } else if (abbr.equals("FA")) {
                        lipid_ExpertKnowledge.setLipid(Lipid.FA);
                        path = "rules_id_lipid_expert_knowledge/negative/FA_NegativeCheck.drl";
                    }

                    detectedLipids.add(lipid_ExpertKnowledge);

                    if (path != null) {
                        // Initialize KieServices
                        KieServices ks = KieServices.Factory.get();
                        // Create the KieFileSystem
                        var kfs = ks.newKieFileSystem();
                        Resource resource = ks.getResources().newClassPathResource(path);
                        kfs.write("src/main/resources/" + path, resource);

                        var kBuilder = ks.newKieBuilder(kfs);
                        kBuilder.buildAll();

                        var kContainer = ks.newKieContainer(kBuilder.getKieModule().getReleaseId());
                        KieSession kSession = kContainer.newKieSession();

                        kSession.setGlobal("detectedLipids", detectedLipids);
                        kSession.setGlobal("lipid", lipid_ExpertKnowledge);

                        for (FoundAdduct adduct : found) {
                            kSession.insert(adduct);
                        }

                        kSession.fireAllRules();
                        kSession.dispose();
                    }

                }
                if (!detectedLipids.isEmpty()){
                    for (FoundLipid foundLipid : detectedLipids) {
                        if (foundLipid != null) {
                            row.addLipidValidation(foundLipid);
                        }
                    }
                }
            }
        }
    }
}

