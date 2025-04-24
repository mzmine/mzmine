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

/**
 * Task of the module Lipid Validation
 *
 * @author Blanca Pueche Granados (blancapueche@gmail.com)
 */
public class LipidIDExpertKnowledgeTask extends AbstractTask {

    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private double finishedSteps;
    private double totalSteps;
    private final ParameterSet parameters;
    private MZTolerance mzTolerance;
    private List<MobilePhases> mobilePhase;
    private SampleTypes sampleType;

    private final FeatureList featureList;


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
     * Description of the task
     * @return String with description
     */
    @Override
    public String getTaskDescription() {
        return "Validate lipids in " + featureList;
    }

    /**
     * Percentage of completion
     * @return double with finished percentage
     */
    @Override
    public double getFinishedPercentage() {
        if (totalSteps == 0) {
            return 0;
        }
        return (finishedSteps) / totalSteps;
    }

    /**
     * Canceled task
     * @return boolean
     */
    @Override
    public boolean isCanceled() {
        return super.isCanceled();
    }

    /**
     * Finished task
     * @return boolean
     */
    @Override
    public boolean isFinished() {
        return super.isFinished();
    }

    /**
     * Error message
     * @param message error message
     */
    @Override
    public void error(String message) {
        super.error(message);
    }

    /**
     * Sets when the module is finshed
     * @param runnable
     */
    @Override
    public void setOnFinished(Runnable runnable) {
        super.setOnFinished(runnable);
    }

    /**
     * Module logic
     *
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

        //Group by GroupID of metaCorrelate
        Set<RowGroup> groupRows = new HashSet<>();
        for (FeatureListRow row : rows) {
            RowGroup group = row.getGroup();
            if (group != null) {
                groupRows.add(group);
            }
        }

        //TODO delete this, its just to check
        for (RowGroup r : groupRows) {
            System.out.println("GROUP IDS: " + r.getGroupID() + " rows:" + r.size());
        }

        //Iterate through each row group
        for (RowGroup group : groupRows) {

            //Get polarity of our data
            PolarityType polarityType = getPolarityType(group);

            List<ExpertKnowledge> commonAdductsISF = new ArrayList<>();
            if (polarityType.equals(PolarityType.POSITIVE)) {
                commonAdductsISF.addAll(Arrays.asList(CommonAdductPositive.values()));
                commonAdductsISF.addAll(Arrays.asList(CommonISFPositive.values()));
            } else if (polarityType.equals(PolarityType.NEGATIVE)) {
                commonAdductsISF.addAll(Arrays.asList(CommonAdductNegative.values()));
                commonAdductsISF.addAll(Arrays.asList(CommonISFNegative.values()));
            }
            //sort by mz
            commonAdductsISF.sort(Comparator.comparingDouble(ExpertKnowledge::getMz));

            //Find rows that are annotated in the group
            List<FeatureListRow> annotatedRows = LipidIDExpertKnowledgeSearch.findAnnotatedRows(group);
            //Find relevant data to find adducts
            RowInfo rowInfo = LipidIDExpertKnowledgeSearch.findRowInfo(group);

            if (!annotatedRows.isEmpty()) {
                for (FeatureListRow row : annotatedRows) {
                    List<MatchedLipid> lipidsMatched = row.getLipidMatches();
                    //TODO quitar!?
                    System.out.print("-----Row:" + row.getAverageMZ() + " has lipid matches: " + lipidsMatched);
                    for (MatchedLipid matchedLipid : lipidsMatched) {
                        List<FoundAdduct> foundAdductsAndISF = LipidIDExpertKnowledgeSearch.findAdducts(commonAdductsISF, rowInfo, mzTolerance.getMzTolerance(), row, matchedLipid);

                        if (polarityType.equals(PolarityType.POSITIVE)) {
                            LipidIDExpertKnowledgeSearch.findLipidsPositive(row, matchedLipid, foundAdductsAndISF);
                        } else if (polarityType.equals(PolarityType.NEGATIVE)) {
                            LipidIDExpertKnowledgeSearch.findLipidsNegative(row, matchedLipid, foundAdductsAndISF);
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

        // Add task description to featureList
        (featureList).addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
                "Lipid validation", LipidIDExpertKnowledgeModule.class,
                parameters, getModuleCallDate()));

        setStatus(TaskStatus.FINISHED);
        logger.info("Finished on: " + featureList);
    }

    /**
     * Gets the polarity for a group
     * @param group RowGroup
     * @return PolarityType
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
