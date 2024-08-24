/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.OtherFeatureDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.previewpane.AbstractPreviewPane;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherRawOrProcessed;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.javafx.OtherFeatureSelectionPane;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OtherDataResolverPreviewPane extends AbstractPreviewPane<OtherFeature> {

  private final OtherFeatureSelectionPane selectionPane;

  public OtherDataResolverPreviewPane(ParameterSet parameters) {
    super(parameters);

    selectionPane = new OtherFeatureSelectionPane(OtherRawOrProcessed.PREPROCESSED,
        OtherRawOrProcessed.RAW, OtherRawOrProcessed.FEATURES);
    selectionPane.featureProperty().addListener((_, _, _) -> updatePreview());
    setBottom(selectionPane);
  }

  @Override
  public @NotNull SimpleXYChart<PlotXYDataProvider> createChart() {
    final SimpleXYChart<PlotXYDataProvider> preview = new SimpleXYChart<>("Preview");
    preview.setMinHeight(200);
    preview.setDomainAxisLabel(formats.unit("Retention time", "min"));
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

    final List<OtherFeature> resolvedFeatures = OtherDataResolverTask.resolveFeatures(
        List.of(valueForPreview), resolver, null);

    final List<DatasetAndRenderer> datasets = new ArrayList<>();

    final SimpleColorPalette palette = ConfigService.getDefaultColorPalette();

    datasets.add(new DatasetAndRenderer(new OtherFeatureDataProvider(valueForPreview,
        valueForPreview.getFeatureData().getOtherDataFile().getCorrespondingRawDataFile()
            .getColorAWT()), new ColoredXYLineRenderer()));
    for (OtherFeature resolved : resolvedFeatures) {
      datasets.add(new DatasetAndRenderer(
          new ColoredXYDataset(new OtherFeatureDataProvider(resolved, palette.getNextColorAWT())),
          new ColoredAreaShapeRenderer()));
    }
    return datasets;
  }

  @Override
  public OtherFeature getValueForPreview() {
    return selectionPane.getFeature();
  }

  @Override
  public void updateChart(@NotNull List<DatasetAndRenderer> datasets,
      @NotNull SimpleXYChart<? extends PlotXYDataProvider> chart) {

    chart.applyWithNotifyChanges(false, () -> {

      if (datasets.get(0).dataset().getValueProvider() instanceof OtherFeatureDataProvider prov) {
        final String label = prov.getFeature().getFeatureData().getTimeSeriesData()
            .getTimeSeriesRangeLabel();
        final String unit = prov.getFeature().getFeatureData().getTimeSeriesData()
            .getTimeSeriesRangeUnit();
        chart.setRangeAxisLabel(formats.unit(label, unit));
      }

      chart.removeAllDatasets();
      datasets.forEach(dsr -> chart.addDataset(dsr.dataset(), dsr.renderer()));
    });
  }
}
