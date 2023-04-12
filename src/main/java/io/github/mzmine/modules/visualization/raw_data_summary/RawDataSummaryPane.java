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

package io.github.mzmine.modules.visualization.raw_data_summary;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
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
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

public class RawDataSummaryPane extends BorderPane {

  private final List<ScanHistogramTab> tabs = new ArrayList<>(3);


  public RawDataSummaryPane(final RawDataFile[] dataFiles, final ParameterSet parameters) {
    super();

    var scanDataType = parameters.getValue(RawDataSummaryParameters.scanDataType);
    var scanSelection = parameters.getValue(RawDataSummaryParameters.scanSelection);

    var useMzRange = parameters.getValue(RawDataSummaryParameters.mzRange);
    var mzRange = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        RawDataSummaryParameters.mzRange, Range.all());
    var useHeightRange = parameters.getValue(RawDataSummaryParameters.heightRange);
    var heightRange = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        RawDataSummaryParameters.heightRange, Range.all());
    var massDefectFilter = new MassDefectFilter(0.25, 0.4);
    var upperMzCutoffForMassDefect = 200d;

    BorderPane mzHistoPane = createScanHistoParameters(dataFiles, scanDataType, scanSelection,
        useMzRange, mzRange, useHeightRange, heightRange, false, MassDefectFilter.ALL,
        ScanHistogramType.MZ, 0.005);
    BorderPane massDefectHistoPane = createScanHistoParameters(dataFiles, scanDataType,
        scanSelection, useMzRange, mzRange, true, heightRange, false, MassDefectFilter.ALL,
        ScanHistogramType.MASS_DEFECT, 0.0025);

    BorderPane intensityHistoPane = createScanHistoParameters(dataFiles, scanDataType,
        scanSelection, useMzRange, mzRange, false, heightRange, false, MassDefectFilter.ALL,
        ScanHistogramType.INTENSITY, 100);

    // within mass defect
    BorderPane intensityInMassDefectMzHistoPane = createScanHistoParameters(dataFiles, scanDataType,
        scanSelection, true, Range.closed(0d, upperMzCutoffForMassDefect), false, heightRange, true,
        massDefectFilter, ScanHistogramType.INTENSITY, 100);
    BorderPane massDefectBelow200HistoPane = createScanHistoParameters(dataFiles, scanDataType,
        scanSelection, true, Range.closed(0d, upperMzCutoffForMassDefect), true, heightRange, false,
        MassDefectFilter.ALL, ScanHistogramType.MASS_DEFECT, 0.0025);

    var noisePane = new BorderPane(
        new SplitPane(intensityInMassDefectMzHistoPane, massDefectBelow200HistoPane));
    noisePane.setTop(new Label(
        "Signal intensity of mostly noise (m/z < 200; mass defect 0.25-0.4) and mass defect m/z < 200"));

    var mztab = new Tab("m/z", new SplitPane(mzHistoPane, massDefectHistoPane));
    var noisetab = new Tab("Noise", noisePane);
    var intensitytab = new Tab("Intensity", intensityHistoPane);

    TabPane tabPane = new TabPane(mztab, noisetab, intensitytab);
    setCenter(tabPane);
  }

  private BorderPane createScanHistoParameters(final RawDataFile[] dataFiles,
      final ScanDataType scanDataType, final ScanSelection scanSelection, final Boolean useMzRange,
      final Range<Double> mzRange, final Boolean useHeightRange, final Range<Double> heightRange,
      final boolean useMassDetect, MassDefectFilter massDefectFilter,
      final ScanHistogramType histoType, final double binWidth) {
    ScanHistogramParameters mzparams = new ScanHistogramParameters();
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
    var tab = new ScanHistogramTab("", mzparams.cloneParameterSet(), dataFiles);
    tab.setClosable(false);
    tabs.add(tab);
    return tab.getMainPane();
  }

  public void setDataFiles(final Collection<? extends RawDataFile> dataFiles) {
    tabs.forEach(tab -> tab.setDataFiles(dataFiles));
  }

  public Collection<? extends RawDataFile> getDataFiles() {
    return tabs.stream().findFirst().map(ScanHistogramTab::getRawDataFiles).orElseGet(List::of);
  }
}
