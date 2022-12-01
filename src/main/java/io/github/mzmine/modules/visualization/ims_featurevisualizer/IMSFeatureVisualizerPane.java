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

package io.github.mzmine.modules.visualization.ims_featurevisualizer;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilogramAccessType;
import io.github.mzmine.datamodel.data_access.MobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.IntervalXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.SpectraMerging;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.Nullable;

/**
 * Larger view of feature shape, mobilogram and ims trace. includes m/z distribution chart.
 *
 * @author https://github.com/SteffenHeu
 */
public class IMSFeatureVisualizerPane extends SplitPane {

  private static Logger logger = Logger.getLogger(IMSFeatureVisualizerPane.class.getName());
  private final IMSTraceVisualizerPane traceVisualizer;
  private final SimpleXYChart<IntervalXYProvider> mzDistributionChart;
  private final NumberFormat mzFormat;
  private final NumberFormat intensityFormat;
  private final UnitFormat unitFormat;
  private final DoubleComponent mzBinComponent;
  private final ObjectProperty<ModularFeature> featureProperty = new SimpleObjectProperty<>();
  private RangeValueType binType = RangeValueType.ABUNDANCE;

  public IMSFeatureVisualizerPane() {
    setOrientation(Orientation.HORIZONTAL);

    traceVisualizer = new IMSTraceVisualizerPane();
    mzDistributionChart = new SimpleXYChart<>("m/z distribution");

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    mzDistributionChart.setDomainAxisLabel("m/z");
    mzDistributionChart.setDomainAxisNumberFormatOverride(mzFormat);

    featureProperty.addListener(((observable, oldValue, newValue) -> onFeatureChanged(newValue)));

    final VBox pnBinnignControl = new VBox(5d);
    final HBox pnRangeType = new HBox(5d);
    pnBinnignControl.getChildren().add(pnRangeType);
    pnRangeType.getChildren().add(new Label("Range value: "));
    ComboBox<RangeValueType> valueTypeBox = new ComboBox<>(
        FXCollections.observableArrayList(RangeValueType.values()));
    valueTypeBox.valueProperty().addListener(((observable, oldValue, newValue) -> {
      binType = newValue;
      if (binType == RangeValueType.INTENSITY) {
        mzDistributionChart.setRangeAxisLabel(unitFormat.format("Intensity", "a.u."));
        mzDistributionChart.setRangeAxisNumberFormatOverride(intensityFormat);
      } else {
        mzDistributionChart.setRangeAxisLabel("Abundance");
        mzDistributionChart.setRangeAxisNumberFormatOverride(null);
      }
      updateMzBin();
    }));
    valueTypeBox.setValue(RangeValueType.ABUNDANCE);
    pnRangeType.getChildren().add(valueTypeBox);
    final HBox pnBinWidth = new HBox(5d);
    pnBinWidth.getChildren().add(new Label(unitFormat.format("Bin width", "m/z")));

    mzBinComponent = new DoubleComponent(100, 0d, 1d, mzFormat, 0.005);
    mzBinComponent.getTextField().textProperty()
        .addListener(((observable, oldValue, newValue) -> updateMzBin()));
    pnBinWidth.getChildren().add(mzBinComponent);
    pnBinnignControl.getChildren().add(pnBinWidth);

    final BorderPane wrap = new BorderPane(mzDistributionChart);
    wrap.setBottom(pnBinnignControl);
    BorderPane.setAlignment(pnBinnignControl, Pos.CENTER);

    mzDistributionChart.setMinSize(300, 300);
    mzDistributionChart.setDefaultRenderer(new ColoredXYBarRenderer(false));

    setDividerPosition(0, 0.6);
    getItems().add(traceVisualizer);
    getItems().add(wrap);
  }

  private void onFeatureChanged(@Nullable final ModularFeature newFeature) {
    if (newFeature == null || newFeature.getFeatureData() == null) {
      traceVisualizer.setFeature(null);
      return;
    }

    traceVisualizer.setFeature(newFeature);
    mzBinComponent.getTextField()
        .setText(
            String.valueOf(RangeUtils.rangeLength(getFeature().getRawDataPointsMZRange()) / 10));
  }

  private void updateMzBin() {
    if (getFeature() == null) {
      return;
    }

    final IonMobilogramTimeSeries series = (IonMobilogramTimeSeries) getFeature().getFeatureData();
    List<DataPoint> dataPoints = new ArrayList<>();

    double binWidth = 0d;
    try {
      binWidth = Double.parseDouble(mzBinComponent.getText());
    } catch (NumberFormatException e) {
      logger.fine(() -> "Invalid number: " + mzBinComponent.getText());
      return;
    }
    if(binWidth == 0d) {
      binWidth = 0.00001;
    }

    final MobilogramDataAccess access = EfficientDataAccess
        .of(series, MobilogramAccessType.ONLY_DETECTED);
    while (access.hasNext()) {
      access.next();
      for (int i = 0; i < access.getNumberOfValues(); i++) {
        dataPoints.add(new SimpleDataPoint(access.getMZ(i), access.getIntensity(i)));
      }
    }

    final DataPointSorter sorter = new DataPointSorter(SortingProperty.MZ,
        SortingDirection.Ascending);
    dataPoints.sort(sorter);

    final RangeMap<Double, AtomicDouble> binnedValues = TreeRangeMap.create();
    final Range<Double> firstRange = Range
        .open(dataPoints.get(0).getMZ(), dataPoints.get(0).getMZ() + binWidth);
    double lastMaxBin = firstRange.upperEndpoint();

    binnedValues.put(firstRange, new AtomicDouble(0d));
    for (DataPoint dp : dataPoints) {
      var summedIntensity = binnedValues.get(dp.getMZ());
      if (summedIntensity == null) {
        if(lastMaxBin + binWidth <= dp.getMZ()) {
          lastMaxBin = dp.getMZ();
        }
        Range<Double> newRange = SpectraMerging.createNewNonOverlappingRange(binnedValues,
            Range.openClosed(lastMaxBin, lastMaxBin + binWidth));
        summedIntensity = new AtomicDouble(0d);
        binnedValues.put(newRange, summedIntensity);
        lastMaxBin = newRange.upperEndpoint();
      }
      if (binType == RangeValueType.ABUNDANCE) {
        summedIntensity.getAndAdd(1d);
      } else {
        summedIntensity.getAndAdd(dp.getIntensity());
      }
    }

    Map<Range<Double>, AtomicDouble> binnedMap = binnedValues.asMapOfRanges();
    double[] mzs = new double[binnedMap.size()];
    double[] intensities = new double[binnedMap.size()];

    int index = 0;
    for (Entry<Range<Double>, AtomicDouble> entry : binnedMap.entrySet()) {
      mzs[index] = RangeUtils.rangeCenter(entry.getKey());
      intensities[index] = entry.getValue().get();
      index++;
    }

    IntervalXYProvider dataset = new IntervalXYProvider(mzs, intensities, binWidth,
        getFeature().getRawDataFile().getColorAWT(), "m/z distribution");

    mzDistributionChart.removeAllDatasets();
    mzDistributionChart.addDataset(dataset);
  }

  public ModularFeature getFeature() {
    return featureProperty.get();
  }

  public void setFeature(ModularFeature feature) {
    this.featureProperty.set(feature);
  }

  public ObjectProperty<ModularFeature> featureProperty() {
    return featureProperty;
  }

  private enum RangeValueType {
    ABUNDANCE, INTENSITY
  }
}
