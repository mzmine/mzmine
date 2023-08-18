/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.raw_data_summary;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.msms.MsMsVisualizerTab;
import io.github.mzmine.modules.visualization.scan_histogram.ScanHistogramParameters;
import io.github.mzmine.modules.visualization.scan_histogram.ScanHistogramType;
import io.github.mzmine.modules.visualization.scan_histogram.chart.ScanHistogramTab;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.massdefect.MassDefectFilter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class RawDataSummaryPane extends BorderPane {

  private final List<ScanHistogramTab> tabs = new ArrayList<>(3);
  private final SplitPane noiseSplit;
  private final BorderPane noiseMainPane;
  private final BorderPane noiseHistoPane;
  private final BorderPane noiseHistoDenormalizedPane;
  private final MsMsVisualizerTab msmsScatterPlot;


  public RawDataSummaryPane(final RawDataFile[] dataFiles, final ParameterSet parameters) {
    super();

    final var scanDataType = parameters.getValue(RawDataSummaryParameters.scanDataType)
        .mapToScanType();
    final var scanSelection = parameters.getValue(RawDataSummaryParameters.scanSelection);

    final var useMzRange = parameters.getValue(RawDataSummaryParameters.mzRange);
    final var mzRange = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        RawDataSummaryParameters.mzRange, Range.all());
    final var useHeightRange = parameters.getValue(RawDataSummaryParameters.heightRange);
    final var heightRange = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        RawDataSummaryParameters.heightRange, Range.all());
    final var massDefectFilter = new MassDefectFilter(0.6, 0.8);
    final var upperMzCutoffForMassDefect = 200d;

    final var formats = MZmineCore.getConfiguration().getGuiFormats();

    var intensityLbl = heightRange.hasLowerBound() ? ": intensity≥" + formats.intensity(
        heightRange.lowerEndpoint()) : "";
    BorderPane mzHistoPane = createScanHistoParameters("m/z distribution" + intensityLbl, dataFiles,
        scanDataType, scanSelection, useMzRange, mzRange, useHeightRange, heightRange, false,
        MassDefectFilter.ALL, ScanHistogramType.MZ, 0.005);

    BorderPane massDefectHistoPane = createScanHistoParameters(
        "Mass defect (MD) distribution" + intensityLbl, dataFiles, scanDataType, scanSelection,
        useMzRange, mzRange, true, heightRange, false, MassDefectFilter.ALL,
        ScanHistogramType.MASS_DEFECT, 0.0025);

    BorderPane intensityHistoPane = createScanHistoParameters("Intensity distribution", dataFiles,
        scanDataType, scanSelection, useMzRange, mzRange, false, heightRange, false,
        MassDefectFilter.ALL, ScanHistogramType.INTENSITY, -1);

    // within mass defect
    var noiseMzRange = Range.closed(0d, upperMzCutoffForMassDefect);
    String mzFilterString = " for m/z≤" + (int) upperMzCutoffForMassDefect;

    noiseHistoPane = createScanHistoParameters(
        "Noise distribution: MD within " + massDefectFilter + mzFilterString, dataFiles,
        scanDataType, scanSelection, true, noiseMzRange, false, heightRange, true, massDefectFilter,
        ScanHistogramType.INTENSITY, -1);

    noiseHistoDenormalizedPane = createScanHistoParameters(
        "Noise distribution (x inject time): MD within " + massDefectFilter + mzFilterString,
        dataFiles, scanDataType, scanSelection, true, noiseMzRange, false, heightRange, true,
        massDefectFilter, ScanHistogramType.INTENSITY_RECAL, -1);

    BorderPane massDefectBelow200HistoPane = createScanHistoParameters(
        "Mass defect" + mzFilterString + intensityLbl, dataFiles, scanDataType, scanSelection, true,
        noiseMzRange, true, heightRange, false, MassDefectFilter.ALL, ScanHistogramType.MASS_DEFECT,
        0.0025);

    // only show noise recalibrated histograms for
    noiseSplit = new SplitPane(noiseHistoPane, noiseHistoDenormalizedPane);
    noiseSplit.setOrientation(Orientation.VERTICAL);
    noiseMainPane = new BorderPane();
    if (hasInjectTime(dataFiles)) {
      noiseSplit.getItems().setAll(noiseHistoPane, noiseHistoDenormalizedPane);
      noiseMainPane.setCenter(noiseSplit);
    } else {
      noiseSplit.getItems().clear();
      noiseMainPane.setCenter(noiseHistoPane);
    }

    msmsScatterPlot = new MsMsVisualizerTab(dataFiles);

    var noisePane = new BorderPane(new SplitPane(noiseMainPane, massDefectBelow200HistoPane));
    var msmsScattertab = new Tab("MS/MS scatter plot", msmsScatterPlot.getContent());
    var mztab = new Tab("m/z", new SplitPane(mzHistoPane, massDefectHistoPane));
    var noisetab = new Tab("Noise", noisePane);
    var intensitytab = new Tab("Intensity", intensityHistoPane);
    msmsScattertab.setClosable(false);
    mztab.setClosable(false);
    noisetab.setClosable(false);
    intensitytab.setClosable(false);

    // set everything to main pane
    TabPane tabPane = new TabPane(msmsScattertab, mztab, noisetab, intensitytab);
    setCenter(tabPane);
  }

  /**
   * Checks if the first selected file has inject time
   *
   * @return true if first file has a scan with inject time
   */
  private boolean hasInjectTime(final RawDataFile[] files) {
    if (files == null || files.length == 0) {
      return false;
    }
    return files[0].stream().anyMatch(Scan::hasInjectionTime);
  }


  private BorderPane createScanHistoParameters(final String title, final RawDataFile[] dataFiles,
      final ScanDataType scanDataType, final ScanSelection scanSelection, final Boolean useMzRange,
      final Range<Double> mzRange, final Boolean useHeightRange, final Range<Double> heightRange,
      final boolean useMassDetect, MassDefectFilter massDefectFilter,
      final ScanHistogramType histoType, final double binWidth) {
    final var mzparams = new ScanHistogramParameters().cloneParameterSet();
    mzparams.setParameter(ScanHistogramParameters.dataFiles, new RawDataFilesSelection(dataFiles));
    mzparams.setParameter(ScanHistogramParameters.useMobilityScans, false);
    mzparams.setParameter(ScanHistogramParameters.massDefect, useMassDetect, massDefectFilter);
    mzparams.setParameter(ScanHistogramParameters.scanSelection, scanSelection);
    mzparams.setParameter(ScanHistogramParameters.scanDataType, scanDataType);
    mzparams.setParameter(ScanHistogramParameters.mzRange, useMzRange, mzRange);
    mzparams.setParameter(ScanHistogramParameters.heightRange, useHeightRange, heightRange);
    mzparams.setParameter(ScanHistogramParameters.type, histoType);
    mzparams.setParameter(ScanHistogramParameters.binWidth, binWidth);
    // important to clone the parameters here to not change them by static parameters later
    var tab = new ScanHistogramTab("", mzparams, dataFiles);
    tab.setClosable(false);
    tabs.add(tab);
    var label = new Label(title);
    label.getStyleClass().add("bold-title-label");
    var top = new HBox(label);
    top.setAlignment(Pos.CENTER);
    var pane = new BorderPane(tab.getMainPane());
    pane.setTop(top);
    return pane;
  }

  public void setDataFiles(final Collection<? extends RawDataFile> dataFiles) {
    tabs.forEach(tab -> tab.setDataFiles(dataFiles));

    if (dataFiles == null) {
      return;
    }

    msmsScatterPlot.onRawDataFileSelectionChanged(dataFiles);

    if (hasInjectTime(dataFiles.toArray(RawDataFile[]::new))) {
      noiseSplit.getItems().setAll(noiseHistoPane, noiseHistoDenormalizedPane);
      noiseMainPane.setCenter(noiseSplit);
    } else {
      noiseSplit.getItems().clear();
      noiseMainPane.setCenter(noiseHistoPane);
    }
  }

  public Collection<? extends RawDataFile> getDataFiles() {
    return tabs.stream().findFirst().map(ScanHistogramTab::getRawDataFiles).orElseGet(List::of);
  }
}
