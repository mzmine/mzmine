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

package io.github.mzmine.modules.visualization.scan_histogram.chart;

import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.modules.visualization.scan_histogram.ScanHistogramParameters;
import io.github.mzmine.modules.visualization.scan_histogram.ScanHistogramType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.massdefect.MassDefectFilter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.scene.layout.BorderPane;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.XYPlot;

/**
 * Enhanced version. Use arrows to jump to the next or previous distribution
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ScanHistogramTab extends MZmineTab {

  //private final Scene mainScene;
  private final BorderPane mainPane;
  // parameters
  private final ScanSelection scanSelection;
  private final Range<Double> mzRange;
  private final boolean useMobilityScans;
  private final double binWidth;
  private final ScanHistogramType valueType;
  private final Range<Double> intensityRange;
  private final Boolean useIntensityRange;
  private final MassDefectFilter massDefectFilter;
  private final Boolean useMassDefect;
  protected HistogramPanel histo;
  private RawDataFile[] dataFiles;
  // scan counter
  private int processedScans, totalScans;
  private HistogramData data;

  /**
   * @param dataFile   rawDataFile
   * @param title      title
   * @param parameters parameters
   */
  public ScanHistogramTab(String title, ParameterSet parameters, RawDataFile... dataFile) {
    super(title, true, false);
    //setTitle(title);

    this.dataFiles = dataFile;
    scanSelection = parameters.getValue(ScanHistogramParameters.scanSelection);
    mzRange = parameters.getValue(ScanHistogramParameters.mzRange);
    binWidth = parameters.getValue(ScanHistogramParameters.binWidth);
    useMobilityScans = parameters.getValue(ScanHistogramParameters.useMobilityScans);
    useIntensityRange = parameters.getValue(ScanHistogramParameters.heightRange);
    intensityRange = parameters.getParameter(ScanHistogramParameters.heightRange)
        .getEmbeddedParameter().getValue();
    useMassDefect = parameters.getValue(ScanHistogramParameters.massDefect);
    massDefectFilter = parameters.getParameter(ScanHistogramParameters.massDefect)
        .getEmbeddedParameter().getValue();
    valueType = parameters.getValue(ScanHistogramParameters.type);

    data = buildHistogramData(dataFile);

    mainPane = new BorderPane();
    //mainScene = new Scene(mainPane);

    // Use main CSS
    //mainScene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

    histo = new HistogramPanel(valueType.toString(), data, binWidth);

    //setMinWidth(1050);
    //setMinHeight(700);
    //setScene(mainScene);
    setContent(mainPane);

    mainPane.setCenter(histo);

    // Add the Windows menu
    //WindowsMenu.addWindowsMenu(mainScene);
    //addKeyBindings();
  }

  private HistogramData buildHistogramData(RawDataFile... dataFiles) {
    logger.info("Starting to build mz distribution histogram for " + Arrays.stream(dataFiles)
        .map(Object::toString).collect(Collectors.joining(",")));

    // histo data
    DoubleArrayList data = new DoubleArrayList();

    for (RawDataFile dataFile : dataFiles) {
      if (dataFile instanceof IMSRawDataFile ims && useMobilityScans) {
        MobilityScanDataAccess scanAccess = EfficientDataAccess.of(ims,
            MobilityScanDataType.CENTROID, scanSelection);
        totalScans = scanAccess.getNumberOfScans();
        while (scanAccess.nextFrame() != null) {
          while (scanAccess.nextMobilityScan() != null) {
            addAllDataPoints(scanAccess, data);
          }
        }
      } else {
        ScanDataAccess scanAccess = EfficientDataAccess.of(dataFile, ScanDataType.CENTROID,
            scanSelection);
        totalScans = scanAccess.getNumberOfScans();
        while (scanAccess.nextScan() != null) {
          addAllDataPoints(scanAccess, data);
          processedScans++;
        }
      }
    }

    if (!data.isEmpty()) {
      // to array
      return new HistogramData(data.toDoubleArray());
    } else {
      throw new MSDKRuntimeException("Data was empty. Review your selected filters.");
    }
  }

  private void addAllDataPoints(Scan scan, DoubleArrayList data) {
    // is already a ScanDataAccess or mobilityScanAccess
    int n = scan.getNumberOfDataPoints();
    for (int i = 0; i < n; i++) {
      double mz = scan.getMzValue(i);
      double intensity = scan.getIntensityValue(i);
      if (mzRange.contains(mz) && (!useIntensityRange || intensityRange.contains(intensity)) && (
          !useMassDefect || massDefectFilter.contains(mz))) {
        double val = switch (valueType) {
          case MZ -> mz;
          case INTENSITY -> intensity;
          case MASS_DEFECT -> mz - Math.floor(mz);
          case INTENSITY_RECAL ->
              intensity * Objects.requireNonNullElse(scan.getInjectionTime(), 1f);
        };
        data.add(val);
      }
    }
  }

  private ChartViewer getChartPanel() {
    return getHistoPanel().getChartPanel();
  }

  private XYPlot getXYPlot() {
    ChartViewer chart = getHistoPanel().getChartPanel();
    if (chart != null) {
      return chart.getChart().getXYPlot();
    } else {
      return null;
    }
  }

  public HistogramPanel getHistoPanel() {
    return histo;
  }

  public int getTotalScans() {
    return totalScans;
  }

  public int getProcessedScans() {
    return processedScans;
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return List.of(dataFiles);
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    if (rawDataFiles == null || rawDataFiles.isEmpty() || CollectionUtils.isEqualCollection(
        rawDataFiles, getRawDataFiles())) {
      return;
    }

    RawDataFile[] newFiles = rawDataFiles.toArray(RawDataFile[]::new);
    HistogramData newData = buildHistogramData(newFiles);
    histo.setData(newData, binWidth);
    dataFiles = newFiles;

    data = newData;
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }
}
