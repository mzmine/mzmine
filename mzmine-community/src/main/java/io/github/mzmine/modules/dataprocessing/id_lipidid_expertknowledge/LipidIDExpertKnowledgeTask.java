package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge;

import com.lowagie.text.Row;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.lipidexpertknowledge.LipidValidationListType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClassParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.*;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.*;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids.FoundLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.MobilePhaseParameter;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.MobilePhases;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.SampleTypeParameter;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.SampleTypes;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

public class LipidIDExpertKnowledgeTask extends AbstractTask {

    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private double finishedSteps;
    private double totalSteps;
    private final ParameterSet parameters;
    private MZTolerance mzTolerance;
    //TODO this is new, to differentiate between the rules! (when we get them)
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

    @Override
    public String getTaskDescription() {
        return "Annotate lipids in " + featureList;
    }

    @Override
    public double getFinishedPercentage() {
        if (totalSteps == 0) {
            return 0;
        }
        return (finishedSteps) / totalSteps;
    }

    @Override
    public boolean isCanceled() {
        return super.isCanceled();
    }

    @Override
    public boolean isFinished() {
        return super.isFinished();
    }

    @Override
    public void error(String message) {
        super.error(message);
    }

    @Override
    public void setOnFinished(Runnable runnable) {
        super.setOnFinished(runnable);
    }

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
        //Get polarity of our data
        Set<PolarityType> polarityTypes = getPolarityTypes();

        //Combined list with all the adducts and ISFs, later sorted by mz
        //It will search in different lists depending on polarity of our data (+) || (-) || (+-)
        List<ExpertKnowledge> commonAdductsISF = new ArrayList<>();
        if (polarityTypes.contains(PolarityType.POSITIVE) && polarityTypes.contains(PolarityType.NEGATIVE)) {
            commonAdductsISF.addAll(Arrays.asList(CommonAdductPositive.values()));
            commonAdductsISF.addAll(Arrays.asList(CommonAdductNegative.values()));
            commonAdductsISF.addAll(Arrays.asList(CommonISFPositive.values()));
            commonAdductsISF.addAll(Arrays.asList(CommonISFNegative.values()));
        } else if (polarityTypes.contains(PolarityType.POSITIVE)) {
            commonAdductsISF.addAll(Arrays.asList(CommonAdductPositive.values()));
            commonAdductsISF.addAll(Arrays.asList(CommonISFPositive.values()));
        } else if (polarityTypes.contains(PolarityType.NEGATIVE)) {
            commonAdductsISF.addAll(Arrays.asList(CommonAdductNegative.values()));
            commonAdductsISF.addAll(Arrays.asList(CommonISFNegative.values()));
        }
        //sort by mz
        commonAdductsISF.sort(Comparator.comparingDouble(ExpertKnowledge::getMz));

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
            //The module will only work if the row has annotations, if not, it won't run
            List<FoundLipid> detectedLipids = new ArrayList<>();

            //findAdducts function in Utils
            List<FoundAdduct> foundAdductsAndISF = LipidIDExpertKnowledgeSearch.findAdducts(commonAdductsISF, group, mzTolerance.getMzTolerance());

            if (foundAdductsAndISF.isEmpty()) {
                //nothing
            } else {
                //TODO separar en positive y negative adducts para distribuir a rules
                /*List<FoundAdduct> positiveAdductsAndISF = new ArrayList<>();
                List<FoundAdduct> negativeAdductsAndISF = new ArrayList<>();
                for (FoundAdduct adduct : foundAdductsAndISF) {
                    if (adduct.getCharge() == 1) {
                        positiveAdductsAndISF.add(adduct);
                    } else if (adduct.getCharge() == -1) {
                        negativeAdductsAndISF.add(adduct);
                    }
                }*/
                //Based on polarity re-direct to specific rules
                if (polarityTypes.contains(PolarityType.POSITIVE) && polarityTypes.contains(PolarityType.NEGATIVE)) {
                    List<FoundLipid> detectedLipidsPos = LipidIDExpertKnowledgeSearch.findLipidsPositive(group, foundAdductsAndISF);
                    List<FoundLipid> detectedLipidsNeg = LipidIDExpertKnowledgeSearch.findLipidsNegative(group, foundAdductsAndISF);
                    detectedLipids = new ArrayList<>();
                    detectedLipids.addAll(detectedLipidsPos);
                    detectedLipids.addAll(detectedLipidsNeg);
                } else if (polarityTypes.contains(PolarityType.POSITIVE)) {
                    detectedLipids = LipidIDExpertKnowledgeSearch.findLipidsPositive(group, foundAdductsAndISF);
                } else if (polarityTypes.contains(PolarityType.NEGATIVE)) {
                    detectedLipids = LipidIDExpertKnowledgeSearch.findLipidsNegative(group, foundAdductsAndISF);
                }

                finishedSteps++;


                for (FoundLipid fl : detectedLipids) {
                    if (fl != null) {
                        for (FeatureListRow row : group.getRows()) {
                            row.addLipidValidation(fl);
                        }
                    }
                }
            }
        }

        // Add task description to featureList
        (featureList).addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
                "Lipid annotation with expert knowledge", LipidIDExpertKnowledgeModule.class,
                parameters, getModuleCallDate()));

        setStatus(TaskStatus.FINISHED);
        logger.info("Finished on: " + featureList);
    }

    @NotNull
    private Set<PolarityType> getPolarityTypes() {
        Set<PolarityType> polarityTypes = new HashSet<>();
        ObservableList<RawDataFile> rawDataFiles = featureList.getRawDataFiles();
        for (RawDataFile raw : rawDataFiles) {
            List<PolarityType> dataPolarity = raw.getDataPolarity();
            polarityTypes.addAll(dataPolarity);
        }
        return polarityTypes;
    }
}
