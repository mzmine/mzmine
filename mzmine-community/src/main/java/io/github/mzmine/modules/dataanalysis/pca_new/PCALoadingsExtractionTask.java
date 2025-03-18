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

package io.github.mzmine.modules.dataanalysis.pca_new;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.chartbasics.listener.RegionSelectionListener;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.modules.MZmineModule;
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

public class PCALoadingsExtractionTask extends AbstractFeatureListTask {

  private final MZmineProject project;
  private final ModularFeatureList resultFlist;

  protected PCALoadingsExtractionTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass, MZmineProject project) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.project = project;
    resultFlist = FeatureListUtils.createCopy(
        parameters.getValue(PCALoadingsExtractionParameters.flist).getMatchingFeatureLists()[0],
        " extracted", storage, false);
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(resultFlist);
  }

  @Override
  protected void process() {
    final var param = (PCALoadingsExtractionParameters) parameters;
    final PCAModel pcaModel = param.toPcaModel();
    final PCAUpdateTask task = new PCAUpdateTask("task", pcaModel);
    if (!task.checkPreConditions()) {
      error("Cannot build PCA. Please check the parameters, metadata may be missing.");
    }

    task.process();
    task.updateGuiModel();
    final List<PCALoadingsProvider> loadingProviders = pcaModel.getLoadingsDatasets().stream()
        .map(DatasetAndRenderer::dataset)
        .filter(ds -> ds.getValueProvider() instanceof PCALoadingsProvider)
        .map(ds -> (PCALoadingsProvider) ds.getValueProvider()).toList();
    final List<Path2D> regions = param.getValue(PCALoadingsExtractionParameters.regions).stream()
        .map(RegionSelectionListener::getShape).toList();

    List<FeatureListRow> rows = new ArrayList<>();
    for (Path2D region : regions) {
      for (PCALoadingsProvider loadings : loadingProviders) {
        for (int i = 0; i < loadings.getValueCount(); i++) {
          if (region.contains(loadings.getDomainValue(i), loadings.getRangeValue(i))) {
            rows.add(new ModularFeatureListRow(resultFlist,
                (ModularFeatureListRow) loadings.getItemObject(i), true));
          }
        }
      }
    }
    resultFlist.setRows(rows);
    project.addFeatureList(resultFlist);
  }

  @Override
  public String getTaskDescription() {
    return "Extracting regions from PCA plot. Recalculating PCA...";
  }
}
