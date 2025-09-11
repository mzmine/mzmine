package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils;


import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.features.FeatureListRow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.*;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids.FoundLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids.Lipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.MobilePhases;
import org.kie.api.KieServices;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieSession;

/**
 * Class that contains all the methods that search and find information for the module.
 */
public class LipidIDExpertKnowledgeSearch {

    /**
     * Finds the rows that have Lipid Annotations by is the lipid matches are empty.
     * If they are empty, they are added to a list.
     * @param group RowGroup that has all the rows with the same groupID (assigned by metaCorrelate).
     * @return List of rows with Lipid Annotations.
     */
    public static List<FeatureListRow> findAnnotatedRows(RowGroup group) {
        List<FeatureListRow> annotatedRows = new ArrayList<>();
        for (FeatureListRow row : group.getRows()) {
            if (!row.getLipidMatches().isEmpty()) {
                annotatedRows.add(row);
            }
        }
        return annotatedRows;
    }

    /**
     * Finds and stores important information of each row in a group (mz, intensity and rt).
     * @param group RowGroup that has all the rows with the same groupID.
     * @return RowInfo objects with the m/z, intensity and RT values for a group.
     */
    public static RowInfo findRowInfo(RowGroup group) {
        List<Double> mzList = new ArrayList<>();
        List<Float> intensityList = new ArrayList<>();
        List<Float> rtList = new ArrayList<>();
        for (FeatureListRow groupRow : group.getRows()) {
            mzList.add(groupRow.getAverageMZ());
            intensityList.add(groupRow.getMaxHeight());
            rtList.add(groupRow.getAverageRT());
        }
        RowInfo rowInfo = new RowInfo(mzList, intensityList, rtList);
        return rowInfo;
    }

    /**
     * Finds adducts taking as input rows with Lipid Annotations.
     * It is run for individual rows(for simplicity), as well as individual MatchedLipids. If one row has many MatchedLipids, this method will be called once for every MatchedLipid in that row.
     * It checks to see if the row has MatchedLipids, if it does, it uses it as a reference to get the neutral mass of the molecule. After we have the neutral mass it loops through the List<ExpertKnowledge> and calculates a range of m/z with the neutral mass, the ExpertKnowledge m/z and the mzTolerance, and then if any of the values in RowInfo have an m/z within the range, we can say that that adduct is found.
     * In case the row’s MatchedLipid is empty, it does the same process but taking every adduct from List<ExpertKowledge> as reference.
     * @param adductsISF List of possible adducts and ISF, obtained from the enumerations.
     * @param rowInfo RowInfo for the group the row is part of.
     * @param mzTolerance mz tolerance, set by the user.
     * @param row FeatureListRow with Lipid Annotation.
     * @param match MatchedLipid present in the row.
     * @return List of adducts found for the group the row is part of.
     */
    public static List<FoundAdduct> findAdducts(List<ExpertKnowledge> adductsISF, RowInfo rowInfo, double mzTolerance,
                                                FeatureListRow row, MatchedLipid match, List<Adduct> userAdducts) {
        List<FoundAdduct> foundAdducts = new ArrayList<>();
        List<Double> mzList = rowInfo.getMzList();
        List<Float> intensityList = rowInfo.getIntensityList();
        List<Float> rtList = rowInfo.getRtList();

        // Combine system adducts + user adducts into one pool
        List<ExpertKnowledge> allAdducts = new ArrayList<>(adductsISF);
        if (userAdducts != null) {
            allAdducts.addAll(userAdducts);
        }

        //This is case 1: there are annotated lipids
        if (!row.getLipidMatches().isEmpty()) {
            IonizationType annotatedAdduct = match.getIonizationType();

            String adductName = annotatedAdduct.getAdductName(); //Returns name. eg: [M+H]+
            Double adductMZ = 0.00;
            Double neutralMass = 0.00;
            for (ExpertKnowledge myAdduct : allAdducts) {
                if (myAdduct.getCompleteName().equals(adductName)) {
                    adductMZ = myAdduct.getMz();
                    // Add the found adduct to the list based on the class type for the charge
                    /*if (myAdduct.getClass().equals(CommonAdductNegative.class) || myAdduct.getClass().equals(CommonISFNegative.class)) {
                        foundAdducts.add(new FoundAdduct(myAdduct.getCompleteName(), row.getAverageMZ(), row.getMaxHeight(), row.getAverageRT(), -1));
                    } else if (myAdduct.getClass().equals(CommonAdductPositive.class) || myAdduct.getClass().equals(CommonISFPositive.class)) {
                        foundAdducts.add(new FoundAdduct(myAdduct.getCompleteName(), row.getAverageMZ(), row.getMaxHeight(), row.getAverageRT(), 1));
                    }
                    neutralMass = row.getAverageMZ() - adductMZ;
                    break;*/
                    neutralMass = (row.getAverageMZ() - adductMZ) / myAdduct.getMoleculeMultiplier(); // support 2M, 3M etc.
                    foundAdducts.add(new FoundAdduct(
                            myAdduct.getCompleteName(),
                            row.getAverageMZ(),
                            row.getMaxHeight(),
                            row.getAverageRT(),
                            myAdduct.getCharge()   // use dynamic charge
                    ));
                    break;
                }
            }

            //Search the adducts in my list of m/z
            for (ExpertKnowledge myAdduct : allAdducts) {
                if (myAdduct.getCompleteName() == adductName) continue;

                //double minRange = (neutralMass + myAdduct.getMz()) - mzTolerance;
                //double maxRange = (neutralMass + myAdduct.getMz()) + mzTolerance;

                double expectedMz = neutralMass * myAdduct.getMoleculeMultiplier() + myAdduct.getMz();
                double minRange = expectedMz - mzTolerance;
                double maxRange = expectedMz + mzTolerance;

                for (int i = 0; i < mzList.size(); i++) {
                    double mz = mzList.get(i); //Get mz value from the list
                    double intensity = intensityList.get(i); // Get the intensity from the list
                    float rt = rtList.get(i); //Get the RT from the list

                    if (mz > minRange && mz < maxRange) { //Found it in the list
                        // Add the found adduct to the list based on the class type for the charge
                        /*if (myAdduct.getClass().equals(CommonAdductNegative.class) || myAdduct.getClass().equals(CommonISFNegative.class)) {
                            foundAdducts.add(new FoundAdduct(myAdduct.getCompleteName(), mz, intensity, rt, -1));
                        } else if (myAdduct.getClass().equals(CommonAdductPositive.class) || myAdduct.getClass().equals(CommonISFPositive.class)) {
                            foundAdducts.add(new FoundAdduct(myAdduct.getCompleteName(), mz, intensity, rt, 1));
                        }*/
                        foundAdducts.add(new FoundAdduct(
                                myAdduct.getCompleteName(),
                                mz,
                                intensity,
                                rt,
                                myAdduct.getCharge()
                        ));
                    }
                }
            }
            //Case 2: There are no annotated lipids
        } else {
            for (int i = 0; i < mzList.size(); i++) {
                List<FoundAdduct> tempAdducts = new ArrayList<>();
                //Double neutralMass = 0.00;
                double mzObserved = mzList.get(i);

                for (ExpertKnowledge myHypotheticalAdduct : adductsISF) {
                    //neutralMass = mzList.get(i) - myHypotheticalAdduct.getMz();
                    double neutralMass = (mzObserved - myHypotheticalAdduct.getMz()) / myHypotheticalAdduct.getMoleculeMultiplier();
                    // Add the found adduct to the list based on the class type for the charge
                    /*if (myHypotheticalAdduct.getClass().equals(CommonAdductNegative.class) || myHypotheticalAdduct.getClass().equals(CommonISFNegative.class)) {
                        FoundAdduct candidate = new FoundAdduct(myHypotheticalAdduct.getCompleteName(), mzList.get(i), intensityList.get(i), rtList.get(i), -1);
                        if (!tempAdducts.contains(candidate)) {
                            tempAdducts.add(candidate);
                        }
                    } else if (myHypotheticalAdduct.getClass().equals(CommonAdductPositive.class) || myHypotheticalAdduct.getClass().equals(CommonISFPositive.class)) {
                        FoundAdduct candidate = new FoundAdduct(myHypotheticalAdduct.getCompleteName(), mzList.get(i), intensityList.get(i), rtList.get(i), 1);
                        if (!tempAdducts.contains(candidate)) {
                            tempAdducts.add(candidate);
                        }
                    }*/

                    FoundAdduct candidate = new FoundAdduct(
                            myHypotheticalAdduct.getCompleteName(),
                            mzObserved,
                            intensityList.get(i),
                            rtList.get(i),
                            myHypotheticalAdduct.getCharge()
                    );
                    if (!tempAdducts.contains(candidate)) {
                        tempAdducts.add(candidate);
                    }

                    for (ExpertKnowledge myAdduct : allAdducts) {
                        if (myAdduct == myHypotheticalAdduct) continue;

                        //double minRange = (neutralMass + myAdduct.getMz()) - mzTolerance;
                        //double maxRange = (neutralMass + myAdduct.getMz()) + mzTolerance;

                        double expectedMz = neutralMass * myAdduct.getMoleculeMultiplier() + myAdduct.getMz();
                        double minRange = expectedMz - mzTolerance;
                        double maxRange = expectedMz + mzTolerance;

                        for (int j = 0; j < mzList.size(); j++) {
                            double mz = mzList.get(j); //Get mz value from the list
                            double intensity = intensityList.get(j); // Get the intensity from the list
                            float rt = rtList.get(j); //Get the RT from the list

                            if (mz > minRange && mz < maxRange) { //Found it in the list
                                // Add the found adduct to the list based on the class type for the charge
                                /*if (myAdduct.getClass().equals(CommonAdductNegative.class) || myAdduct.getClass().equals(CommonISFNegative.class)) {
                                    FoundAdduct candidate = new FoundAdduct(myAdduct.getCompleteName(), mzList.get(j), intensityList.get(j), rtList.get(j), -1);
                                    if (!tempAdducts.contains(candidate)) {
                                        tempAdducts.add(candidate);
                                    }
                                } else if (myAdduct.getClass().equals(CommonAdductPositive.class) || myAdduct.getClass().equals(CommonISFPositive.class)) {
                                    FoundAdduct candidate = new FoundAdduct(myAdduct.getCompleteName(), mzList.get(j), intensityList.get(j), rtList.get(j), 1);
                                    if (!tempAdducts.contains(candidate)) {
                                        tempAdducts.add(candidate);
                                    }
                                }*/
                                FoundAdduct complement = new FoundAdduct(
                                        myAdduct.getCompleteName(),
                                        mz,
                                        intensity,
                                        rt,
                                        myAdduct.getCharge()
                                );
                                if (!tempAdducts.contains(complement)) {
                                    tempAdducts.add(complement);
                                }
                            }
                        }
                    }
                    if (tempAdducts.size() > foundAdducts.size() && tempAdducts.size() >= 3) {
                        foundAdducts.clear();
                        foundAdducts.addAll(tempAdducts);
                        tempAdducts.clear();
                    }
                    tempAdducts.clear();
                }
            }
        }

        Collections.sort(foundAdducts, Comparator.comparingDouble(FoundAdduct::getIntensity).reversed());
        return foundAdducts;
    }

    /**
     * Finds lipids in ESI+ from adducts found for a group.
     * It obtains all the necessary information from the MatchedLipid, and creates a new FoundLipid with it. Then, depending on the abbreviation of the MatchedLipid, it will set a path, a set of double parameters to get the score at the end, and it sets the Lipid in FoundLipid. It then generates all the necessary parameters to fire the rules in the file specified by the path and it calculates the final score.
     * Finally, it adds the results to the FeatureListRow to be able to see them in the output.
     * @param row FeatureListRow that has Lipid Annotation.
     * @param lipid MatchedLipid for the row.
     * @param found List of FoundAdducts for the group the row is part of.
     */
    public static void findLipidsPositive(FeatureListRow row, MatchedLipid lipid, List<FoundAdduct> found, List<MobilePhases> mobilePhases, VirtualRowGroup virtualGroup) throws IOException {
        // Find matching lipids based on detected adducts, re-direct to drl file depending on LipidMatched
        List<FoundLipid> detectedLipids = new ArrayList<>();

        FoundLipid lipid_ExpertKnowledge = new FoundLipid(lipid);
        lipid_ExpertKnowledge.setSubgroupID(virtualGroup.getSubgroupID());
        lipid_ExpertKnowledge.setAdducts(found);
        String name = lipid.getLipidAnnotation().getLipidClass().getName();
        String abbr = lipid.getLipidAnnotation().getLipidClass().getAbbr();
        String path = null;
        //To normalize final score
        double scorePresence = 1.00;
        double scoreIntensity = 2.00;
        //double totalMaxPresence;
        //double totalMaxIntensity;
        double appliedPresence = 0.00;
        double appliedIntensity = 0.00;

        double maxScore = 0.00;

        String[] positiveNames = {"CAR", "CE", "Cer", "Chol", "DG", "HexCer", "LPC", "LPE", "LPI", "PC", "PE", "PI", "SM", "TG"};
        boolean isUserFile;

        if (Arrays.asList(positiveNames).contains(abbr)) { //matches one of the default drl files
            isUserFile = false;
            if (abbr.equals("CAR")) {
                lipid_ExpertKnowledge.setLipid(Lipid.CAR);
                path = "rules_id_lipid_expert_knowledge/positive/CAR_PositiveCheck.drl";
                //totalMaxPresence = 1;
                //totalMaxIntensity = 0;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("CE")) {
                lipid_ExpertKnowledge.setLipid(Lipid.CE);
                path = "rules_id_lipid_expert_knowledge/positive/CE_PositiveCheck.drl";
                //totalMaxPresence = 6;
                //totalMaxIntensity = 4;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("Cer")) {
                lipid_ExpertKnowledge.setLipid(Lipid.Cer);
                path = "rules_id_lipid_expert_knowledge/positive/Cer_PositiveCheck.drl";
                //totalMaxPresence = 5;
                //totalMaxIntensity = 3;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("Chol")) {
                lipid_ExpertKnowledge.setLipid(Lipid.Chol);
                path = "rules_id_lipid_expert_knowledge/positive/Chol_PositiveCheck.drl";
                //totalMaxPresence = 3;
                //totalMaxIntensity = 1;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("DG")) {
                lipid_ExpertKnowledge.setLipid(Lipid.DG);
                path = "rules_id_lipid_expert_knowledge/positive/DG_PositiveCheck.drl";
                //totalMaxPresence = 6;
                //totalMaxIntensity = 4;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("HexCer")) {
                lipid_ExpertKnowledge.setLipid(Lipid.HexCer);
                path = "rules_id_lipid_expert_knowledge/positive/HexCer_PositiveCheck.drl";
                //totalMaxPresence = 4;
                //totalMaxIntensity = 2;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("LPC") && name.equals("Monoalkylglycerophosphocholines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.LPC_OP);
                path = "rules_id_lipid_expert_knowledge/positive/LPC_OP_PositiveCheck.drl";
                //totalMaxPresence = 5;
                //totalMaxIntensity = 1;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("LPC") && name.equals("Monoacylglycerophosphocholines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.LPC);
                path = "rules_id_lipid_expert_knowledge/positive/LPC_PositiveCheck.drl";
                //totalMaxPresence = 5;
                //totalMaxIntensity = 4;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("LPE") && name.equals("Monoalkylglycerophosphoethanolamines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.LPE_OP);
                path = "rules_id_lipid_expert_knowledge/positive/LPE_OP_PositiveCheck.drl";
                //totalMaxPresence = 5;
                //totalMaxIntensity = 0;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("LPE") && name.equals("Monoacylglycerophosphoethanolamines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.LPE);
                path = "rules_id_lipid_expert_knowledge/positive/LPE_PositiveCheck.drl";
                //totalMaxPresence = 5;
                //totalMaxIntensity = 1;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("LPI")) {
                lipid_ExpertKnowledge.setLipid(Lipid.LPI);
                path = "rules_id_lipid_expert_knowledge/positive/LPI_PositiveCheck.drl";
                //totalMaxPresence = 6;
                //totalMaxIntensity = 0;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("PE") && !name.equals("Diacylglycerophosphoethanolamines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.PE_OP);
                path = "rules_id_lipid_expert_knowledge/positive/PE_OP_PositiveCheck.drl";
                //totalMaxPresence = 4;
                //totalMaxIntensity = 3;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("PE") && name.equals("Diacylglycerophosphoethanolamines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.PE);
                path = "rules_id_lipid_expert_knowledge/positive/PE_PositiveCheck.drl";
                //totalMaxPresence = 4;
                //totalMaxIntensity = 1;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("PI")) {
                lipid_ExpertKnowledge.setLipid(Lipid.PI);
                path = "rules_id_lipid_expert_knowledge/positive/PI_PositiveCheck.drl";
                //totalMaxPresence = 5;
                //totalMaxIntensity = 4;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("SM")) {
                lipid_ExpertKnowledge.setLipid(Lipid.SM);
                path = "rules_id_lipid_expert_knowledge/positive/SM_PositiveCheck.drl";
                //totalMaxPresence = 4;
                //totalMaxIntensity = 3;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("TG")) {
                lipid_ExpertKnowledge.setLipid(Lipid.TG);
                path = "rules_id_lipid_expert_knowledge/positive/TG_PositiveCheck.drl";
                //totalMaxPresence = 5;
                //totalMaxIntensity = 4;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("PC") && name.equals("Diacylglycerophosphocholines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.PC);
                path = "rules_id_lipid_expert_knowledge/positive/PC_PositiveCheck.drl";
                //totalMaxPresence = 4;
                //totalMaxIntensity = 4;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("PC") && (name.equals("Alkylacylglycerophosphocholines") || name.equals("Dialkylglycerophosphocholines"))) {
                lipid_ExpertKnowledge.setLipid(Lipid.PC_OP);
                path = "rules_id_lipid_expert_knowledge/positive/PC_OP_PositiveCheck.drl";
                //totalMaxPresence = 4;
                //totalMaxIntensity = 2;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            }
        } else { //does not match one of the default drl files
            isUserFile = true;
            Path userFolder = Paths.get(System.getProperty("user.dir"),
                    "mzmine-community/src/main/resources/rules_id_lipid_expert_knowledge/positive/userFiles");
            Path path1 = findFirstFileWithAbbreviation(userFolder, abbr);
            if (path1 != null) {
                path = String.valueOf(path1);
                //continue logic
                //totalMaxPresence = countPresenceRules(path1);
                //totalMaxIntensity = countPositiveIntensityRules(path1);
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            }

        }

        detectedLipids.add(lipid_ExpertKnowledge);

        if (path != null) {
            // Initialize KieServices
            KieServices ks = KieServices.Factory.get();
            // Create the KieFileSystem
            var kfs = ks.newKieFileSystem();

            if (!isUserFile) {
                Resource resource = ks.getResources().newClassPathResource(path);
                kfs.write("src/main/resources/" + path, resource);
            } else if (isUserFile) {
                Resource resource = ks.getResources().newFileSystemResource(path);
                kfs.write(resource);
            }

            var kBuilder = ks.newKieBuilder(kfs);
            kBuilder.buildAll();

            var kContainer = ks.newKieContainer(kBuilder.getKieModule().getReleaseId());
            KieSession kSession = kContainer.newKieSession();

            kSession.setGlobal("lipid", lipid_ExpertKnowledge);
            kSession.setGlobal("mobilePhases", mobilePhases);

            for (FoundAdduct adduct : found) {
                kSession.insert(adduct);
            }

            kSession.fireAllRules();
            kSession.dispose();

            double rawScore = lipid_ExpertKnowledge.getScore();
            maxScore = scorePresence * lipid_ExpertKnowledge.getAppliedPresence() + scoreIntensity * lipid_ExpertKnowledge.getAppliedIntensity();

            double normScore = lipid_ExpertKnowledge.normalizeScore(rawScore, maxScore);
            lipid_ExpertKnowledge.setFinalScore(normScore);
        }
        if (!detectedLipids.isEmpty()) {
            for (FoundLipid foundLipid : detectedLipids) {
                if (foundLipid != null) {
                    row.addLipidValidation(foundLipid);
                }
            }
        }
    }

    /**
     * Finds a .drl file from the folder with user-input rule files that matches the lipid abbreviation in the file name.
     * @param folder user input folder with rule files.
     * @param abbreviation lipid abbreviation to search for in the file names.
     * @return Path for the file that contains the lipid abbreviation in the name.
     * @throws IOException In case the folder is not available.
     */
    public static Path findFirstFileWithAbbreviation(Path folder, String abbreviation) throws IOException {
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("Not a valid folder: " + folder);
        }

        try (Stream<Path> files = Files.list(folder)) {
            return files
                    .filter(path -> path.getFileName().toString().contains(abbreviation))
                    .findFirst()
                    .orElse(null);
        }
    }



    /**
     * Finds lipids in ESI- from adducts found for a group.
     * It obtains all the necessary information from the MatchedLipid, and creates a new FoundLipid with it. Then, depending on the abbreviation of the MatchedLipid, it will set a path, a set of double parameters to get the score at the end, and it sets the Lipid in FoundLipid. It then generates all the necessary parameters to fire the rules in the file specified by the path and it calculates the final score.
     * Finally, it adds the results to the FeatureListRow to be able to see them in the output.
     * @param row FeatureListRow that has Lipid Annotation.
     * @param lipid MatchedLipid for the row.
     * @param found List of FoundAdducts for the group the row is part of.
     */
    public static void findLipidsNegative(FeatureListRow row, MatchedLipid lipid, List<FoundAdduct> found, List<MobilePhases> mobilePhases, VirtualRowGroup virtualGroup) throws IOException {
        // Find matching lipids based on detected adducts, re-direct to drl file depending on LipidMatched
        List<FoundLipid> detectedLipids = new ArrayList<>();

        FoundLipid lipid_ExpertKnowledge = new FoundLipid(lipid);
        lipid_ExpertKnowledge.setSubgroupID(virtualGroup.getSubgroupID());
        lipid_ExpertKnowledge.setAdducts(found);
        String abbr = lipid.getLipidAnnotation().getLipidClass().getAbbr();
        String name = lipid.getLipidAnnotation().getLipidClass().getName();
        String path = null;
        //To normalize final score
        double scorePresence = 1.00;
        double scoreIntensity = 2.00;
        //double totalMaxPresence;
        //double totalMaxIntensity;
        double maxScore = 0.00;

        String[] negativeNames = {"Cer", "DG", "FA", "HexCer", "LPC", "LPE", "LPI", "PC", "PE", "PI", "SM"};
        boolean isUserFile;

        if (Arrays.asList(negativeNames).contains(abbr)) {
            isUserFile = false;
            if (abbr.equals("Cer")) {
                lipid_ExpertKnowledge.setLipid(Lipid.Cer);
                path = "rules_id_lipid_expert_knowledge/negative/Cer_NegativeCheck.drl";
                //totalMaxPresence = 4;
                //totalMaxIntensity = 4;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("DG")) {
                lipid_ExpertKnowledge.setLipid(Lipid.DG);
                path = "rules_id_lipid_expert_knowledge/negative/DG_NegativeCheck.drl";
                //totalMaxPresence = 3;
                //totalMaxIntensity = 2;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("HexCer")) {
                lipid_ExpertKnowledge.setLipid(Lipid.HexCer);
                path = "rules_id_lipid_expert_knowledge/negative/HexCer_NegativeCheck.drl";
                //totalMaxPresence = 4;
                //totalMaxIntensity = 3;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("LPC") && name.equals("Monoalkylglycerophosphocholines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.LPC_OP);
                path = "rules_id_lipid_expert_knowledge/negative/LPC_OP_NegativeCheck.drl";
                //totalMaxPresence = 8;
                //totalMaxIntensity = 1;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("LPC") && name.equals("Monoacylglycerophosphocholines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.LPC);
                path = "rules_id_lipid_expert_knowledge/negative/LPC_NegativeCheck.drl";
                //totalMaxPresence = 8;
                //totalMaxIntensity = 7;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("LPE") && name.equals("Monoalkylglycerophosphoethanolamines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.LPE_OP);
                path = "rules_id_lipid_expert_knowledge/negative/LPE_OP_NegativeCheck.drl";
                //totalMaxPresence = 2;
                //totalMaxIntensity = 0;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("LPE") && name.equals("Monoacylglycerophosphoethanolamines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.LPE);
                path = "rules_id_lipid_expert_knowledge/negative/LPE_NegativeCheck.drl";
                //totalMaxPresence = 2;
                //totalMaxIntensity = 1;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("LPI")) {
                lipid_ExpertKnowledge.setLipid(Lipid.LPI);
                path = "rules_id_lipid_expert_knowledge/negative/LPI_NegativeCheck.drl";
                //totalMaxPresence = 2;
                //totalMaxIntensity = 0;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("PE") && !name.equals("Diacylglycerophosphoethanolamines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.PE_OP);
                path = "rules_id_lipid_expert_knowledge/negative/PE_OP_NegativeCheck.drl";
                //totalMaxPresence = 2;
                //totalMaxIntensity = 1;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("PE") && name.equals("Diacylglycerophosphoethanolamines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.PE);
                path = "rules_id_lipid_expert_knowledge/negative/PE_NegativeCheck.drl";
                //totalMaxPresence = 2;
                //totalMaxIntensity = 1;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("PI")) {
                lipid_ExpertKnowledge.setLipid(Lipid.PI);
                path = "rules_id_lipid_expert_knowledge/negative/PI_NegativeCheck.drl";
                //totalMaxPresence = 2;
                //totalMaxIntensity = 1;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("SM")) {
                lipid_ExpertKnowledge.setLipid(Lipid.SM);
                path = "rules_id_lipid_expert_knowledge/negative/SM_NegativeCheck.drl";
                //totalMaxPresence = 8;
                //totalMaxIntensity = 6;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("PC") && name.equals("Diacylglycerophosphocholines")) {
                lipid_ExpertKnowledge.setLipid(Lipid.PC);
                path = "rules_id_lipid_expert_knowledge/negative/PC_NegativeCheck.drl";
                //totalMaxPresence = 8;
                //totalMaxIntensity = 6;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("PC") && (name.equals("Alkylacylglycerophosphocholines") || name.equals("Dialkylglycerophosphocholines"))) {
                lipid_ExpertKnowledge.setLipid(Lipid.PC_OP);
                path = "rules_id_lipid_expert_knowledge/negative/PC_OP_NegativeCheck.drl";
                //totalMaxPresence = 8;
                //totalMaxIntensity = 3;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            } else if (abbr.equals("FA")) {
                lipid_ExpertKnowledge.setLipid(Lipid.FA);
                path = "rules_id_lipid_expert_knowledge/negative/FA_NegativeCheck.drl";
                //totalMaxPresence = 2;
                //totalMaxIntensity = 1;
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            }
        } else { //does not match one of the default drl files
            isUserFile = true;
            Path userFolder = Paths.get(System.getProperty("user.dir"),
                    "mzmine-community/src/main/resources/rules_id_lipid_expert_knowledge/negative/userFiles");
            Path path1 = findFirstFileWithAbbreviation(userFolder, abbr);
            if (path1 != null) {
                path = String.valueOf(path1);
                //continue logic
                //totalMaxPresence = countPresenceRules(path1);
                //totalMaxIntensity = countPositiveIntensityRules(path1);
                //maxScore = scorePresence * totalMaxPresence + scoreIntensity * totalMaxIntensity;
            }

        }

        detectedLipids.add(lipid_ExpertKnowledge);

        if (path != null) {
            // Initialize KieServices
            KieServices ks = KieServices.Factory.get();
            // Create the KieFileSystem
            var kfs = ks.newKieFileSystem();

            if (!isUserFile) {
                Resource resource = ks.getResources().newClassPathResource(path);
                kfs.write("src/main/resources/" + path, resource);
            } else if (isUserFile) {
                Resource resource = ks.getResources().newFileSystemResource(path);
                kfs.write(resource);
            }

            var kBuilder = ks.newKieBuilder(kfs);
            kBuilder.buildAll();

            var kContainer = ks.newKieContainer(kBuilder.getKieModule().getReleaseId());
            KieSession kSession = kContainer.newKieSession();

            kSession.setGlobal("lipid", lipid_ExpertKnowledge);
            kSession.setGlobal("mobilePhases", mobilePhases);

            for (FoundAdduct adduct : found) {
                kSession.insert(adduct);
            }

            kSession.fireAllRules();
            kSession.dispose();

            double rawScore = lipid_ExpertKnowledge.getScore();
            maxScore = scorePresence * lipid_ExpertKnowledge.getAppliedPresence() + scoreIntensity * lipid_ExpertKnowledge.getAppliedIntensity();
            double normScore = lipid_ExpertKnowledge.normalizeScore(rawScore, maxScore);
            lipid_ExpertKnowledge.setFinalScore(normScore);
        }

        if (!detectedLipids.isEmpty()) {
            for (FoundLipid foundLipid : detectedLipids) {
                if (foundLipid != null) {
                    row.addLipidValidation(foundLipid);
                }
            }
        }
    }

    /**
     * Counts the number of positive presence rules in user-input rule files.
     * @param drlFilePath Path for the file to read and count in.
     * @return Number of positive presence rules.
     * @throws IOException In case the file is not available.
     */
    public static int countPresenceRules(Path drlFilePath) throws IOException {
        String content = Files.readString(drlFilePath);
        String[] ruleBlocks = content.split("(?i)rule\\s+");

        int count = 0;
        for (String block : ruleBlocks) {
            String header = block.split("\n", 2)[0];
            if (header.contains("Presence")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts the number of positive intensity rules in user-input rule files.
     * @param drlFilePath Path for the file to read and count in.
     * @return Number of positive intensity rules.
     * @throws IOException In case the file is not available.
     */
    public static int countPositiveIntensityRules(Path drlFilePath) throws IOException {
        String content = Files.readString(drlFilePath);
        String[] ruleBlocks = content.split("(?i)rule\\s+");

        int count = 0;
        for (String block : ruleBlocks) {
            String header = block.split("\n", 2)[0];
            if (header.contains("Intensity") && header.contains(">")) {
                count++;
            }
        }
        return count;
    }

}

