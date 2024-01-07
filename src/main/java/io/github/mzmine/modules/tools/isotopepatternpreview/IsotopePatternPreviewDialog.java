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

package io.github.mzmine.modules.tools.isotopepatternpreview;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ExtendedIsotopePatternDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.SpectraToolTipGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class IsotopePatternPreviewDialog extends ParameterSetupDialog {

  private static final Logger logger = Logger.getLogger(
      IsotopePatternPreviewDialog.class.getName());
  private final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private final NumberFormat intFormat = new DecimalFormat("0.00 %");
  private final SpectraPlot spectraPlot;
  private final EStandardChartTheme theme;
  private final TableView<IsotopePatternTableData> table;
  private final ObservableList<IsotopePatternTableData> tableData;
  private final DoubleParameter pMergeWidth;
  private final PercentParameter pMinIntensity;
  private final StringParameter pFormula;
  private final IntegerParameter pCharge;
  private final SpectraToolTipGenerator ttGen;
  private final PauseTransition listenerDelay;
  private final BooleanParameter pApplyFit;
  private final String exactMassLabel = "Exact mass / Da";
  private final String mzLabel = "m/z";
  IsotopePatternPreviewTask task;
  Color aboveMin, belowMin;
  private double minIntensity, mergeWidth;
  private int charge;
  private PolarityType pol;
  private String formula;
  private ExtendedIsotopePatternDataSet dataset;
  private boolean applyFit;

  public IsotopePatternPreviewDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters, true, false, null);

    // one delay to add to all listeners
    listenerDelay = new PauseTransition(Duration.seconds(1));
    listenerDelay.setOnFinished(event -> delayedHandlingOfParameterChanges());

    aboveMin = MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT();
    belowMin = MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColorAWT();

    pFormula = parameterSet.getParameter(IsotopePatternPreviewParameters.formula);
    pMinIntensity = parameterSet.getParameter(IsotopePatternPreviewParameters.minIntensity);
    pMergeWidth = parameterSet.getParameter(IsotopePatternPreviewParameters.mergeWidth);
    pCharge = parameterSet.getParameter(IsotopePatternPreviewParameters.charge);
    pApplyFit = parameterSet.getParameter(IsotopePatternPreviewParameters.applyFit);

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    mainPane.setPrefSize(screenSize.width / 2d, screenSize.height / 2d);

    // panels
    BorderPane newMainPanel = new BorderPane();
    // pnText = new ScrollPane();
    spectraPlot = new SpectraPlot();
    table = new TableView<>();
    SplitPane pnSplit = new SplitPane(spectraPlot, table);
    pnSplit.setOrientation(Orientation.HORIZONTAL);
    newMainPanel.setPadding(new Insets(5));

    table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    final KeyCodeCombination keyCodeCopy = new KeyCodeCombination(KeyCode.C,
        KeyCombination.CONTROL_ANY);
    table.setOnKeyPressed(event -> {
      if (keyCodeCopy.match(event)) {
        copySelectionToClipboard(table);
      }
    });

    tableData = FXCollections.observableArrayList();
    TableColumn<IsotopePatternTableData, String> mzColumn = new TableColumn<>("m/z");
    TableColumn<IsotopePatternTableData, String> intensityColumn = new TableColumn<>("Intensity");
    TableColumn<IsotopePatternTableData, String> compositionColumn = new TableColumn<>(
        "Composition");
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

    formatChart();
    parametersChanged();
  }

  private void delayedHandlingOfParameterChanges() {
    updateParameterSetFromComponents();
    if (checkParameters()) {
      updateWindow();
    }
  }

  @Override
  protected void parametersChanged() {
    // restart the delay to update everything on any parameter change
    listenerDelay.playFromStart();
  }

  public void updateWindow() {
    if (!updateParameters()) {
      logger.warning("updateWindow() failed. Could not update parameters or parameters are invalid."
          + "\nPlease check the parameters.");
      return;
    }

    if (FormulaUtils.getFormulaSize(formula) > 5E3) {
      logger.finest("Big formula " + formula + " size: " + FormulaUtils.getFormulaSize(formula));
    }

    if (task != null && task.getStatus() == TaskStatus.PROCESSING) {
      task.setDisplayResult(false);
      task.setStatus(TaskStatus.CANCELED);
    }

    logger.finest("Creating new Thread: " + formula);
    task = new IsotopePatternPreviewTask(formula, minIntensity, mergeWidth, charge, pol, applyFit,
        this);
    MZmineCore.getTaskController().addTask(task);
  }

  /**
   * this is being called by the calculation task to update the pattern
   */
  protected void updateChart(SimpleIsotopePattern pattern, XYDataset fit) {
    dataset = new ExtendedIsotopePatternDataSet(pattern, minIntensity, mergeWidth);

    spectraPlot.applyWithNotifyChanges(false, true, () -> {

      final ValueAxis domainAxis = spectraPlot.getXYPlot().getRangeAxis();
      if (pol == PolarityType.NEUTRAL && !exactMassLabel.equals(domainAxis.getLabel())) {
        domainAxis.setLabel(exactMassLabel);
      } else if (pol != PolarityType.NEUTRAL && !mzLabel.equals(domainAxis.getLabel())) {
        domainAxis.setLabel(mzLabel);
      }

      spectraPlot.removeAllDataSets();
      spectraPlot.addDataSet(dataset,
          MZmineCore.getConfiguration().getDefaultColorPalette().getMainColorAWT(), true, false);
      if (fit != null) {
        spectraPlot.addDataSet(fit,
            MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT(), false,
            false);
        spectraPlot.getXYPlot()
            .setRenderer(spectraPlot.getXYPlot().indexOf(fit), new ColoredXYLineRenderer());
      }
      formatChart();
    });
  }

  /**
   * this is being called by the calculation task to update the table
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
    applyFit = pApplyFit.getValue();

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
    if (pMergeWidth.getValue() == null || pMergeWidth.getValue() <= 0.000001d) {
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

  /**
   * <a href="https://stackoverflow.com/a/48126059">https://stackoverflow.com/a/48126059</a>
   */
  @SuppressWarnings("rawtypes")
  public void copySelectionToClipboard(final TableView<?> table) {
    final Set<Integer> rows = new TreeSet<>();
    for (final TablePosition tablePosition : table.getSelectionModel().getSelectedCells()) {
      rows.add(tablePosition.getRow());
    }
    final StringBuilder strb = new StringBuilder();
    boolean firstRow = true;
    for (final Integer row : rows) {
      if (!firstRow) {
        strb.append('\n');
      }
      firstRow = false;
      boolean firstCol = true;
      for (final TableColumn<?, ?> column : table.getColumns()) {
        if (!firstCol) {
          strb.append('\t');
        }
        firstCol = false;
        final Object cellData = column.getCellData(row);
        strb.append(cellData == null ? "" : cellData.toString());
      }
    }
    final ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(strb.toString());
    Clipboard.getSystemClipboard().setContent(clipboardContent);
  }

  public void taskFinishedUpdate(IsotopePatternPreviewTask finishedTask,
      SimpleIsotopePattern pattern, XYDataset fit) {
    // check if task equals latest task
    if (finishedTask.equals(task)) {
      updateTable(pattern);
    }
    // check again. update table might take a while
    if (finishedTask.equals(task)) {
      updateChart(pattern, fit);
    }
  }
}
