package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.*;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.features.types.annotations.lipidexpertknowledge.LipidValidationListType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.*;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.*;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids.FoundLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.MobilePhases;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.SampleTypes;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Task of the module Lipid Validation module.
 * It contains all the logic for the module.
 *
 * @author Blanca Pueche Granados (blancapueche@gmail.com)
 */
public class LipidIDExpertKnowledgeTask extends AbstractTask {

    /**
     * Logger object to output information in the command line.
     */
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    /**
     * Value to keep track of progress.
     * It updates many times.
     */
    private double finishedSteps;
    /**
     * Value to keep track of progress.
     * It stores the total number of steps needed to finish the task.
     */
    private double totalSteps;
    /**
     * Set of parameters needed to run the module.
     * These are defined in {@link LipidIDExpertKnowledgeParameters}.
     */
    private final ParameterSet parameters;
    /**
     * m/z tolerance set by the user.
     */
    private MZTolerance mzTolerance;
    /**
     * List of possible mobile phases defined in {@link MobilePhases}.
     */
    private List<MobilePhases> mobilePhase;
    /**
     * One of the sample types defined in {@link SampleTypes}.
     */
    private SampleTypes sampleType;
    /**
     * Feature list the module will be run over.
     */
    private final FeatureList featureList;


    /**
     * Creates a new task with the specified info.
     * @param parameters Set of parameters to run the module.
     * @param featureList Feature list the module will be run over.
     * @param moduleCallDate Call date of the module.
     */
    protected LipidIDExpertKnowledgeTask(ParameterSet parameters, FeatureList featureList, @NotNull Instant moduleCallDate) {
        super(null, moduleCallDate);
        this.featureList = featureList;
        this.parameters = parameters;
        this.mzTolerance = parameters.getParameter(LipidIDExpertKnowledgeParameters.mzTolerance).getValue();
        //Convert Objects to MobilePhases
        Object[] selectedMP = parameters.getParameter(LipidIDExpertKnowledgeParameters.mobilePhaseParameter).getValue();
        this.mobilePhase = new ArrayList<>(Arrays.stream(selectedMP).filter(o -> o instanceof MobilePhases).map(o -> (MobilePhases) o).toList());
        //Convert Object to SampleType
        Object[] selectedST = parameters.getParameter(LipidIDExpertKnowledgeParameters.sampleTypeParameter).getValue();
        this.sampleType = Arrays.stream(selectedST).filter(o -> o instanceof SampleTypes).map(o -> (SampleTypes) o).findFirst().orElse(null);
    }

    /**
     * Gets the description of the task.
     * @return The task description.
     */
    @Override
    public String getTaskDescription() {
        return "Validate lipids in " + featureList;
    }

    /**
     * Gets the progress of the task.
     * @return The percentage of completion.
     */
    @Override
    public double getFinishedPercentage() {
        if (totalSteps == 0) {
            return 0;
        }
        return (finishedSteps) / totalSteps;
    }

    /**
     * Cancels the task.
     * @return True if this task has been canceled or stopped due to an error
     */
    @Override
    public boolean isCanceled() {
        return super.isCanceled();
    }

    /**
     * Checks if the status to finished.
     * @return True if this task is finished
     */
    @Override
    public boolean isFinished() {
        return super.isFinished();
    }

    /**
     * Error message.
     * @param message Error message.
     */
    @Override
    public void error(String message) {
        super.error(message);
    }

    /**
     * Sets the input parameter when the module is finished.
     * @param runnable To be run when the task is finished.
     */
    @Override
    public void setOnFinished(Runnable runnable) {
        super.setOnFinished(runnable);
    }

    /**
     * Logic of the module
     * Creates the columns in the feature list where the output will be displayed, and groups the rows by the ID assigned by the metaCorrelate module.
     * Once the rows are grouped, it gets the polarity for each group with getPolarity(...), and based on the result, it creates a List<ExpertKnowledge> with the corresponding adducts and ISF from the enumerations, if it is positive it will use the Positive enumerations and if it is negative it will use the Negative enumerations.
     * It obtains the annotated rows and RowInfo.
     * If there are annotated rows, it iterates through them and finds the adducts, after which it find the lipids calling specific methods.
     * If there are no annotated rows, it only finds the adducts for each row to be displayed in the feature list table.
     */
    @Override
    public void run() {
        //set status to processing
        setStatus(TaskStatus.PROCESSING);
        //The user chooses the feature list (should be Aligned Feature List after metaCorrelate)
        logger.info("Annotating in " + featureList);

        //Adds the columns with the output info
        List<FeatureListRow> rows = featureList.getRows();
        if (featureList instanceof ModularFeatureList) {
            featureList.addRowType(new LipidValidationListType());
        }

        totalSteps = rows.size();


        //OPTION1
        //Group by GroupID of metaCorrelate
        Set<RowGroup> groupRows = new HashSet<>();
        for (FeatureListRow row : rows) {
            RowGroup group = row.getGroup();
            if (group != null) {
                groupRows.add(group);
            }
        }
        // Keep only rows fully connected (correlated with all others)
        /*int originalTotalRows = 0;
        int finalTotalRows = 0;
        for (RowGroup group : groupRows) {
            System.out.print("GROUP ID:"+group.getGroupID());
            System.out.print("---ORIGINAL SIZE:"+group.size());
            originalTotalRows = originalTotalRows + group.size();

            List<FeatureListRow> originalRows = group.getRows();
            List<FeatureListRow> filteredRows = new ArrayList<>();

            int size = originalRows.size();

            for (int i = 0; i < size; i++) {
                boolean correlatedToAll = true;

                for (int j = 0; j < size; j++) {
                    if (i == j) continue;
                    if (!group.isCorrelated(i, j)) {
                        correlatedToAll = false;
                        break;
                    }
                }

                if (correlatedToAll) {
                    filteredRows.add(originalRows.get(i));
                }
            }
            // Replace group content with only the fully correlated rows
            originalRows.clear();
            originalRows.addAll(filteredRows);
            System.out.print("---GROUP SIZE:"+originalRows.size());
            finalTotalRows = finalTotalRows + originalRows.size();
            System.out.println("");
        }

        System.out.println("---FROM: "+originalTotalRows+" WE KEEP: "+finalTotalRows);

         */

        //OPTION2: create subgroups inside each RowGroup of correlated FeatureListRows
        for (RowGroup group : groupRows) {
            int nextSubgroupId = 0;
            List<FeatureListRow> allRows = group.getRows();
            List<List<FeatureListRow>> subgroups = new ArrayList<>();

            for (FeatureListRow row : allRows) {
                boolean added = false;

                for (List<FeatureListRow> subgroup : subgroups) {
                    boolean correlated = true;
                    for (FeatureListRow other : subgroup) {
                        if (!group.isCorrelated(row, other)) {
                            correlated = false;
                            break;
                        }
                    }
                    if (correlated) {
                        subgroup.add(row);
                        added = true;
                        System.out.println("Row " + row.getID() + " added to existing subgroup.");
                        break;
                    }
                }
                if (!added) {
                    List<FeatureListRow> newSubgroup = new ArrayList<>();
                    newSubgroup.add(row);
                    subgroups.add(newSubgroup);
                    System.out.println("Row " + row.getID() + " [hash: " + System.identityHashCode(row) + "] started a new subgroup.");
                }
            }
            for (List<FeatureListRow> subgroup : subgroups) {
                for (FeatureListRow row : subgroup) {
                    row.setComment("");
                }
                String comment = String.format("subgroup_%d", nextSubgroupId++);

                for (FeatureListRow row : subgroup) {
                    row.setComment(comment);

                    // ✅ Debug AFTER setting comment
                    if (row.getComment() == null || row.getComment().isEmpty()) {
                        System.out.println("⚠️ Row " + row.getID() + " did NOT receive a subgroup comment!");
                    } else {
                        System.out.println("✅ Row " + row.getID() + " assigned to " + row.getComment());
                    }
                }
            }
        }

        List<ExpertKnowledge> commonAdductsISF = new ArrayList<>();
        List<FeatureListRow> annotatedRows = new ArrayList<>();

        //Iterate through each row group
        /*for (RowGroup group : groupRows) {
            if (group.size() != 0) {
                annotatedRows.clear();
                commonAdductsISF.clear();

                //Get polarity of our data
                PolarityType polarityType = getPolarityType(group);

                commonAdductsISF = new ArrayList<>();
                if (polarityType.equals(PolarityType.POSITIVE)) {
                    commonAdductsISF.addAll(Arrays.asList(CommonAdductPositive.values()));
                    commonAdductsISF.addAll(Arrays.asList(CommonISFPositive.values()));
                } else if (polarityType.equals(PolarityType.NEGATIVE)) {
                    commonAdductsISF.addAll(Arrays.asList(CommonAdductNegative.values()));
                    commonAdductsISF.addAll(Arrays.asList(CommonISFNegative.values()));
                }
                //sort by mz
                commonAdductsISF.sort(Comparator.comparingDouble(ExpertKnowledge::getMz));

                //check and delete adducts that can't appear due to mobile phases
                if (polarityType.equals(PolarityType.POSITIVE)) {
                    if (!(mobilePhase.contains(MobilePhases.NH4) && mobilePhase.contains(MobilePhases.CH3CN) && mobilePhase.contains(MobilePhases.CH3OH))) {
                        commonAdductsISF.remove(CommonAdductPositive.M_PLUS_C2H7N2);
                        commonAdductsISF.remove(CommonAdductPositive.M_PLUS_NH4);
                    } else if (!mobilePhase.contains(MobilePhases.NH4)) {
                        commonAdductsISF.remove(CommonAdductPositive.M_PLUS_NH4);
                    }
                } else if (polarityType.equals(PolarityType.NEGATIVE)) {
                    if (!(mobilePhase.contains(MobilePhases.CH3COO) && mobilePhase.contains(MobilePhases.HCOO))) {
                        commonAdductsISF.remove(CommonAdductNegative.M_MINUS_H_PLUS_CH3COONa);
                        commonAdductsISF.remove(CommonAdductNegative.M_PLUS_CH3COO);
                        commonAdductsISF.remove(CommonAdductNegative.M_PLUS_CH3COO_PLUS_CH3COONa);
                        commonAdductsISF.remove(CommonAdductNegative.M_PLUS_CH3COO_PLUS_2CH3COONa);
                        commonAdductsISF.remove(CommonAdductNegative.M_PLUS_CH3COO_PLUS_3CH3COONa);
                        commonAdductsISF.remove(CommonAdductNegative.M_PLUS_HCOO_PLUS_CH3COONa);
                        commonAdductsISF.remove(CommonAdductNegative.M_PLUS_HCOO);
                        commonAdductsISF.remove(CommonISFNegative.M_PLUS_CH3COO_MINUS_CH3COOCH3);
                    } else if (!mobilePhase.contains(MobilePhases.CH3COO)) {
                        commonAdductsISF.remove(CommonAdductNegative.M_MINUS_H_PLUS_CH3COONa);
                        commonAdductsISF.remove(CommonAdductNegative.M_PLUS_CH3COO);
                        commonAdductsISF.remove(CommonAdductNegative.M_PLUS_CH3COO_PLUS_CH3COONa);
                        commonAdductsISF.remove(CommonAdductNegative.M_PLUS_CH3COO_PLUS_2CH3COONa);
                        commonAdductsISF.remove(CommonAdductNegative.M_PLUS_CH3COO_PLUS_3CH3COONa);
                        commonAdductsISF.remove(CommonAdductNegative.M_PLUS_HCOO_PLUS_CH3COONa);
                        commonAdductsISF.remove(CommonISFNegative.M_PLUS_CH3COO_MINUS_CH3COOCH3);
                    } else if (!mobilePhase.contains(MobilePhases.HCOO)) {
                        commonAdductsISF.remove(CommonAdductNegative.M_PLUS_HCOO_PLUS_CH3COONa);
                        commonAdductsISF.remove(CommonAdductNegative.M_PLUS_HCOO);
                    }
                }

                //Find rows that are annotated in the group
                annotatedRows.addAll(LipidIDExpertKnowledgeSearch.findAnnotatedRows(group));
                //Find relevant data to find adducts
                RowInfo rowInfo = LipidIDExpertKnowledgeSearch.findRowInfo(group);

                if (!annotatedRows.isEmpty()) {
                    String[] positiveNames = {"CAR", "CE", "Cer", "Chol", "DG", "HexCer", "LPC", "LPE", "LPI", "PC", "PE", "PI", "SM", "TG"};
                    String[] negativeNames = {"Cer", "DG", "FA", "HexCer", "LPC", "LPE", "LPI", "PC", "PE", "PI", "SM"};

                    for (FeatureListRow row : annotatedRows) {
                        List<MatchedLipid> lipidsMatched = row.getLipidMatches();
                        for (MatchedLipid matchedLipid : lipidsMatched) {
                            String abbr = matchedLipid.getLipidAnnotation().getLipidClass().getAbbr();
                            List<FoundAdduct> foundAdductsAndISF = LipidIDExpertKnowledgeSearch.findAdducts(commonAdductsISF, rowInfo, mzTolerance.getMzTolerance(), row, matchedLipid);

                            if (polarityType.equals(PolarityType.POSITIVE) && Arrays.asList(positiveNames).contains(abbr)) {
                                LipidIDExpertKnowledgeSearch.findLipidsPositive(row, matchedLipid, foundAdductsAndISF, mobilePhase);
                            } else if (polarityType.equals(PolarityType.NEGATIVE) && Arrays.asList(negativeNames).contains(abbr)) {
                                LipidIDExpertKnowledgeSearch.findLipidsNegative(row, matchedLipid, foundAdductsAndISF, mobilePhase);
                            }
                        }
                    }
                } else {
                    for (FeatureListRow row : group.getRows()) {
                        MatchedLipid match = null;
                        List<FoundAdduct> foundAdductsAndISF = LipidIDExpertKnowledgeSearch.findAdducts(commonAdductsISF, rowInfo, mzTolerance.getMzTolerance(), row, match);

                        FoundLipid foundLipid = new FoundLipid(foundAdductsAndISF);
                        row.addLipidValidation(foundLipid);
                    }
                }

                finishedSteps++;
            }
        }*/
        for (RowGroup group : groupRows) {
            if (group.size() == 0) continue;

            // Agrupar filas por subgrupo usando el campo comment
            Map<String, List<FeatureListRow>> subgroups = group.getRows().stream()
                    .collect(Collectors.groupingBy(row -> {
                        String comment = row.getComment();
                        return (comment != null && !comment.isEmpty()) ? comment : "unassigned";
                    }));

            System.out.println("RowGroup ID: " + group.getGroupID() +
                    " → Subgroups: " + subgroups.size());

            for (Map.Entry<String, List<FeatureListRow>> entry : subgroups.entrySet()) {
                String subgroup = entry.getKey();
                List<FeatureListRow> rows2 = entry.getValue();
                for (FeatureListRow row : rows2) {
                    System.out.println("  Feature ID: " + row.getID() + " [hash: " + System.identityHashCode(row) + "] → Comment/Subgroup: " + subgroup);
                }
            }


            for (Map.Entry<String, List<FeatureListRow>> entry : subgroups.entrySet()) {
                List<FeatureListRow> subgroupRows = entry.getValue();
                String comment = entry.getKey();

                // Extract x from "subgroup_x", default to -1 if not parsable
                int subgroupId = -1;
                if (comment != null && comment.startsWith("subgroup_")) {
                    try {
                        subgroupId = Integer.parseInt(comment.substring("subgroup_".length()));
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: could not parse subgroup ID from comment: " + comment);
                    }
                }

                VirtualRowGroup virtualGroup = new VirtualRowGroup(subgroupRows, group.getGroupID(), subgroupId);


                if (subgroupRows.isEmpty()) continue;

                annotatedRows.clear();
                commonAdductsISF.clear();

                // Crear RowGroup virtual si lo necesitas para funciones posteriores
                PolarityType polarityType = getPolarityType(virtualGroup);

                // Adducts según polaridad
                if (polarityType.equals(PolarityType.POSITIVE)) {
                    commonAdductsISF.addAll(Arrays.asList(CommonAdductPositive.values()));
                    commonAdductsISF.addAll(Arrays.asList(CommonISFPositive.values()));
                } else if (polarityType.equals(PolarityType.NEGATIVE)) {
                    commonAdductsISF.addAll(Arrays.asList(CommonAdductNegative.values()));
                    commonAdductsISF.addAll(Arrays.asList(CommonISFNegative.values()));
                }

                // Orden por m/z
                commonAdductsISF.sort(Comparator.comparingDouble(ExpertKnowledge::getMz));

                // Filtros por fase móvil
                if (polarityType.equals(PolarityType.POSITIVE)) {
                    if (!(mobilePhase.contains(MobilePhases.NH4) &&
                            mobilePhase.contains(MobilePhases.CH3CN) &&
                            mobilePhase.contains(MobilePhases.CH3OH))) {
                        commonAdductsISF.remove(CommonAdductPositive.M_PLUS_C2H7N2);
                        commonAdductsISF.remove(CommonAdductPositive.M_PLUS_NH4);
                    } else if (!mobilePhase.contains(MobilePhases.NH4)) {
                        commonAdductsISF.remove(CommonAdductPositive.M_PLUS_NH4);
                    }
                } else {
                    if (!(mobilePhase.contains(MobilePhases.CH3COO) && mobilePhase.contains(MobilePhases.HCOO))) {
                        commonAdductsISF.removeAll(Arrays.asList(
                                CommonAdductNegative.M_MINUS_H_PLUS_CH3COONa,
                                CommonAdductNegative.M_PLUS_CH3COO,
                                CommonAdductNegative.M_PLUS_CH3COO_PLUS_CH3COONa,
                                CommonAdductNegative.M_PLUS_CH3COO_PLUS_2CH3COONa,
                                CommonAdductNegative.M_PLUS_CH3COO_PLUS_3CH3COONa,
                                CommonAdductNegative.M_PLUS_HCOO_PLUS_CH3COONa,
                                CommonAdductNegative.M_PLUS_HCOO,
                                CommonISFNegative.M_PLUS_CH3COO_MINUS_CH3COOCH3
                        ));
                    } else if (!mobilePhase.contains(MobilePhases.CH3COO)) {
                        commonAdductsISF.removeAll(Arrays.asList(
                                CommonAdductNegative.M_MINUS_H_PLUS_CH3COONa,
                                CommonAdductNegative.M_PLUS_CH3COO,
                                CommonAdductNegative.M_PLUS_CH3COO_PLUS_CH3COONa,
                                CommonAdductNegative.M_PLUS_CH3COO_PLUS_2CH3COONa,
                                CommonAdductNegative.M_PLUS_CH3COO_PLUS_3CH3COONa,
                                CommonAdductNegative.M_PLUS_HCOO_PLUS_CH3COONa,
                                CommonISFNegative.M_PLUS_CH3COO_MINUS_CH3COOCH3
                        ));
                    } else if (!mobilePhase.contains(MobilePhases.HCOO)) {
                        commonAdductsISF.removeAll(Arrays.asList(
                                CommonAdductNegative.M_PLUS_HCOO_PLUS_CH3COONa,
                                CommonAdductNegative.M_PLUS_HCOO
                        ));
                    }
                }

                annotatedRows.addAll(LipidIDExpertKnowledgeSearch.findAnnotatedRows(virtualGroup));
                RowInfo rowInfo = LipidIDExpertKnowledgeSearch.findRowInfo(virtualGroup);

                if (!annotatedRows.isEmpty()) {
                    String[] positiveNames = {"CAR", "CE", "Cer", "Chol", "DG", "HexCer", "LPC", "LPE", "LPI", "PC", "PE", "PI", "SM", "TG"};
                    String[] negativeNames = {"Cer", "DG", "FA", "HexCer", "LPC", "LPE", "LPI", "PC", "PE", "PI", "SM"};

                    for (FeatureListRow row : annotatedRows) {
                        List<MatchedLipid> lipidsMatched = row.getLipidMatches();
                        for (MatchedLipid matchedLipid : lipidsMatched) {
                            String abbr = matchedLipid.getLipidAnnotation().getLipidClass().getAbbr();
                            List<FoundAdduct> foundAdductsAndISF = LipidIDExpertKnowledgeSearch.findAdducts(
                                    commonAdductsISF, rowInfo, mzTolerance.getMzTolerance(), row, matchedLipid);

                            if (polarityType.equals(PolarityType.POSITIVE) && Arrays.asList(positiveNames).contains(abbr)) {
                                LipidIDExpertKnowledgeSearch.findLipidsPositive(row, matchedLipid, foundAdductsAndISF, mobilePhase, virtualGroup);
                            } else if (polarityType.equals(PolarityType.NEGATIVE) && Arrays.asList(negativeNames).contains(abbr)) {
                                LipidIDExpertKnowledgeSearch.findLipidsNegative(row, matchedLipid, foundAdductsAndISF, mobilePhase, virtualGroup);
                            }
                        }
                    }
                } else {
                    for (FeatureListRow row : subgroupRows) {
                        MatchedLipid match = null;
                        List<FoundAdduct> foundAdductsAndISF = LipidIDExpertKnowledgeSearch.findAdducts(
                                commonAdductsISF, rowInfo, mzTolerance.getMzTolerance(), row, match);

                        FoundLipid foundLipid = new FoundLipid(foundAdductsAndISF, virtualGroup);
                        row.addLipidValidation(foundLipid);
                    }
                }

                finishedSteps++;
            }
        }


        // Add task description to featureList
        (featureList).addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
                "Lipid validation", LipidIDExpertKnowledgeModule.class,
                parameters, getModuleCallDate()));

        setStatus(TaskStatus.FINISHED);
        logger.info("Finished on: " + featureList);
    }

    /**
     * Gets the polarity for a group of rows.
     * @param group The group of rows.
     * @return The polarity type for the group.
     */
    @NotNull
    private PolarityType getPolarityType(RowGroup group) {
        List<FeatureListRow> rows = group.getRows();
        FeatureListRow row = rows.get(0);
        RawDataFile rdf = row.getFeatures().get(0).getRawDataFile();
        PolarityType polarity = rdf.getDataPolarity().get(0);
        return polarity;
    }
}
