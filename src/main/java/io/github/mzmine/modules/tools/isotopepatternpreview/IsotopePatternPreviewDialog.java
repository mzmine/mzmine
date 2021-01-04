/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.tools.isotopepatternpreview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Logger;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ExtendedIsotopePatternDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.SpectraToolTipGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerComponent;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentComponent;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.scans.ScanUtils;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class IsotopePatternPreviewDialog extends ParameterSetupDialog {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat intFormat = new DecimalFormat("0.00 %");
  private NumberFormat relFormat = new DecimalFormat("0.00000");

  private double minIntensity, mergeWidth;
  private int charge;
  private PolarityType pol;
  private String formula;

  private SpectraPlot spectraPlot;
  private EStandardChartTheme theme;
  private BorderPane newMainPanel;
  private HBox pnlParameters;
  private SplitPane pnSplit;
  private VBox pnlControl;

  private TableView<IsotopePatternTableData> table;
  private ObservableList<IsotopePatternTableData> tableData;

  private DoubleParameter pMergeWidth;
  private PercentParameter pMinIntensity;
  private StringParameter pFormula;
  private IntegerParameter pCharge;

  private DoubleComponent cmpMergeWidth;
  private PercentComponent cmpMinIntensity;
  private TextField cmpFormula;
  private IntegerComponent cmpCharge;

  private ExtendedIsotopePatternDataSet dataset;
  private SpectraToolTipGenerator ttGen;

  IsotopePatternPreviewTask task;
  private boolean newParameters;

  private long lastCalc;

  Color aboveMin, belowMin;

  ParameterSet customParameters;

  public IsotopePatternPreviewDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    aboveMin = MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT();
    belowMin = MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColorAWT();

    lastCalc = 0;

    newParameters = false;

    pFormula = parameterSet.getParameter(IsotopePatternPreviewParameters.formula);
    pMinIntensity = parameterSet.getParameter(IsotopePatternPreviewParameters.minIntensity);
    pMergeWidth = parameterSet.getParameter(IsotopePatternPreviewParameters.mergeWidth);
    pCharge = parameterSet.getParameter(IsotopePatternPreviewParameters.charge);

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    mainPane.setPrefSize(screenSize.width / 2, screenSize.height / 2);

    cmpMinIntensity = getComponentForParameter(IsotopePatternPreviewParameters.minIntensity);
    cmpMergeWidth = getComponentForParameter(IsotopePatternPreviewParameters.mergeWidth);
    cmpCharge = getComponentForParameter(IsotopePatternPreviewParameters.charge);
    cmpFormula = getComponentForParameter(IsotopePatternPreviewParameters.formula);

    // panels
    newMainPanel = new BorderPane();
    // pnText = new ScrollPane();
    spectraPlot = new SpectraPlot();
    table = new TableView<>();
    pnSplit = new SplitPane(spectraPlot, table);
    pnSplit.setOrientation(Orientation.HORIZONTAL);
    pnlParameters = new HBox();
    pnlControl = new VBox();
    newMainPanel.setPadding(new Insets(5));

    tableData = FXCollections.observableArrayList();
    TableColumn<IsotopePatternTableData, String> mzColumn = new TableColumn<>("m/z");
    TableColumn<IsotopePatternTableData, String> intensityColumn = new TableColumn<>("Intensity");
    TableColumn<IsotopePatternTableData, String> compositionColumn =
        new TableColumn<>("Composition");
    mzColumn.setCellValueFactory(
        data -> new SimpleStringProperty(mzFormat.format(data.getValue().getMz())));
    intensityColumn.setCellValueFactory(
        data -> new SimpleStringProperty(intFormat.format(data.getValue().getAbundance())));
    compositionColumn.setCellValueFactory(new PropertyValueFactory<>("composition"));
    mzColumn.getStyleClass().add("number-column");
    intensityColumn.getStyleClass().add("number-column");
    table.getColumns().addAll(mzColumn, intensityColumn, compositionColumn);
    table.setItems(tableData);
    DoubleBinding otherWidth = mzColumn.widthProperty().add(intensityColumn.widthProperty());
    compositionColumn.prefWidthProperty().bind(table.widthProperty().subtract(otherWidth));

    // controls
    ttGen = new SpectraToolTipGenerator();
    theme = MZmineCore.getConfiguration().getDefaultChartTheme();

    // reorganize
    mainPane.getChildren().remove(paramsPane);
    newMainPanel.setCenter(pnSplit);
    newMainPanel.setBottom(paramsPane);
    mainPane.setCenter(newMainPanel);
    pnlButtons.getButtons().remove(super.btnCancel);

    formatChart();
    parametersChanged();
  }

  @Override
  protected void parametersChanged() {
    updateWindow();
  }

  // -----------------------------------------------------
  // methods
  // -----------------------------------------------------
  public void updateWindow() {
    if (!updateParameters()) {
      logger.warning("updateWindow() failed. Could not update parameters or parameters are invalid."
          + "\nPlease check the parameters.");
      return;
    }

    if (FormulaUtils.getFormulaSize(formula) > 5E3
        && ((System.nanoTime() - lastCalc) * 1E-6 < 150)) {
      logger.finest("Big formula " + formula + " size: " + FormulaUtils.getFormulaSize(formula)
          + " or last calculation recent: " + (System.nanoTime() - lastCalc) / 1E6 + " ms");
    }

    if (task != null && task.getStatus() == TaskStatus.PROCESSING
        && FormulaUtils.getFormulaSize(formula) > 1E4) {
      newParameters = true;
      task.setDisplayResult(false);
      task.setStatus(TaskStatus.CANCELED);
    } else {
      if (task != null) {
        task.setDisplayResult(false);
      }
      logger.finest("Creating new Thread: " + formula);
      task = new IsotopePatternPreviewTask(formula, 0.001, mergeWidth, charge, pol, this);
      MZmineCore.getTaskController().addTask(task);
    }

    lastCalc = System.nanoTime();
  }

  /**
   * this is being called by the calculation task to update the pattern
   *
   * @param pattern
   */
  protected void updateChart(SimpleIsotopePattern pattern) {
    dataset = new ExtendedIsotopePatternDataSet(pattern, minIntensity, mergeWidth);

    if (pol == PolarityType.NEUTRAL) {
      spectraPlot.getXYPlot().getRangeAxis().setLabel("Exact mass / Da");
    } else {
      spectraPlot.getXYPlot().getRangeAxis().setLabel("m/z");
    }
    spectraPlot.removeAllDataSets();
    spectraPlot.addDataSet(dataset,
        MZmineCore.getConfiguration().getDefaultColorPalette().getMainColorAWT(), true);
    formatChart();
  }

  /**
   * this is being called by the calculation task to update the table
   *
   * @param pattern
   */
  protected void updateTable(SimpleIsotopePattern pattern) {
    DataPoint[] dp = ScanUtils.extractDataPoints(pattern);
    tableData.clear();
    for (int i = 0; i < pattern.getNumberOfDataPoints(); i++) {
      tableData.add(new IsotopePatternTableData(dp[i].getMZ(), dp[i].getIntensity(),
          pattern.getIsotopeComposition(i)));
    }

    /*
     * if (pol == PolarityType.NEUTRAL) table = new TableView(data, columns[0]); // column 1 =
     * "Exact mass / // Da" else table = new JTable(data, columns[1]); // column 2 = "m/z"
     */
    // pnText.setViewportView(table);
    // table.setDefaultEditor(Object.class, null); // make editing impossible
  }

  private void formatChart() {
    theme.apply(spectraPlot.getChart());
    // plot.addRangeMarker(new ValueMarker(minIntensity, belowMin, new
    // BasicStroke(1.0f)));
    ((NumberAxis) spectraPlot.getXYPlot().getDomainAxis()).setNumberFormatOverride(mzFormat);
    ((NumberAxis) spectraPlot.getXYPlot().getRangeAxis()).setNumberFormatOverride(intFormat);

    XYItemRenderer r = spectraPlot.getXYPlot().getRendererForDataset(dataset);
    r.setSeriesPaint(0, aboveMin);
    r.setSeriesPaint(1, belowMin);
    r.setDefaultToolTipGenerator(ttGen);
  }

  private boolean updateParameters() {
    updateParameterSetFromComponents();
    if (!checkParameters()) {
      logger.fine("updateParameters() failed due to invalid input.");
      return false;
    }

    formula = pFormula.getValue();
    mergeWidth = pMergeWidth.getValue();
    minIntensity = pMinIntensity.getValue();
    charge = pCharge.getValue();

    if (charge > 0) {
      pol = PolarityType.POSITIVE;
    } else if (charge < 0) {
      pol = PolarityType.NEGATIVE;
      charge *= -1;
    } else {
      pol = PolarityType.NEUTRAL;
    }

    return true;
  }

  private boolean checkParameters() {
    if (pFormula.getValue() == null || pFormula.getValue().equals("")
        || !FormulaUtils.checkMolecularFormula(pFormula.getValue())) {
      logger.fine("Invalid input or Element == \"\" or invalid elements.");
      return false;
    }
    if (pMinIntensity.getValue() == null || pMinIntensity.getValue() > 1.0d
        || pMinIntensity.getValue() < 0.0d) {
      logger.fine("Minimum intensity invalid. " + pMinIntensity.getValue());
      return false;
    }
    if (pMergeWidth.getValue() == null || pMergeWidth.getValue() < 0.0d) {
      logger.fine("Merge width invalid. " + pMergeWidth.getValue());
      return false;
    }
    if (pCharge.getValue() == null) {
      logger.fine("Charge invalid. " + pCharge.getValue());
      return false;
    }

    logger.finest("Parameters valid");
    return true;
  }

  public void startNextThread() {
    if (newParameters) {
      newParameters = false;
      logger.finest("Creating new Thread: " + formula);
      task = new IsotopePatternPreviewTask(formula, minIntensity, mergeWidth, charge, pol, this);
      MZmineCore.getTaskController().addTask(task);
    }
  }
}
