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

package io.github.mzmine.modules.dataprocessing.otherdata.featdet_bin;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.OtherFeatureUtils;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.OtherFeatureDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.previewpane.AbstractPreviewPane;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherRawOrProcessed;
import io.github.mzmine.util.javafx.OtherFeatureSelectionPane;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BinTracesPreviewPane extends AbstractPreviewPane<OtherFeature> {

  private final OtherFeatureSelectionPane selectionPane;

  private volatile RawDataFile previousFile = null;
  private volatile ColoredXYDataset previousFileDataset = null;

  public BinTracesPreviewPane(ParameterSet parameters) {
    super(parameters);

    selectionPane = new OtherFeatureSelectionPane(OtherRawOrProcessed.PREPROCESSED,
        OtherRawOrProcessed.RAW);
    selectionPane.featureProperty().addListener((_, _, _) -> updatePreview());
    setBottom(selectionPane);
  }

  @Override
  public @NotNull SimpleXYChart<PlotXYDataProvider> createChart() {
    final SimpleXYChart<PlotXYDataProvider> chart = new SimpleXYChart<>();
    chart.setDomainAxisLabel(formats.unit("RT", "min"));
    chart.setDomainAxisNumberFormatOverride(formats.rtFormat());
    chart.setRangeAxisLabel("Intensity");
    return chart;
  }

  @Override
  public void updateChart(@NotNull List<DatasetAndRenderer> datasets,
      @NotNull SimpleXYChart<? extends PlotXYDataProvider> chart) {
    chart.applyWithNotifyChanges(false, () -> {
      chart.removeAllDatasets();
      datasets.forEach(ds -> chart.addDataset(ds.dataset(), ds.renderer()));
    });
  }

  @Override
  public @NotNull List<@NotNull DatasetAndRenderer> calculateNewDatasets(
      @Nullable OtherFeature valueForPreview) {
    if (valueForPreview == null) {
      return List.of();
    }

    List<DatasetAndRenderer> newDatasets = new ArrayList<>();

    final int binWidth = Objects.requireNonNullElse(
        parameters.getValue(BinTracesParameters.binWidth), 0d).intValue();

    final OtherFeature binned = OtherFeatureUtils.bin(null, valueForPreview, binWidth);
    final ColoredXYDataset ds = new ColoredXYDataset(new OtherFeatureDataProvider(binned,
        ConfigService.getDefaultColorPalette().getNegativeColorAWT()), RunOption.THIS_THREAD);
    newDatasets.add(new DatasetAndRenderer(ds, new ColoredXYLineRenderer()));
    return newDatasets;
  }

  @Override
  public OtherFeature getValueForPreview() {
    return selectionPane.featureProperty().get();
  }

}
