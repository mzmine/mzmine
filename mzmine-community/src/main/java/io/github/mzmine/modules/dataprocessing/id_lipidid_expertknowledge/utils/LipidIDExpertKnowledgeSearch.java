package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils;


import io.github.mzmine.datamodel.features.FeatureListRow;

import java.util.ArrayList;
import java.util.List;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import org.kie.api.KieServices;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieSession;

public class LipidIDExpertKnowledgeSearch {
    public static List<FoundAdduct> findAdducts(List<ExpertKnowledge> adductsISF, FeatureListRow row, double mzTolerance) {
        List<FoundAdduct> foundAdducts = new ArrayList<>();

        for (ExpertKnowledge a : adductsISF) {
            if (Math.abs(a.getMz() - row.getAverageMZ()) <= mzTolerance) {
                foundAdducts.add(new FoundAdduct(a.getName(), row.getAverageMZ(), row.getMaxHeight(), row.getAverageRT()));
                System.out.println("---Found: "+a.getName()+" on row: "+row.getAverageMZ());
            }
        }
        return foundAdducts;
    }

    public static List<FoundLipid> findLipidsPositive(FeatureListRow row, List<FoundAdduct> found){
        // Find matching lipids based on detected adducts, re-direct to drl file depending on LipidMatched
        List<MatchedLipid> lipidMatched = row.getLipidMatches();
        FoundLipid lipid_ExpertKnowledge = new FoundLipid();
        List<FoundLipid> detectedLipids = new ArrayList<>();

        for (MatchedLipid lipid : lipidMatched){
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

                //Only lipids that have been matched to a rule are added to detectedLipids
                return detectedLipids;
            }
        }
        return detectedLipids;
    }

    public static List<FoundLipid> findLipidsNegative(FeatureListRow row, List<FoundAdduct> found) {
        // Find matching lipids based on detected adducts, re-direct to drl file depending on LipidMatched
        List<MatchedLipid> lipidMatched = row.getLipidMatches();
        FoundLipid lipid_ExpertKnowledge = new FoundLipid();
        List<FoundLipid> detectedLipids = new ArrayList<>();

        for (MatchedLipid lipid : lipidMatched) {
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
            } else if (abbr.equals("TG")) {
                lipid_ExpertKnowledge.setLipid(Lipid.TG);
                path = "rules_id_lipid_expert_knowledge/negative/TG_NegativeCheck.drl";
            }

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

                //Only lipids that have been matched to a rule are added to detectedLipids
                return detectedLipids;
            }
        }
        return detectedLipids;
    }
}

