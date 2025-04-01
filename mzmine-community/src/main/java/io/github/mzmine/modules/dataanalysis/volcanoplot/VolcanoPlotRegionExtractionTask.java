/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataanalysis.volcanoplot;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.chartbasics.listener.RegionSelectionListener;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestResult;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.awt.geom.Path2D;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VolcanoPlotRegionExtractionTask extends AbstractFeatureListTask {

  private final List<Path2D> regions;
  private final ModularFeatureList flist;
  private final MZmineProject project;
  private final ModularFeatureList resultFlist;

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   * @param parameters
   * @param moduleClass
   */
  protected VolcanoPlotRegionExtractionTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass, MZmineProject project) {
    super(storage, moduleCallDate, parameters, moduleClass);
    regions = parameters.getValue(VolcanoPlotRegionExtractionParameters.regions).stream()
        .map(RegionSelectionListener::getShape).toList();
    flist = parameters.getValue(VolcanoPlotRegionExtractionParameters.flists)
        .getMatchingFeatureLists()[0];
    this.project = project;
    resultFlist = FeatureListUtils.createCopy(flist, " extracted", getMemoryMapStorage(), false);
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(resultFlist);
  }

  @Override
  protected void process() {
    final VolcanoPlotModel model = ((VolcanoPlotRegionExtractionParameters) parameters).toModel();

    // reuse the same processing as we have in the gui
    VolcanoPlotUpdateTask task = new VolcanoPlotUpdateTask(model);
    if (!task.checkPreConditions()) {
      error("Cannot construct Volcano plot dataset. Metadata may be absent.");
      return;
    }
    task.process();
    task.updateGuiModel();
    final List<VolcanoDatasetProvider> datasets = model.getDatasets().stream()
        .map(DatasetAndRenderer::dataset)
        .filter(ds -> ds.getValueProvider() instanceof VolcanoDatasetProvider)
        .map(ds -> (VolcanoDatasetProvider) ds.getValueProvider()).toList();

    List<FeatureListRow> rows = new ArrayList<>();
    for (VolcanoDatasetProvider ds : datasets) {
      for (int i = 0; i < ds.getValueCount(); i++) {
        final RowSignificanceTestResult testResult = ds.getItemObject(i);
        final int finalI = i;
        if (regions.stream().anyMatch(
            region -> region.contains(ds.getDomainValue(finalI), ds.getRangeValue(finalI)))) {
          rows.add(new ModularFeatureListRow(resultFlist, (ModularFeatureListRow) testResult.row(),
              true));
        }
      }
    }
    resultFlist.setRows(rows);
    project.addFeatureList(resultFlist);
  }

  @Override
  public String getTaskDescription() {
    return "Extracting regions from a volcano plot.";
  }
}
