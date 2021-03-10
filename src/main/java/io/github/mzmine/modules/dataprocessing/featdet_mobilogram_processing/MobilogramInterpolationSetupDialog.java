/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_mobilogram_processing;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.data_access.SummedMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.FastColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.BasicStroke;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.jfree.chart.plot.IntervalMarker;

public class MobilogramInterpolationSetupDialog extends ParameterSetupDialogWithPreview {

  private final SimpleXYChart<SummedMobilogramXYProvider> previewChart;
  private final ColoredXYShapeRenderer processedRenderer;

  private final NumberFormat rtFormat;
  private final NumberFormat intensityFormat;

  protected ComboBox<ModularFeatureList> flistBox;
  protected ComboBox<ModularFeature> fBox;
  protected ColoredXYShapeRenderer shapeRenderer = new ColoredXYShapeRenderer();

  protected SummedMobilogramDataAccess dataAccess;

  public MobilogramInterpolationSetupDialog(boolean valueCheckRequired,
      ParameterSet parameters) {
    super(valueCheckRequired, parameters,
        "EXPERIMENTAL USE! READ THE HELP FILE BEFORE USAGE! DO NOT USE FOR NON-TARGET ANALYSES!");

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    previewChart = new SimpleXYChart<>("Preview");
    previewChart.setRangeAxisLabel("Intensity");
    previewChart.setDomainAxisLabel("Mobility");
    previewChart.setRangeAxisNumberFormatOverride(intensityFormat);
    previewChart.setMinHeight(400);
    processedRenderer = new ColoredXYShapeRenderer();

    previewChart.setDomainAxisNumberFormatOverride(rtFormat);
    previewChart.setRangeAxisNumberFormatOverride(intensityFormat);
    ObservableList<ModularFeatureList> flists = (ObservableList<ModularFeatureList>)
        (ObservableList<? extends FeatureList>) MZmineCore.getProjectManager().getCurrentProject()
            .getFeatureLists();

    fBox = new ComboBox<>();
    flistBox = new ComboBox<>(flists);
    flistBox.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> {
          if (newValue != null) {
            fBox.setItems(
                (ObservableList<ModularFeature>) (ObservableList<? extends Feature>) newValue
                    .getFeatures(newValue.getRawDataFile(0)));
          } else {
            fBox.setItems(FXCollections.emptyObservableList());
          }
        }));

    fBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(ModularFeature object) {
        if (object == null) {
          return null;
        }
        return FeatureUtils.featureToString(object);
      }

      @Override
      public ModularFeature fromString(String string) {
        return null;
      }
    });

    fBox.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> onSelectedFeatureChanged(newValue)));

    GridPane pnControls = new GridPane();
    pnControls.add(new Label("Feature list"), 0, 0);
    pnControls.add(flistBox, 1, 0);
    pnControls.add(new Label("Feature"), 0, 1);
    pnControls.add(fBox, 1, 1);
    previewWrapperPane.setBottom(pnControls);
    previewWrapperPane.setCenter(previewChart);
    shapeRenderer.setDefaultItemLabelPaint(
        MZmineCore.getConfiguration().getDefaultChartTheme().getItemLabelPaint());
  }

  private void onSelectedFeatureChanged(final ModularFeature f) {
    previewChart.removeAllDatasets();
    previewChart.getXYPlot().clearDomainMarkers();

    if (f == null || !(f.getRawDataFile() instanceof IMSRawDataFile) || !(f
        .getFeatureData() instanceof IonMobilogramTimeSeries series)) {
      return;
    }
    if (dataAccess == null || f.getRawDataFile() != dataAccess.getDataFile()) {
      dataAccess = new SummedMobilogramDataAccess((IMSRawDataFile) f.getRawDataFile());
    }

    previewChart.setDomainAxisLabel(f.getMobilityUnit().getAxisLabel());

    previewChart.addDataset(new FastColoredXYDataset(
        new SummedMobilogramXYProvider(series.getSummedMobilogram(),
            new SimpleObjectProperty<>(f.getRawDataFile().getColor()),
            FeatureUtils.featureToString(f))));

    if (!parameterSet.checkParameterValues(new ArrayList<>())) {
      return;
    }

    final int windowWidth = parameterSet.getParameter(MobilogramInterpolationParameters.windowWidth)
        .getValue();
    final int minNumIntensities = parameterSet
        .getParameter(MobilogramInterpolationParameters.numIntensities)
        .getValue();
    final int filterWidth = parameterSet.getParameter(MobilogramInterpolationParameters.filterWidth)
        .getValue();

    dataAccess.setSummedMobilogram(series.getSummedMobilogram());

    if (!MobilogramInterpolationTask.isEligible(dataAccess, windowWidth, minNumIntensities)) {
      return;
    }

    final List<Range<Integer>> ranges = MobilogramInterpolationTask
        .getEligibleRanges(dataAccess, windowWidth, minNumIntensities);
    final double[][] resultArray = MobilogramInterpolationTask
        .process(dataAccess, ranges, filterWidth);
    final SummedIntensityMobilitySeries result = new SummedIntensityMobilitySeries(null,
        resultArray[0], resultArray[1], f.getMZ());

    final SimpleColorPalette colorPalette = MZmineCore.getConfiguration().getDefaultColorPalette();

    for (Range<Integer> range : ranges) {
      previewChart.getXYPlot().addDomainMarker(
          new IntervalMarker(dataAccess.getMobility(range.lowerEndpoint()),
              dataAccess.getMobility(range.upperEndpoint()), colorPalette.getPositiveColorAWT(),
              new BasicStroke(1f), colorPalette.getNeutralColorAWT(), new BasicStroke(1f), 0.2f));
    }

    previewChart.addDataset(new FastColoredXYDataset(
        new SummedMobilogramXYProvider(result,
            new SimpleObjectProperty<>(colorPalette.getPositiveColor()),
            FeatureUtils.featureToString(f))), processedRenderer);
  }

  @Override
  protected void parametersChanged() {
    super.parametersChanged();
    updateParameterSetFromComponents();
    onSelectedFeatureChanged(fBox.getValue());
  }
}
