package io.github.mzmine.modules.visualization.lipidannotationoverview;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotChart;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotParameters;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYZDataset;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickPlotDataTypes;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class LipidAnnotationOverviewModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "Lipid Annotation Overview";
  private static final String MODULE_DESCRIPTION = "Overview to validate lipid annotations";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @NotNull
  @Override
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    Task newTask = new LipidAnnotationOverviewTask(parameters, moduleCallDate);
    tasks.add(newTask);

    return ExitCode.OK;
  }

  public static void showNewTab(List<ModularFeatureListRow> rows,
      List<ModularFeature> selectedFeatures, FeatureTableFX table) {
    MZmineCore.runLater(() -> {
      LipidAnnotationOverviewWindow newWindow = new LipidAnnotationOverviewWindow(rows,
          selectedFeatures, table);
      newWindow.show();
    });

  }


  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONFEATURELIST;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return LipidAnnotationOverviewParameters.class;
  }

  private static KendrickMassPlotChart buildKendrickMassPlot() {
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
    kendrickMassPlotParameters.setParameter(
        KendrickMassPlotParameters.bubbleSizeCustomKendrickMassBase, "H");
    KendrickMassPlotXYZDataset kendrickMassPlotXYZDataset = new KendrickMassPlotXYZDataset(
        kendrickMassPlotParameters);
    return new KendrickMassPlotChart("Kendrick Mass Plot", "m/z", "KMD (H)", "Retention time",
        kendrickMassPlotXYZDataset);
  }


}
