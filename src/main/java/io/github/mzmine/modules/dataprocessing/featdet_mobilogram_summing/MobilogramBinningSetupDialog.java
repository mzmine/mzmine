/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.javafx.SortableFeatureComboBox;
import java.text.NumberFormat;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

/**
 * @author Steffen https://github.com/SteffenHeu
 */
public class MobilogramBinningSetupDialog extends ParameterSetupDialogWithPreview {

  protected final Label lbkApproxBinSize = new Label();
  private final SimpleXYChart<SummedMobilogramXYProvider> previewChart;
  private final ColoredXYShapeRenderer processedRenderer;
  private final NumberFormat intensityFormat;
  private final NumberFormat mobilityFormat;
  protected ComboBox<FeatureList> flistBox;
  protected SortableFeatureComboBox fBox;
  protected ColoredXYShapeRenderer shapeRenderer = new ColoredXYShapeRenderer();
  protected BinningMobilogramDataAccess summedMobilogramAccess;

  public MobilogramBinningSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();

    previewChart = new SimpleXYChart<>("Preview");
    previewChart.setRangeAxisLabel("Intensity");
    previewChart.setDomainAxisLabel("Mobility");
    previewChart.setRangeAxisNumberFormatOverride(intensityFormat);
    previewChart.setDomainAxisNumberFormatOverride(
        MZmineCore.getConfiguration().getMobilityFormat());
    previewChart.setMinHeight(400);
    processedRenderer = new ColoredXYShapeRenderer();

    previewChart.setRangeAxisNumberFormatOverride(intensityFormat);
    ObservableList<FeatureList> flists = FXCollections.observableArrayList(
        MZmineCore.getProjectManager().getCurrentProject().getCurrentFeatureLists());

    fBox = new SortableFeatureComboBox();
    flistBox = new ComboBox<>(flists);
    flistBox.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> {
          if (newValue != null) {
            fBox.getFeatureBox().setItems(FXCollections.observableArrayList(
                newValue.getFeatures(newValue.getRawDataFile(0))));
          } else {
            fBox.getFeatureBox().setItems(FXCollections.emptyObservableList());
          }
        }));

    fBox.getFeatureBox().setConverter(new StringConverter<>() {
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

    fBox.getFeatureBox().getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> onSelectedFeatureChanged(newValue)));

    GridPane pnControls = new GridPane();
    pnControls.setHgap(5);
    pnControls.setVgap(5);
    pnControls.setPadding(new Insets(5));
    pnControls.add(new Label("Feature list"), 0, 0);
    pnControls.add(flistBox, 1, 0);
    pnControls.add(new Label("Feature"), 0, 1);
    pnControls.add(fBox, 1, 1);
    pnControls.add(new Label("Approximate bin size"), 0, 2);
    pnControls.add(lbkApproxBinSize, 1, 2);
    previewWrapperPane.setBottom(pnControls);
    previewWrapperPane.setCenter(previewChart);
    shapeRenderer.setDefaultItemLabelPaint(
        MZmineCore.getConfiguration().getDefaultChartTheme().getItemLabelPaint());
  }

  private void onSelectedFeatureChanged(final Feature f) {
    previewChart.removeAllDatasets();

    if (f == null || !(f.getRawDataFile() instanceof IMSRawDataFile)
        || !(f.getFeatureData() instanceof IonMobilogramTimeSeries series)) {
      return;
    }

    previewChart.setDomainAxisLabel(f.getMobilityUnit().getAxisLabel());
    previewChart.addDataset(new ColoredXYDataset(
        new SummedMobilogramXYProvider(series.getSummedMobilogram(),
            new SimpleObjectProperty<>(f.getRawDataFile().getColor()),
            FeatureUtils.featureToString(f))));

    final Integer binWidth = switch (((IMSRawDataFile) f.getRawDataFile()).getMobilityType()) {
      case TIMS -> parameterSet.getParameter(MobilogramBinningParameters.timsBinningWidth)
          .getValue();
      case TRAVELING_WAVE -> parameterSet.getParameter(
          MobilogramBinningParameters.twimsBinningWidth).getValue();
      case DRIFT_TUBE -> parameterSet.getParameter(MobilogramBinningParameters.dtimsBinningWidth)
          .getValue();
      default -> throw new UnsupportedOperationException(
          "Summing of the mobility type in raw data file " + f.getRawDataFile().getName()
          + " is unsupported.");
    };
    if (binWidth == null || binWidth == 0) {
      return;
    }

    final SimpleColorPalette colorPalette = MZmineCore.getConfiguration().getDefaultColorPalette();

    if (summedMobilogramAccess == null || f.getRawDataFile() != summedMobilogramAccess.getDataFile()
        || Double.compare(binWidth, summedMobilogramAccess.getBinWidth()) != 0) {
      summedMobilogramAccess = new BinningMobilogramDataAccess((IMSRawDataFile) f.getRawDataFile(),
          binWidth);
      lbkApproxBinSize.setText(
          mobilityFormat.format(summedMobilogramAccess.getApproximateBinSize()) + " "
          + summedMobilogramAccess.getDataFile().getMobilityType().getUnit());
    }

    summedMobilogramAccess.setMobilogram(series.getSummedMobilogram());
    SummedIntensityMobilitySeries fromSummed = summedMobilogramAccess.toSummedMobilogram(null);

    previewChart.addDataset(new ColoredXYDataset(new SummedMobilogramXYProvider(fromSummed,
            new SimpleObjectProperty<>(colorPalette.getPositiveColor()), "From preprocessed")),
        processedRenderer);

    summedMobilogramAccess.setMobilogram(series.getMobilograms());
    SummedIntensityMobilitySeries fromMobilograms = summedMobilogramAccess.toSummedMobilogram(null);

    previewChart.addDataset(new ColoredXYDataset(new SummedMobilogramXYProvider(fromMobilograms,
        new SimpleObjectProperty<>(colorPalette.getPositiveColor()), "From raw")));
  }

  @Override
  protected void parametersChanged() {
    super.parametersChanged();
    updateParameterSetFromComponents();
    onSelectedFeatureChanged(fBox.getFeatureBox().getValue());
  }
}
