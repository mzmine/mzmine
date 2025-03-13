package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.*;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LipidIDExpertKnowledgeTask extends AbstractTask{

    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private double finishedSteps;
    private double totalSteps;
    private final ParameterSet parameters;
    private MZTolerance mzTolerance;
    private final FeatureList featureList;


    protected LipidIDExpertKnowledgeTask(ParameterSet parameters, FeatureList featureList, @NotNull Instant moduleCallDate) {
        super(null, moduleCallDate);
        this.featureList = featureList;
        this.parameters = parameters;
        this.mzTolerance = parameters.getParameter(LipidIDExpertKnowledgeParameters.mzTolerance).getValue();
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

        logger.info("Annotating in "+featureList);

        List<FeatureListRow> rows = featureList.getRows();
        if(featureList instanceof ModularFeatureList){
            featureList.addRowType(new LipidMatchListType());
        }
        //TODO finish this: make metaCorrelate a requisite before this maybe with the featurelist description or something similar
        //Checks if the row has any lipid matches (it only alerts, but it doesn't matter, we will check later when comparing both lists)
        for (FeatureListRow r : rows){
            if (r.getLipidMatches().isEmpty()){
                //System.out.println("Row "+ r.getID() +" does not have any previous lipid matches");
            }
        }

        totalSteps = rows.size();
        //Get polarity of our data
        Set<PolarityType> polarityTypes = getPolarityTypes();

        //Combined list with all the adducts and ISFs, later sorted by mz
        //It will search in different lists depending on polarity of our data (+) || (-) || (+-)
        List<ExpertKnowledge> commonAdductsISF = new ArrayList<>();
        if(polarityTypes.contains(PolarityType.POSITIVE) && polarityTypes.contains(PolarityType.NEGATIVE)) {
            commonAdductsISF.addAll(Arrays.asList(CommonAdductPositive.values()));
            commonAdductsISF.addAll(Arrays.asList(CommonAdductNegative.values()));
            commonAdductsISF.addAll(Arrays.asList(CommonISFPositive.values()));
            commonAdductsISF.addAll(Arrays.asList(CommonISFNegative.values()));
            commonAdductsISF.sort(Comparator.comparingDouble(ExpertKnowledge::getMz));
        } else if (polarityTypes.contains(PolarityType.POSITIVE)) {
            commonAdductsISF.addAll(Arrays.asList(CommonAdductPositive.values()));
            commonAdductsISF.addAll(Arrays.asList(CommonISFPositive.values()));
        } else if (polarityTypes.contains(PolarityType.NEGATIVE)) {
            commonAdductsISF.addAll(Arrays.asList(CommonAdductNegative.values()));
            commonAdductsISF.addAll(Arrays.asList(CommonISFNegative.values()));
        }

        //TODO albertos' comments idk if i'll do it:

        // grouped features by RT, find adduct relationships, then provide the evidence (positive or negative). In comments maybe?
        // hacer una implementación de FeatureListRow que incluya un score? Valorar
        // List<GroupedFeaturesListRow>

        //Iterate through each row
        rows.parallelStream().forEach(row -> {

            //findAdducts function in Utils
            List<DetectedAdduct> foundAdductsISF = LipidIDExpertKnowledgeUtils.findAdductsPos(commonAdductsISF, row, mzTolerance.getMzTolerance());

            //TODO findLipids function
            // Find matching lipids based on detected adducts, re-direct to drl file depending on LipidMatched
            List<MatchedLipid> matchedLipids = row.getLipidMatches();
            for (MatchedLipid mL : matchedLipids){
                //Abbreviation of the matched lipid
                String abbreviation = mL.getLipidAnnotation().getLipidClass().getAbbr();

            }
            //List<Lipid> detectedLipids = LipidIDExpertKnowledgeUtils.findLipids(foundAdductsISF);


            finishedSteps++;
        });

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
