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
package io.github.mzmine.modules.visualization.spectra.spectra_stack;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.ms2.interf.AbstractMSMSIdentity;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectrum;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectrumDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.MirrorChartFactory;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.collections.ObservableList;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;

/**
 * Holds more charts for data reviewing
 *
 * @author Robin Schmid
 */
public class SpectraStackVisualizerPane extends BorderPane {

  private final ParameterSet parameters;
  private final ParameterSetupPane paramPane;
  private final Label lbRawIndex;
  //
  private final GridPane pnCharts;
  private final Label lbRawName;
  private boolean exchangeTolerance = true;
  private MZTolerance mzTolerance = new MZTolerance(0.0015, 2.5d);
  // MS 1
  private ChartViewWrapper msone;
  // MS 2
  private ChartGroup group;
  // to flag annotations in spectra
  // annotations for MSMS
  private List<AbstractMSMSIdentity> msmsAnnotations;
  private int currentColumns = 1;
  private FeatureListRow[] rows;
  private RawDataFile selectedRaw;
  private RawDataFile[] allRaw;
  private boolean createMS1;
  private int rawIndex;

  /**
   * Create the frame.
   */
  public SpectraStackVisualizerPane() {
    parameters = MZmineCore.getConfiguration()
        .getModuleParameters(SpectraStackVisualizerModule.class).cloneParameterSet();

//    contentPane.setPadding(new Insets(5));

    paramPane = new ParameterSetupPane(true, true, parameters) {
      @Override
      protected void callOkButton() {
        updateAllCharts();
      }
    };
    Accordion paramAccordion = new Accordion(new TitledPane("Options", paramPane));
    setBottom(paramAccordion);

    // top
    // reset zoom
    Button resetZoom = new Button("Reset zoom");
    resetZoom.setOnAction(e -> {
      if (group != null) {
        group.resetZoom();
      }
    });

    Button prevRaw = new Button("<");
    prevRaw.setOnAction(e -> prevRaw());

    Button nextRaw = new Button(">");
    nextRaw.setOnAction(e -> nextRaw());

    lbRawIndex = new Label("");
    lbRawName = new Label("");

    Pane pnTopMenu = new FlowPane(3, 3, resetZoom, prevRaw, nextRaw, lbRawIndex, lbRawName);
    setTop(pnTopMenu);

    pnCharts = new GridPane();
    setCenter(pnCharts);

    // listeners
    paramPane.getComponentForParameter(
            parameters.getParameter(SpectraStackVisualizerParameters.showLegend)).selectedProperty()
        .addListener((observable, oldValue, newValue) -> updateChartStyle());

    paramPane.getComponentForParameter(
            parameters.getParameter(SpectraStackVisualizerParameters.showTitle)).selectedProperty()
        .addListener((observable, oldValue, newValue) -> updateChartStyle());

    paramPane.getComponentForParameter(
            parameters.getParameter(SpectraStackVisualizerParameters.showCrosshair)).selectedProperty()
        .addListener((observable, oldValue, newValue) -> updateChartStyle());

    paramPane.getComponentForParameter(
            parameters.getParameter(SpectraStackVisualizerParameters.showAllAxes)).selectedProperty()
        .addListener((observable, oldValue, newValue) -> updateChartStyle());
  }

  /**
   * Update chart style, everything that does not trigger an update of all charts
   */
  private void updateChartStyle() {
    paramPane.updateParameterSetFromComponents();
    final boolean showAllAxes = parameters.getValue(SpectraStackVisualizerParameters.showAllAxes);
    final boolean showTitle = parameters.getValue(SpectraStackVisualizerParameters.showTitle);
    final boolean showLegend = parameters.getValue(SpectraStackVisualizerParameters.showLegend);
    final boolean showCrosshair = parameters.getValue(
        SpectraStackVisualizerParameters.showCrosshair);
    if (group != null) {
      group.setShowCrosshair(showCrosshair, showCrosshair);

      int rows = currentRows();
      for (int i = 0; i < group.getList().size(); i++) {
        JFreeChart chart = group.getList().get(i).getChart();
        chart.getLegend().setVisible(showLegend);
        chart.getTitle().setVisible(showTitle);

        // show only the last domain axes
        ValueAxis axis = chart.getXYPlot().getDomainAxis();
        // last in column, last overall, or all
        axis.setVisible(showAllAxes || (i + 1) % rows == 0 || i == group.size() - 1);
      }
    }
  }


  /**
   * ensures the correct number of columns
   */
  private void addColumnsAndRows() {
    boolean userCol = parameters.getValue(SpectraStackVisualizerParameters.columns);
    currentColumns = userCol ? parameters.getParameter(SpectraStackVisualizerParameters.columns)
        .getEmbeddedParameter().getValue() : autoDetectNumberOfColumns();

    ObservableList<RowConstraints> rows = pnCharts.getRowConstraints();
    rows.clear();
    double maxRows = currentRows();
    for (int i = rows.size(); i < maxRows; i++) {
      RowConstraints rc = new RowConstraints();
      rc.setVgrow(Priority.ALWAYS);
      rc.setPercentHeight(100);
      rows.add(rc);
    }

    ObservableList<ColumnConstraints> columns = pnCharts.getColumnConstraints();
    if (currentColumns != columns.size()) {
      columns.clear();
      for (int i = 0; i < currentColumns; i++) {
        var colCon = new ColumnConstraints();
        colCon.setFillWidth(true);
        colCon.setPercentWidth(100);
        columns.add(colCon);
      }
    }
  }

  public int currentRows() {
    return currentColumns == 0 || group == null ? 0
        : (int) Math.ceil(group.size() / (double) currentColumns);
  }

  private int autoDetectNumberOfColumns() {
    return group == null ? 1 : Math.max(1, (int) (Math.floor(Math.sqrt(group.size())) - 1));
  }

  private void nextRaw() {
    if (allRaw == null) {
      return;
    }
    while (rawIndex + 1 < allRaw.length) {
      rawIndex++;
      if (rawContainsFragmentation(allRaw[rawIndex])) {
        setSelectedRaw(allRaw[rawIndex]);
        break;
      }
    }
  }

  private void prevRaw() {
    if (allRaw == null) {
      return;
    }
    while (rawIndex - 1 >= 0) {
      rawIndex--;
      if (rawContainsFragmentation(allRaw[rawIndex])) {
        setSelectedRaw(allRaw[rawIndex]);
        break;
      }
    }
  }

  /**
   * any row contains fragment scan in raw data file
   */
  private boolean rawContainsFragmentation(RawDataFile raw) {
    for (FeatureListRow row : rows) {
      Feature peak = row.getFeature(raw);
      if (peak != null && peak.getMostIntenseFragmentScan() != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * set raw data file and update
   */
  public void setSelectedRaw(RawDataFile selectedRaw) {
    this.selectedRaw = selectedRaw;

    this.rawIndex = 0;
    if (selectedRaw != null) {
      for (int i = 0; i < allRaw.length; i++) {
        if (selectedRaw.equals(allRaw[i])) {
          rawIndex = i;
          break;
        }
      }
    }

    lbRawName.setText(selectedRaw == null ? "" : selectedRaw.getName());
    lbRawIndex.setText("(" + rawIndex + ") ");
    updateAllCharts();
  }

  /**
   * Sort rows
   */
  public void setData(FeatureListRow[] rows, RawDataFile[] allRaw, RawDataFile raw,
      boolean createMS1, SortingProperty sorting, SortingDirection direction) {
    Arrays.sort(rows, new FeatureListRowSorter(sorting, direction));
    setData(rows, allRaw, raw, createMS1);
  }

  /**
   * Create charts and show
   */
  public void setData(FeatureListRow[] rows, RawDataFile[] allRaw, RawDataFile raw,
      boolean createMS1) {
    this.rows = rows;
    this.allRaw = allRaw;
    this.createMS1 = createMS1;

    // check raw
    if (raw != null && !rawContainsFragmentation(raw)) {
      // change to best of the highest row
      raw = Arrays.stream(rows).map(FeatureListRow::getMostIntenseFragmentScan)
          .filter(Objects::nonNull).findFirst().get().getDataFile();
    }
    // set raw and update
    setSelectedRaw(raw);
  }

  /**
   * Create new charts
   */
  public void updateAllCharts() {
    paramPane.updateParameterSetFromComponents();
    final boolean showCrosshair = parameters.getValue(
        SpectraStackVisualizerParameters.showCrosshair);

    msone = null;
    group = new ChartGroup(showCrosshair, showCrosshair, true, false);
    // MS1
    if (createMS1) {
      Scan scan = null;
      Feature best = null;
      for (FeatureListRow r : rows) {
        Feature f = selectedRaw == null ? r.getBestFeature() : r.getFeature(selectedRaw);
        if (f != null && (best == null || f.getHeight() > best.getHeight())) {
          best = f;
        }
      }
      if (best != null) {
        scan = best.getRepresentativeScan();
        EChartViewer cp = SpectrumChartFactory.createScanChartViewer(scan, false, false);
        if (cp != null) {
          msone = new ChartViewWrapper(cp);
        }
      }
    } else {
      // pseudo MS1 from all rows and isotope pattern
      EChartViewer cp = PseudoSpectrum.createChartViewer(rows, selectedRaw, false, "pseudo");
      if (cp != null) {
        cp.setMinHeight(100);
        cp.getChart().getLegend().setVisible(false);
        cp.getChart().getTitle().setVisible(false);
        msone = new ChartViewWrapper(cp);
      }
    }

    if (msone != null) {
      group.add(msone);
    }

    final boolean bestForEach = parameters.getValue(
        SpectraStackVisualizerParameters.useBestForEach);
    final boolean useBestMissingRaw = parameters.getValue(
        SpectraStackVisualizerParameters.useBestMissingRaw);

    // COMMON
    // MS2 of all rows
    for (FeatureListRow row : rows) {
      EChartViewer c = MirrorChartFactory.createMSMSChartViewer(row, selectedRaw, false, false,
          bestForEach, useBestMissingRaw);
      if (c != null) {
//        c.minHeightProperty().bind(pnCharts.heightProperty().divide(rows.length + 1));
        group.add(new ChartViewWrapper(c));
      }
    }

    renewGridLayout(group);
  }

  public void renewGridLayout(ChartGroup group) {
    paramPane.updateParameterSetFromComponents();
    pnCharts.getChildren().clear();
    addColumnsAndRows();
    if (group != null && group.size() > 0) {
      // add to layout
      int maxRows = currentRows();
      int row = 0;
      int col = 0;
      for (ChartViewWrapper cp : group.getList()) {
        pnCharts.add(new BorderPane(cp.getChartFX()), col, row);
        row++;
        if (row == maxRows) {
          col++;
          row = 0;
        }
      }
      updateChartStyle();
    }
  }

  // ANNOTATIONS
  public void addMSMSAnnotation(AbstractMSMSIdentity ann) {
    if (msmsAnnotations == null) {
      msmsAnnotations = new ArrayList<>();
    }
    msmsAnnotations.add(ann);

    // extract mz tolerance
    if (mzTolerance == null || exchangeTolerance) {
      setMzTolerance(ann.getMzTolerance());
    }

    // add to charts
    addAnnotationToCharts(ann);
  }

  public void addMSMSAnnotations(List<? extends AbstractMSMSIdentity> ann) {
    if (ann == null) {
      return;
    }
    // extract mz tolerance
    if (mzTolerance == null || exchangeTolerance) {
      for (AbstractMSMSIdentity a : ann) {
        if (a.getMzTolerance() != null) {
          setMzTolerance(a.getMzTolerance());
          break;
        }
      }
    }

    // add all
    for (AbstractMSMSIdentity a : ann) {
      addMSMSAnnotation(a);
    }
  }

  private void addAllAnnotationsToCharts() {
    if (msmsAnnotations == null) {
      return;
    }

    removeAllAnnotationsFromCharts();

    for (AbstractMSMSIdentity a : msmsAnnotations) {
      addAnnotationToCharts(a);
    }
  }

  private void removeAllAnnotationsFromCharts() {
    forAllCharts(c -> {

    });
  }

  private void addAnnotationToCharts(AbstractMSMSIdentity ann) {
    if (mzTolerance != null) {
      forAllCharts(c -> {
        PseudoSpectrumDataSet data = (PseudoSpectrumDataSet) c.getXYPlot().getDataset(0);
        data.addIdentity(mzTolerance, ann);
      });
    }
  }

  public MZTolerance getMzTolerance() {
    return mzTolerance;
  }

  /**
   * To flag annotations in spectra
   *
   * @param mzTolerance
   */
  public void setMzTolerance(MZTolerance mzTolerance) {
    if (mzTolerance == null && this.mzTolerance == null) {
      return;
    }

    boolean changed =
        mzTolerance != this.mzTolerance || (this.mzTolerance == null && mzTolerance != null)
            || !this.mzTolerance.equals(mzTolerance);
    this.mzTolerance = mzTolerance;
    exchangeTolerance = false;

    if (changed) {
      addAllAnnotationsToCharts();
    }
  }

  /**
   * all charts (ms1 and MS2)
   */
  public void forAllCharts(Consumer<JFreeChart> op) {
    if (group != null) {
      group.forAllCharts(op);
    }
  }

  /**
   * only ms2 charts
   */
  public void forAllMSMSCharts(Consumer<JFreeChart> op) {
    if (group == null || group.getList() == null) {
      return;
    }

    int start = msone == null ? 0 : 1;
    for (int i = start; i < group.getList().size(); i++) {
      op.accept(group.getList().get(i).getChart());
    }
  }
}
