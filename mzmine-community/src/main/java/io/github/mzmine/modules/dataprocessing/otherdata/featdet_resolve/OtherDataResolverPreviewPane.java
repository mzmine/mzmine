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

package io.github.mzmine.modules.dataprocessing.otherdata.featdet_resolve;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.previewpane.AbstractPreviewPane;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.javafx.OtherFeatureSelectionPane;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OtherDataResolverPreviewPane extends AbstractPreviewPane<OtherFeature> {

  private final OtherFeatureSelectionPane selectionPane;

  public OtherDataResolverPreviewPane(ParameterSet parameters) {
    super(parameters);

    selectionPane = new OtherFeatureSelectionPane();
    setBottom(selectionPane);
  }

  @Override
  public @NotNull SimpleXYChart<PlotXYDataProvider> createChart() {
    final SimpleXYChart<PlotXYDataProvider> preview = new SimpleXYChart<>("Preview");
    return preview;
  }

  @Override
  public @NotNull List<@NotNull DatasetAndRenderer> calculateNewDatasets(
      @Nullable OtherFeature valueForPreview) {
    if (valueForPreview == null) {
      return List.of();
    }

    if (!parameters.checkParameterValues(new ArrayList<>())) {
      return List.of();
    }

    final ParameterSet resolverParameters = ((OtherDataResolverParameters) parameters).toResolverParameters();
    final MinimumSearchFeatureResolver resolver = new MinimumSearchFeatureResolver(
        resolverParameters,
        new ModularFeatureList("dummy", null, new RawDataFileImpl("dummy", null, null)));

    final List<OtherFeature> resolved = OtherDataResolverTask.resolveFeatures(
        List.of(valueForPreview), resolver, null);

    final List<DatasetAndRenderer> datasets = new ArrayList<>();
  }

  @Override
  public OtherFeature getValueForPreview() {
    return selectionPane.getFeature();
  }

  @Override
  public void updateChart(@NotNull List<DatasetAndRenderer> datasets,
      @NotNull SimpleXYChart<? extends PlotXYDataProvider> chart) {

  }
}
