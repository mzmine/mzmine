/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

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
import java.awt.geom.Point2D;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

public class RegionExtractionTask extends AbstractTask {

  private final ParameterSet kendrickParameters;
  private final List<Path2D> regions;
  private final ParameterSet parameters;
  private final MZmineProject project;
  private final Integer xAxisCharge;
  private final Integer yAxisCharge;
  private final Integer xAxisDivisior;
  private final Integer yAxisDivisor;
  private String suffix;

  public RegionExtractionTask(ParameterSet parameters, MZmineProject project,
      Instant moduleCallDate) {
    super(moduleCallDate, "Extract regions from regions");

    kendrickParameters = parameters.getEmbeddedParameterValue(
        RegionExtractionParameters.kendrickParam);
    regions = parameters.getValue(RegionExtractionParameters.regions).stream()
        .map(RegionSelectionListener::getShape).toList();
    xAxisCharge = parameters.getValue(RegionExtractionParameters.xAxisCharge);
    yAxisCharge = parameters.getValue(RegionExtractionParameters.yAxisCharge);
    xAxisDivisior = parameters.getValue(RegionExtractionParameters.xAxisDivisor);
    yAxisDivisor = parameters.getValue(RegionExtractionParameters.yAxisDivisor);
    suffix = parameters.getValue(RegionExtractionParameters.suffix);
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

    final List<FeatureListRow> rows = IntStream.range(0, dataset.getItemCount(0)).filter(
            index -> regions.stream().anyMatch(region -> region.contains(
                new Point2D.Double(dataset.getXValue(0, index), dataset.getYValue(0, index)))))
        .mapToObj(dataset::getItemObject).toList();

    if (isCanceled()) {
      return;
    }

    final ModularFeatureList flist = kendrickParameters.getValue(
        KendrickMassPlotParameters.featureList).getMatchingFeatureLists()[0];

    final ModularFeatureList filtered = new ModularFeatureList(flist.getName() + " " + suffix,
        flist.getMemoryMapStorage(), flist.getRawDataFiles().stream().toList());
    DataTypeUtils.copyTypes(flist, filtered, true, true);
    rows.forEach(filtered::addRow);

    filtered.getAppliedMethods().

        addAll(flist.getAppliedMethods());
    filtered.addDescriptionOfAppliedTask(new

        SimpleFeatureListAppliedMethod(RegionExtractionModule.class, parameters,
        getModuleCallDate()));

    if (!isCanceled()) {
      project.addFeatureList(filtered);
      setStatus(TaskStatus.FINISHED);
    }
  }
}
