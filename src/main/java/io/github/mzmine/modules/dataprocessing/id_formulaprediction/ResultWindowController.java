/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.id_formulaprediction;

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
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.Comparators;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.ExitCode;
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
import java.time.Instant;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;


public class ResultWindowController {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final NumberFormat massFormat = MZmineCore.getConfiguration().getMZFormat();
  private final DecimalFormat percentFormat = new DecimalFormat("##.##%");
  private final NumberFormat ppmFormat = new DecimalFormat("0.0");

  private final ObservableList<ResultFormula> formulas = FXCollections.observableArrayList();
  public CheckBox cbLimitFormula;
  public TextField txtMaxFormula;
  public TextField txtSearchedMz;
  public HBox pnParam;
  // controls to change the mz and parameters on the fly
  private ResultWindowFX window;
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

  private @Nullable FeatureListRow featureListRow;
  private AbstractTask searchTask;
  private String title;
  private double searchedMass;
  private ParameterSet parameters;
  private PauseTransition updateDelay;

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

    txtSearchedMz.setTooltip(new Tooltip(
        "Uses this m/z for the search instead of the one defined in the parameters (if set)"));
    cbLimitFormula.setTooltip(new Tooltip(
        "Quick way to limit the elements even further to a defined formula, e.g., C12H20O2"));
    txtMaxFormula.setTooltip(new Tooltip(
        "Quick way to limit the elements even further to a defined formula, e.g., C12H20O2"));

    updateDelay = new PauseTransition(Duration.seconds(1.5));
    updateDelay.setOnFinished(event -> rerunPrediction());
    txtSearchedMz.textProperty().addListener((observable, oldValue, newValue) -> {
      updateDelay.playFromStart();
    });
    txtMaxFormula.textProperty().addListener((observable, oldValue, newValue) -> {
      updateDelay.playFromStart();
    });

    cbLimitFormula.selectedProperty()
        .addListener((observable, oldValue, newValue) -> rerunPrediction());

    resultTable.setItems(formulas);
  }

  public void initValues(ResultWindowFX window, String title, FeatureListRow peakListRow,
      double searchedMass, int charge, AbstractTask searchTask, @Nullable ParameterSet parameters) {
    this.window = window;
    this.title = title;
    this.featureListRow = peakListRow;
    this.searchTask = searchTask;
    this.searchedMass = searchedMass;
    this.parameters = parameters;
  }

  @FXML
  private void addIdentityClick(ActionEvent ae) {
    if (featureListRow == null) {
      return;
    }

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

    if (featureListRow != null) {
      Feature peak = featureListRow.getBestFeature();

      RawDataFile dataFile = peak.getRawDataFile();
      Scan scanNumber = peak.getRepresentativeScan();
      SpectraVisualizerModule.addNewSpectrumTab(dataFile, scanNumber, null,
          peak.getIsotopePattern(), predictedPattern);
    }
  }

  @FXML
  private void viewIsotopeMirrorClick(ActionEvent ae) {
    ResultFormula formula = resultTable.getSelectionModel().getSelectedItem();
    if (formula == null) {
      MZmineCore.getDesktop().displayMessage(null, "Select one result to copy");
      return;
    }

    logger.finest(
        "Showing isotope pattern mirror match for formula " + formula.getFormulaAsString());
    IsotopePattern predictedPattern = formula.getPredictedIsotopes();

    if (predictedPattern == null) {
      return;
    }

    if (featureListRow != null) {
      Feature peak = featureListRow.getBestFeature();
      final IsotopePattern detectedPattern = peak.getIsotopePattern().getRelativeIntensity();

      final UnitFormat uf = MZmineCore.getConfiguration().getUnitFormat();
      EChartViewer mirrorChart = MirrorChartFactory.createMirrorChartViewer(detectedPattern,
          predictedPattern, uf.format("Detected pattern", "%"), uf.format("Predicted pattern", "%"),
          false, true);

      SimpleTab tab = new SimpleTab("Isotope mirror");
      tab.setContent(mirrorChart);

      MZmineCore.getDesktop().addTab(tab);
    }
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

    if (featureListRow != null) {
      Feature bestPeak = featureListRow.getBestFeature();

      RawDataFile dataFile = bestPeak.getRawDataFile();
      Scan msmsScanNumber = bestPeak.getMostIntenseFragmentScan();

      if (msmsScanNumber == null) {
        return;
      }

      SpectraVisualizerTab msmsPlot = SpectraVisualizerModule.addNewSpectrumTab(dataFile,
          msmsScanNumber);

      if (msmsPlot == null) {
        return;
      }
      Map<Double, String> annotation = formula.getMSMSannotation();

      if (annotation == null) {
        return;
      }
      msmsPlot.addMzAnnotation(annotation);
    }
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


  public void setIsotopes(ActionEvent actionEvent) {
  }

  public void setFragmentationSpectra(ActionEvent actionEvent) {
  }

  public ParameterSet getParameters() {
    if (parameters == null) {
      parameters = MZmineCore.getConfiguration().getModuleParameters(FormulaPredictionModule.class);
    }
    return parameters;
  }

  public void setParameters(ActionEvent actionEvent) {
    if (parameters == null) {
      parameters = MZmineCore.getConfiguration().getModuleParameters(FormulaPredictionModule.class);
    }
    if (parameters.showSetupDialog(true) == ExitCode.OK) {
      rerunPrediction();
    }
  }

  private void rerunPrediction() {
    updateDelay.stop();
    if (searchTask != null && !searchTask.isFinished()) {
      searchTask.cancel();
    }

    // read mz and
    final ParameterSet param = getParameters().cloneParameterSet();

    double searchedMz = -1;
    try {
      // optionally set
      searchedMz = Double.parseDouble(txtSearchedMz.getText());
      param.getParameter(FormulaPredictionParameters.neutralMass).setIonMass(searchedMz);
    } catch (Exception ex) {
    }

    try {
      // TODO set maximum elements based on formula input
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Error during formula set " + ex.getMessage(), ex);
    }

    // neutral mass
    searchedMass = param.getValue(FormulaPredictionParameters.neutralMass);

    // run new task
    searchTask = new FormulaPredictionTask(param, Instant.now(), window, null, null);
    MZmineCore.getTaskController().addTask(searchTask, TaskPriority.HIGH);
  }
}
