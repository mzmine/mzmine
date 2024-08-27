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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.previewpane.FeaturePreviewPane;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BaselineCorrectionPreview extends FeaturePreviewPane {

  public BaselineCorrectionPreview(ParameterSet parameterSet) {
    super(parameterSet);
  }

  @Override
  public @NotNull SimpleXYChart<PlotXYDataProvider> createChart() {
    final SimpleXYChart<PlotXYDataProvider> c = new SimpleXYChart<>("Preview",
        formats.unit("Retention time", "min"), formats.unit("Intensity", "a.u."));
    c.setMinWidth(100);
    c.setMinHeight(200);
    return c;
  }

  @Override
  public void updateChart(@NotNull List<DatasetAndRenderer> datasets,
      @NotNull SimpleXYChart<? extends PlotXYDataProvider> chart) {
    chart.applyWithNotifyChanges(false, () -> {
      chart.removeAllDatasets();
      datasets.forEach(dsr -> chart.addDataset(dsr.dataset(), dsr.renderer()));
    });
  }

  @Override
  public @NotNull List<@NotNull DatasetAndRenderer> calculateNewDatasets(
      @Nullable Feature feature) {

    final List<String> errorMessages = new ArrayList<>();
    if (feature == null || !parameters.checkParameterValues(errorMessages, true)) {
      if (!errorMessages.isEmpty()) {
        FxThread.runLater(() -> DialogLoggerUtil.showMessageDialog("Invalid parameter settings",
            "Please double check parameters: " + errorMessages.stream()
                .collect(Collectors.joining(", "))));
      }
      return List.of();
    }

    final BaselineCorrectors enumValue = parameters.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getValue();
    final BaselineCorrector baselineCorrector = enumValue.getModuleInstance().newInstance(
        parameters, null, feature.getFeatureList());
    if (baselineCorrector instanceof AbstractBaselineCorrector uv) {
      uv.setPreview(true);
    }

    final IonTimeSeries<Scan> full = IonTimeSeriesUtils.remapRtAxis(feature.getFeatureData(),
        feature.getFeatureList().getSeletedScans(feature.getRawDataFile()));

    IonTimeSeries<? extends Scan> corrected = baselineCorrector.correctBaseline(full);
    final List<PlotXYDataProvider> additionalPreviewData = baselineCorrector.getAdditionalPreviewData();

    final List<DatasetAndRenderer> data = new ArrayList<>();

    data.addAll(List.of(new DatasetAndRenderer(new ColoredXYDataset(
            new IonTimeSeriesToXYProvider(corrected, feature.toString() + " corrected",
                feature.getRawDataFile().getColor())), new ColoredAreaShapeRenderer()),
        new DatasetAndRenderer(new ColoredXYDataset(
            new IonTimeSeriesToXYProvider(full, feature.toString(),
                feature.getRawDataFile().getColor())), new ColoredXYLineRenderer())));

    additionalPreviewData.forEach(a -> data.add(
        new DatasetAndRenderer(new ColoredXYDataset(a), new ColoredXYLineRenderer())));

    return data;
  }
}
