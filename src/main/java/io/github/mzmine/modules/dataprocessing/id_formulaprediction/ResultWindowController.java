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

package io.github.mzmine.modules.dataprocessing.id_formulaprediction;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.Comparators;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.MirrorChartFactory;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;
import java.util.logging.Logger;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;


public class ResultWindowController {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final NumberFormat massFormat = MZmineCore.getConfiguration().getMZFormat();
  private final DecimalFormat percentFormat = new DecimalFormat("##.##%");
  private final NumberFormat ppmFormat = new DecimalFormat("0.0");

  private final ObservableList<ResultFormula> formulas = FXCollections.observableArrayList();

  @FXML
  private TableView<ResultFormula> resultTable;
  @FXML
  private TableColumn<ResultFormula, String> Formula;
  @FXML
  private TableColumn<ResultFormula, Float> absoluteMassDifference;
  @FXML
  private TableColumn<ResultFormula, Float> massDifference;
  @FXML
  private TableColumn<ResultFormula, Float> RDBE;
  @FXML
  private TableColumn<ResultFormula, String> isotopePattern;
  @FXML
  private TableColumn<ResultFormula, String> msScore;
  @FXML
  private TableColumn<ResultFormula, String> combinedScore;

  private FeatureListRow featureListRow;
  private Task searchTask;
  private String title;
  private double searchedMass;

  @FXML
  private void initialize() {
    Formula.setCellValueFactory(cell -> {
      String formula = cell.getValue().getFormulaAsString();
      String cellVal = "";
      if (cell.getValue().getFormulaAsString() != null) {
        cellVal = formula;
      }

      return new ReadOnlyObjectWrapper<>(cellVal);
    });

    RDBE.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getRDBE()));

    absoluteMassDifference.setComparator(Comparators.COMPARE_ABS_FLOAT);
    absoluteMassDifference.setCellValueFactory(cell -> {
      double exactMass = cell.getValue().getExactMass();
      double massDiff = searchedMass - exactMass;

      return new ReadOnlyObjectWrapper<>(Float.parseFloat(massFormat.format(massDiff)));
    });

    massDifference.setComparator(Comparators.COMPARE_ABS_FLOAT);
    massDifference.setCellValueFactory(cell -> {
      double ExactMass = cell.getValue().getExactMass();
      double MassDiff = searchedMass - ExactMass;
      MassDiff = (MassDiff / ExactMass) * 1E6;

      return new ReadOnlyObjectWrapper<>(Float.parseFloat(ppmFormat.format(MassDiff)));
    });

    isotopePattern.setCellValueFactory(cell -> {
      final Float score = cell.getValue().getIsotopeScore();
      final String cellVal = score == null ? "" : percentFormat.format(score);
      return new ReadOnlyObjectWrapper<>(cellVal);
    });

    msScore.setCellValueFactory(cell -> {
      final Float score = cell.getValue().getMSMSScore();
      final String cellVal = score == null ? "" : percentFormat.format(score);
      return new ReadOnlyObjectWrapper<>(cellVal);
    });

    combinedScore.setCellValueFactory(cell -> {
      final float score = cell.getValue().getScore(10, 3, 1);
      final String cellVal = percentFormat.format(score);
      return new ReadOnlyObjectWrapper<>(cellVal);
    });

    resultTable.setItems(formulas);
  }

  public void initValues(String title, FeatureListRow peakListRow, double searchedMass, int charge,
      Task searchTask) {

    this.title = title;
    this.featureListRow = peakListRow;
    this.searchTask = searchTask;
    this.searchedMass = searchedMass;
  }

  @FXML
  private void addIdentityClick(ActionEvent ae) {

    ResultFormula formula = resultTable.getSelectionModel().getSelectedItem();

    if (formula == null) {
      MZmineCore.getDesktop().displayMessage(null, "Select one result to add as compound identity");
      return;
    }

    SimpleFeatureIdentity newIdentity = new SimpleFeatureIdentity(formula.getFormulaAsString());
    featureListRow.addFeatureIdentity(newIdentity, false);

    dispose();
  }

  @FXML
  private void exportClick(ActionEvent ae) throws IOException {

    // Ask for filename
    FileChooser fileChooser = new FileChooser();
    File result = fileChooser.showSaveDialog(null);
    if (result == null) {
      return;
    }
    fileChooser.setTitle("Export");

    try {
      FileWriter fileWriter = new FileWriter(result);
      BufferedWriter writer = new BufferedWriter(fileWriter);
      writer.write("Formula,Mass,RDBE,Isotope pattern score,MS/MS score");
      writer.newLine();

      for (int row = 0; row < resultTable.getItems().size(); row++) {
        ResultFormula formula = resultTable.getItems().get(row);
        writer.write(formula.getFormulaAsString());
        writer.write(",");
        writer.write(String.valueOf(formula.getExactMass()));
        writer.write(",");
        if (formula.getRDBE() != null) {
          writer.write(String.valueOf(formula.getRDBE()));
        }
        writer.write(",");
        if (formula.getIsotopeScore() != null) {
          writer.write(String.valueOf(formula.getIsotopeScore()));
        }
        writer.write(",");
        if (formula.getMSMSScore() != null) {
          writer.write(String.valueOf(formula.getMSMSScore()));
        }
        writer.newLine();
      }

      writer.close();

    } catch (Exception ex) {
      MZmineCore.getDesktop().displayErrorMessage(
          "Error writing to file " + result + ": " + ExceptionUtils.exceptionToString(ex));
    }
    return;

  }

  @FXML
  private void viewIsotopeClick(ActionEvent ae) {
    ResultFormula formula = resultTable.getSelectionModel().getSelectedItem();
    if (formula == null) {
      MZmineCore.getDesktop().displayMessage(null, "Select one result to copy");
      return;
    }

    logger.finest("Showing isotope pattern for formula " + formula.getFormulaAsString());
    IsotopePattern predictedPattern = formula.getPredictedIsotopes();

    if (predictedPattern == null) {
      return;
    }

    Feature peak = featureListRow.getBestFeature();

    RawDataFile dataFile = peak.getRawDataFile();
    Scan scanNumber = peak.getRepresentativeScan();
    SpectraVisualizerModule
        .addNewSpectrumTab(dataFile, scanNumber, null, peak.getIsotopePattern(), predictedPattern);
  }

  @FXML
  private void viewIsotopeMirrorClick(ActionEvent ae) {
    ResultFormula formula = resultTable.getSelectionModel().getSelectedItem();
    if (formula == null) {
      MZmineCore.getDesktop().displayMessage(null, "Select one result to copy");
      return;
    }

    logger
        .finest("Showing isotope pattern mirror match for formula " + formula.getFormulaAsString());
    IsotopePattern predictedPattern = formula.getPredictedIsotopes();

    if (predictedPattern == null) {
      return;
    }

    Feature peak = featureListRow.getBestFeature();
    final IsotopePattern detectedPattern = peak.getIsotopePattern().getRelativeIntensityCopy();

    final UnitFormat uf = MZmineCore.getConfiguration().getUnitFormat();
    EChartViewer mirrorChart = MirrorChartFactory
        .createMirrorChartViewer(detectedPattern, predictedPattern,
            uf.format("Detected pattern", "%"), uf.format("Predicted pattern", "%"), false, true);

    SimpleTab tab = new SimpleTab("Isotope mirror");
    tab.setContent(mirrorChart);

    MZmineCore.getDesktop().addTab(tab);
  }

  @FXML
  private void copyClick(ActionEvent ae) {
    ResultFormula formula = resultTable.getSelectionModel().getSelectedItem();
    if (formula == null) {
      MZmineCore.getDesktop().displayMessage(null, "Select one result to copy");
      return;
    }

    String formulaString = formula.getFormulaAsString();
    StringSelection stringSelection = new StringSelection(formulaString);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, null);
  }

  @FXML
  private void showsMSClick(ActionEvent ae) {
    ResultFormula formula = resultTable.getSelectionModel().getSelectedItem();
    if (formula == null) {
      MZmineCore.getDesktop().displayMessage(null, "Select one result to show MS score");
      return;
    }

    Feature bestPeak = featureListRow.getBestFeature();

    RawDataFile dataFile = bestPeak.getRawDataFile();
    Scan msmsScanNumber = bestPeak.getMostIntenseFragmentScan();

    if (msmsScanNumber == null) {
      return;
    }

    SpectraVisualizerTab msmsPlot = SpectraVisualizerModule
        .addNewSpectrumTab(dataFile, msmsScanNumber);

    if (msmsPlot == null) {
      return;
    }
    Map<DataPoint, String> annotation = formula.getMSMSannotation();

    if (annotation == null) {
      return;
    }
    msmsPlot.addMzAnnotation(annotation);
  }

  public void addNewListItem(final ResultFormula formula) {
    MZmineCore.runLater(() -> formulas.add(formula));
  }

  public void dispose() {

    // Cancel the search task if it is still running
    TaskStatus searchStatus = searchTask.getStatus();
    if ((searchStatus == TaskStatus.WAITING) || (searchStatus == TaskStatus.PROCESSING)) {
      searchTask.cancel();
    }

    resultTable.getScene().getWindow().hide();

  }


}
