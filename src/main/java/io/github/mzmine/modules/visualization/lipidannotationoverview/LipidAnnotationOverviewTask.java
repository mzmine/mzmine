package io.github.mzmine.modules.visualization.lipidannotationoverview;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotChart;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotParameters;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYZDataset;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickPlotDataTypes;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class LipidAnnotationOverviewTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final ParameterSet parameters;

  private final FeatureList featureList;

  public LipidAnnotationOverviewTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    featureList = parameters.getParameter(LipidAnnotationOverviewParameters.featureList).getValue()
        .getMatchingFeatureLists()[0];
    this.parameters = parameters;
  }

  @Override
  public String getTaskDescription() {
    return "Create lipid overview for " + featureList;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Create lipid overview for " + featureList);
    // Task canceled?
    if (isCanceled()) {
      return;
    }

//    KendrickMassPlotChart kendrickMassPlotChart = buildKendrickMassPlot();
//    MZmineCore.runLater(() -> {
//      FeatureTableFXUtil.addFeatureTableTab(featureList);
//    });
//
//    //find all tabs fitting selected feature table
//    List<FeatureTableFX> linkedFeatureTables = new ArrayList<>();
//    List<MZmineTab> allTabs = MZmineCore.getDesktop().getAllTabs();
//
//    MZmineCore.runLater(() -> {
//      LipidAnnotationOverviewTab newTab = new LipidAnnotationOverviewTab(featureList,
//          kendrickMassPlotChart, linkedFeatureTables);
//      MZmineCore.getDesktop().addTab(newTab);
//    });

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished creating lipid overview of " + featureList);
  }

  private KendrickMassPlotChart buildKendrickMassPlot() {
    //init a dataset
    KendrickMassPlotParameters kendrickMassPlotParameters = new KendrickMassPlotParameters();
    kendrickMassPlotParameters.setParameter(KendrickMassPlotParameters.xAxisValues,
        KendrickPlotDataTypes.M_OVER_Z);
    kendrickMassPlotParameters.setParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase,
        "CH2");
    kendrickMassPlotParameters.setParameter(KendrickMassPlotParameters.yAxisValues,
        KendrickPlotDataTypes.KENDRICK_MASS_DEFECT);
    kendrickMassPlotParameters.setParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase,
        "H");
    kendrickMassPlotParameters.setParameter(KendrickMassPlotParameters.colorScaleValues,
        KendrickPlotDataTypes.RETENTION_TIME);
    kendrickMassPlotParameters.setParameter(
        KendrickMassPlotParameters.colorScaleCustomKendrickMassBase, "H");
    kendrickMassPlotParameters.setParameter(KendrickMassPlotParameters.bubbleSizeValues,
        KendrickPlotDataTypes.INTENSITY);
    KendrickMassPlotXYZDataset kendrickMassPlotXYZDataset = new KendrickMassPlotXYZDataset(
        kendrickMassPlotParameters, 1, 1);
    kendrickMassPlotParameters.setParameter(
        KendrickMassPlotParameters.bubbleSizeCustomKendrickMassBase, "H");
    return new KendrickMassPlotChart("Kendrick Mass Plot", "m/z", "KMD (H)", "Retention time",
        kendrickMassPlotXYZDataset);
  }

}