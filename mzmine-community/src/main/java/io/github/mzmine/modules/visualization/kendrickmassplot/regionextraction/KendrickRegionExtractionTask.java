package io.github.mzmine.modules.visualization.kendrickmassplot.regionextraction;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.gui.chartbasics.listener.RegionSelectionListener;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotParameters;
import io.github.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotXYZDataset;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D.Double;
import java.time.Instant;
import java.util.List;

public class KendrickRegionExtractionTask extends AbstractTask {

  private final ParameterSet kendrickParameters;
  private final List<Path2D> regions;
  private final ParameterSet parameters;
  private final MZmineProject project;
  private final Integer xAxisCharge;
  private final Integer yAxisCharge;
  private final Integer xAxisDivisior;
  private final Integer yAxisDivisor;
  private String suffix;

  public KendrickRegionExtractionTask(ParameterSet parameters, MZmineProject project,
      Instant moduleCallDate) {
    super(moduleCallDate, "Extract regions from regions");

    kendrickParameters = parameters.getEmbeddedParameterValue(
        KendrickRegionExtractionParameters.kendrickParam);
    regions = parameters.getValue(KendrickRegionExtractionParameters.regions).stream()
        .map(RegionSelectionListener::getShape).toList();
    xAxisCharge = parameters.getValue(KendrickRegionExtractionParameters.xAxisCharge);
    yAxisCharge = parameters.getValue(KendrickRegionExtractionParameters.yAxisCharge);
    xAxisDivisior = parameters.getValue(KendrickRegionExtractionParameters.xAxisDivisor);
    yAxisDivisor = parameters.getValue(KendrickRegionExtractionParameters.yAxisDivisor);
    suffix = parameters.getValue(KendrickRegionExtractionParameters.suffix);
    this.parameters = parameters;
    this.project = project;
  }

  @Override
  public String getTaskDescription() {
    return "Extracting regions from plots.";
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    KendrickMassPlotXYZDataset dataset = new KendrickMassPlotXYZDataset(kendrickParameters,
        xAxisDivisior, xAxisCharge, yAxisDivisor, yAxisCharge);
    dataset.run();

    final List<FeatureListRow> rows = regions.stream().<FeatureListRow>mapMulti((r, c) -> {
      for (int i = 0; i < dataset.getItemCount(0); i++) {
        if (isCanceled()) {
          return;
        }

        if (r.contains(new Double(dataset.getXValue(0, i), dataset.getYValue(0, i)))) {
          c.accept(dataset.getItemObject(i));
        }
      }
    }).toList();

    if (isCanceled()) {
      return;
    }

    final ModularFeatureList flist = kendrickParameters.getValue(
        KendrickMassPlotParameters.featureList).getMatchingFeatureLists()[0];

    final ModularFeatureList filtered = new ModularFeatureList(STR."\{flist.getName()} \{suffix}",
        flist.getMemoryMapStorage(), flist.getRawDataFiles().stream().toList());
    DataTypeUtils.copyTypes(flist, filtered, true, true);
    rows.forEach(filtered::addRow);

    filtered.getAppliedMethods().addAll(flist.getAppliedMethods());
    filtered.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(KendrickRegionExtractionModule.class, parameters,
            getModuleCallDate()));

    if (!isCanceled()) {
      project.addFeatureList(filtered);
      setStatus(TaskStatus.FINISHED);
    }
  }
}
