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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingTask.SmoothingDimension;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.javafx.SortableFeatureComboBox;
import java.text.NumberFormat;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

public class SmoothingSetupDialog extends ParameterSetupDialogWithPreview {

  protected final UnitFormat uf;
  protected final NumberFormat rtFormat;
  protected final NumberFormat intensityFormat;
  private final SimpleXYChart<IonTimeSeriesToXYProvider> previewChart;
  private final ColoredAreaShapeRenderer smoothedRenderer;
  protected ComboBox<FeatureList> flistBox;
  protected SortableFeatureComboBox fBox;
  protected ColoredAreaShapeRenderer shapeRenderer = new ColoredAreaShapeRenderer();
  protected SmoothingDimension previewDimension;

  public SmoothingSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    uf = MZmineCore.getConfiguration().getUnitFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    previewChart = new SimpleXYChart<>("Preview");
    previewChart.setRangeAxisLabel("Intensity");
    previewChart.setDomainAxisLabel(uf.format("Retention time", "min"));
    previewChart.setRangeAxisNumberFormatOverride(intensityFormat);
    previewChart.setMinHeight(400);
    smoothedRenderer = new ColoredAreaShapeRenderer();

    previewDimension = SmoothingDimension.RETENTION_TIME;
    previewChart.setDomainAxisNumberFormatOverride(rtFormat);
    previewChart.setRangeAxisNumberFormatOverride(intensityFormat);
    ObservableList<FeatureList> flists = FXCollections.observableArrayList(
        ProjectService.getProjectManager().getCurrentProject().getCurrentFeatureLists());

    fBox = new SortableFeatureComboBox();
    flistBox = new ComboBox<>(flists);
    flistBox.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> {
          if (newValue != null) {
            fBox.setItems(FXCollections.observableArrayList(
                newValue.getFeatures(newValue.getRawDataFile(0))));
          } else {
            fBox.setItems(FXCollections.emptyObservableList());
          }
        }));

    fBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(Feature object) {
        if (object == null) {
          return null;
        }
        return FeatureUtils.featureToString(object);
      }

      @Override
      public Feature fromString(String string) {
        return null;
      }
    });

    fBox.selectedFeatureProperty()
        .addListener(((_, _, newValue) -> onSelectedFeatureChanged(newValue)));

    ComboBox<SmoothingDimension> previewDimensionBox = new ComboBox<>(
        FXCollections.observableArrayList(SmoothingDimension.values()));
    previewDimensionBox.setValue(previewDimension);
    previewDimensionBox.valueProperty().addListener((obs, old, newval) -> {
      this.previewDimension = newval;
      if (previewDimension == SmoothingDimension.RETENTION_TIME) {
        previewChart.setDomainAxisLabel(uf.format("Retention time", "min"));
      } else {
        previewChart.setDomainAxisLabel("Mobility");
      }
      parametersChanged();
    });

    GridPane pnControls = new GridPane();
    pnControls.add(new Label("Feature list"), 0, 0);
    pnControls.add(flistBox, 1, 0);
    pnControls.add(new Label("Feature"), 0, 1);
    pnControls.add(fBox, 1, 1);
    pnControls.add(new Label("Preview dimension"), 0, 2);
    pnControls.add(previewDimensionBox, 1, 2);
    previewWrapperPane.setBottom(pnControls);
    previewWrapperPane.setCenter(previewChart);
    shapeRenderer.setDefaultItemLabelPaint(
        MZmineCore.getConfiguration().getDefaultChartTheme().getItemLabelPaint());

  }

  private void onSelectedFeatureChanged(final Feature f) {
    previewChart.removeAllDatasets();
    if (f == null) {
      return;
    }

    IonTimeSeries<? extends Scan> featureSeries = f.getFeatureData();

    if (previewDimension == SmoothingDimension.RETENTION_TIME) {
      previewChart.addDataset(new ColoredXYDataset(
          new IonTimeSeriesToXYProvider(f.getFeatureData(), FeatureUtils.featureToString(f),
              f.getRawDataFile().colorProperty())));
    } else {
      if (featureSeries instanceof IonMobilogramTimeSeries) {
        previewChart.addDataset(new ColoredXYDataset(new SummedMobilogramXYProvider(f)));
      }
    }

    final Color previewColor = MZmineCore.getConfiguration().getDefaultColorPalette()
        .getPositiveColor();

    // in case we smooth rt, we remap the rt dimension to all scans, as we would do usually.
    featureSeries =
        previewDimension == SmoothingDimension.RETENTION_TIME ? IonTimeSeriesUtils.remapRtAxis(
            featureSeries, flistBox.getValue().getSeletedScans(f.getRawDataFile())) : featureSeries;

    final SmoothingAlgorithm smoothing = FeatureSmoothingOptions.createSmoother(parameterSet);
    final IonTimeSeries<? extends Scan> smoothed = smoothing.smoothFeature(null, featureSeries, f,
        ZeroHandlingType.KEEP);

    if (previewDimension == SmoothingDimension.RETENTION_TIME) {
      previewChart.addDataset(new ColoredXYDataset(
          new IonTimeSeriesToXYProvider(smoothed, "smoothed",
              new SimpleObjectProperty<>(previewColor))), smoothedRenderer);
    } else {
      if (smoothed instanceof IonMobilogramTimeSeries) {
        previewChart.addDataset(new ColoredXYDataset(new SummedMobilogramXYProvider(
            ((IonMobilogramTimeSeries) smoothed).getSummedMobilogram(),
            new SimpleObjectProperty<>(previewColor), "smoothed")), smoothedRenderer);
      }
    }
  }

  @Override
  protected void parametersChanged() {
    super.parametersChanged();
    updateParameterSetFromComponents();
    onSelectedFeatureChanged(fBox.getSelectedFeature());
  }

}
