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

package io.github.mzmine.modules.dataprocessing.otherdata.featdet_baseline;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureImpl;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.OtherFeatureDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractBaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectors;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.previewpane.AbstractPreviewPane;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.javafx.OtherFeatureSelectionPane;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class OtherDataBaselineCorrectionPreview extends AbstractPreviewPane<OtherFeature> {

  private final OtherFeatureSelectionPane selectionPane = new OtherFeatureSelectionPane();

  public OtherDataBaselineCorrectionPreview(ParameterSet parameterSet) {
    super(parameterSet);

    setBottom(selectionPane);
    selectionPane.featureProperty().addListener((_,_,v) -> updatePreview());
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
      @Nullable OtherFeature feature) {

    final List<String> errorMessages = new ArrayList<>();
    if (feature == null || !parameters.checkParameterValues(errorMessages, true)) {
      if (!errorMessages.isEmpty()) {
        FxThread.runLater(() -> DialogLoggerUtil.showMessageDialog("Invalid parameter settings",
            "Please double check parameters: " + errorMessages.stream()
                .collect(Collectors.joining(", "))));
      }
      return List.of();
    }

    final ModularFeatureList dummyFlist = new ModularFeatureList("dummy", null,
        new RawDataFileImpl("dummy", null, null));

    final BaselineCorrectors enumValue = parameters.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getValue();
    final BaselineCorrector baselineCorrector = enumValue.getModuleInstance()
        .newInstance(parameters, null, dummyFlist);
    if (baselineCorrector instanceof AbstractBaselineCorrector uv) {
      uv.setPreview(true);
    }

    OtherFeature corrected = new OtherFeatureImpl(
        baselineCorrector.correctBaseline(feature.getFeatureData()));
    final List<PlotXYDataProvider> additionalPreviewData = baselineCorrector.getAdditionalPreviewData();

    final List<DatasetAndRenderer> data = new ArrayList<>();

    final Color color = feature.getFeatureData().getOtherDataFile().getCorrespondingRawDataFile()
        .getColorAWT();
    data.addAll(List.of(new DatasetAndRenderer(new ColoredXYDataset(
            new OtherFeatureDataProvider(corrected, feature.toString() + " corrected",
                ConfigService.getDefaultColorPalette().getPositiveColorAWT())),
            new ColoredAreaShapeRenderer()), //
        new DatasetAndRenderer(
            new ColoredXYDataset(new OtherFeatureDataProvider(feature, feature.toString(), color)),
            new ColoredXYLineRenderer())));

    additionalPreviewData.forEach(a -> data.add(
        new DatasetAndRenderer(new ColoredXYDataset(a), new ColoredXYLineRenderer())));

    return data;
  }

  @Override
  public OtherFeature getValueForPreview() {
    return selectionPane.getFeature();
  }
}
