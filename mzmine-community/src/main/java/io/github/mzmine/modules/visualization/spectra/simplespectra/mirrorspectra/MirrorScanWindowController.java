/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
package io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.correlation.SpectralSimilarity;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.framework.FormattedTableCell;
import io.github.mzmine.gui.framework.fx.FeatureRowInterfaceFx;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.CosinePairContributions;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SignalAlignmentAnnotation;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.ModifiedCosineSpectralNetworkingTask;
import io.github.mzmine.modules.io.export_features_gnps.GNPSUtils;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.MirrorChartFactory;
import io.github.mzmine.util.components.ColorTableCell;
import io.github.mzmine.util.components.ColorTableCell.Style;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.Weights;
import io.github.mzmine.util.spectraldb.entry.DataPointsTag;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

/**
 * @author Robin Schmid (<a
 * href="https://github.com/robinschmid">https://github.com/robinschmid</a>)
 */

public class MirrorScanWindowController implements FeatureRowInterfaceFx {

  public static final DataPointsTag[] tags = new DataPointsTag[]{DataPointsTag.ORIGINAL,
      DataPointsTag.FILTERED, DataPointsTag.ALIGNED};
  private static final Logger logger = Logger.getLogger(MirrorScanWindowController.class.getName());
  private final ParameterSet parameters;
  // USI / spec number / gnps library id
  @FXML
  public TextField txtTop;
  @FXML
  public TextField txtBottom;
  @FXML
  public Label lbTitleCos;
  @FXML
  public Label lbTitleNL;
  @FXML
  public TitledPane pnParams;
  public BorderPane mainPane;
  public TabPane tabPane;
  public Tab tabNeutralLoss;
  // components
  @FXML
  private BorderPane pnMirror;
  @FXML
  private TableView<TableData> tableMirror;
  @FXML
  private BorderPane pnNLMirror;
  @FXML
  private TableView<TableData> tableNLMIrror;
  @FXML
  private Label lbMirrorStats;
  @FXML
  private Label lbNeutralLossStats;
  @FXML
  private Label lbMirrorModifiedStats;
  @FXML
  private TableColumn<TableData, Double> colMzTop;
  @FXML
  private TableColumn<TableData, Double> colIntensityTop;
  @FXML
  private TableColumn<TableData, Double> colMzBottom;
  @FXML
  private TableColumn<TableData, Double> colIntensityBottom;
  @FXML
  private TableColumn<TableData, SignalAlignmentAnnotation> colMatch;
  @FXML
  private TableColumn<TableData, Color> colMatchColor;
  @FXML
  private TableColumn<TableData, Double> colContribution;
  // neutral loss columns
  @FXML
  private TableColumn<TableData, Double> colMzTop1;
  @FXML
  private TableColumn<TableData, Double> colIntensityTop1;
  @FXML
  private TableColumn<TableData, Double> colMzBottom1;
  @FXML
  private TableColumn<TableData, Double> colIntensityBottom1;
  @FXML
  private TableColumn<TableData, SignalAlignmentAnnotation> colMatch1;
  @FXML
  private TableColumn<TableData, Color> colMatchColor1;
  @FXML
  private TableColumn<TableData, Double> colContribution1;
  // data
  private EChartViewer mirrorSpecrumPlot;
  private EChartViewer neutralLossMirrorSpecrumPlot;
  private ParameterSetupPane parameterSetupPane;

  // last spectra
  private Double precursorMZA;
  private DataPoint[] dpsA;
  private Double precursorMZB;
  private DataPoint[] dpsB;

  public MirrorScanWindowController() {
    parameters = MZmineCore.getConfiguration().getModuleParameters(MirrorScanModule.class);
  }

  @FXML
  public void initialize() {
    final MZmineConfiguration config = MZmineCore.getConfiguration();

    colMzTop.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue().mzA()));
    colIntensityTop.setCellValueFactory(
        row -> new SimpleObjectProperty<>(row.getValue().intensityA()));
    colMzBottom.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue().mzB()));
    colIntensityBottom.setCellValueFactory(
        row -> new SimpleObjectProperty<>(row.getValue().intensityB()));
    colMatch.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue().match()));
    colMatchColor.setCellValueFactory(
        row -> new SimpleObjectProperty<>(getColor(row.getValue().match())));
    colContribution.setCellValueFactory(
        row -> new SimpleDoubleProperty(row.getValue().contribution()).asObject());

    colMatchColor.setCellFactory(col -> new ColorTableCell<>(Style.CIRCLE));
    colMzTop.setCellFactory(col -> new FormattedTableCell<>(config.getMZFormat()));
    colMzBottom.setCellFactory(col -> new FormattedTableCell<>(config.getMZFormat()));
    colIntensityTop.setCellFactory(col -> new FormattedTableCell<>(config.getIntensityFormat()));
    colIntensityBottom.setCellFactory(col -> new FormattedTableCell<>(config.getIntensityFormat()));
    colContribution.setCellFactory(col -> new FormattedTableCell<>(config.getScoreFormat()));

    // neutral loss columns
    colMzTop1.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue().mzA()));
    colIntensityTop1.setCellValueFactory(
        row -> new SimpleObjectProperty<>(row.getValue().intensityA()));
    colMzBottom1.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue().mzB()));
    colIntensityBottom1.setCellValueFactory(
        row -> new SimpleObjectProperty<>(row.getValue().intensityB()));
    colMatch1.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue().match()));
    colMatchColor1.setCellValueFactory(
        row -> new SimpleObjectProperty<>(getColor(row.getValue().match())));
    colContribution1.setCellValueFactory(
        row -> new SimpleDoubleProperty(row.getValue().contribution()).asObject());

    colMatchColor1.setCellFactory(col -> new ColorTableCell<>(Style.CIRCLE));
    colMzTop1.setCellFactory(col -> new FormattedTableCell<>(config.getMZFormat()));
    colMzBottom1.setCellFactory(col -> new FormattedTableCell<>(config.getMZFormat()));
    colIntensityTop1.setCellFactory(col -> new FormattedTableCell<>(config.getIntensityFormat()));
    colIntensityBottom1.setCellFactory(
        col -> new FormattedTableCell<>(config.getIntensityFormat()));
    colContribution1.setCellFactory(col -> new FormattedTableCell<>(config.getScoreFormat()));

    //
    PauseTransition pause = new PauseTransition(Duration.seconds(1));
    pause.setOnFinished(event -> loadSpectra());
    txtTop.textProperty().addListener((observable, oldValue, newValue) -> pause.playFromStart());
    txtBottom.textProperty().addListener((observable, oldValue, newValue) -> pause.playFromStart());

    parameterSetupPane = new ParameterSetupPane(true, true, parameters) {
      @Override
      protected void callOkButton() {
        updateAll();
      }
    };
    pnParams.setContent(parameterSetupPane);
  }

  public BorderPane getMainPane() {
    return mainPane;
  }

  private void updateAll() {
    if (dpsA != null) {
      setScans(precursorMZA, dpsA, precursorMZB, dpsB);
    }
  }

  private Color getColor(SignalAlignmentAnnotation match) {
    return switch (match) {
      case MATCH -> MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColor();
      case MODIFIED -> MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColor();
      case NONE, FILTERED ->
          MZmineCore.getConfiguration().getDefaultColorPalette().getNeutralColor();
    };
  }

  public void setScans(Double precursorMZA, DataPoint[] dpsA, Double precursorMZB,
      DataPoint[] dpsB) {
    setScans(precursorMZA, dpsA, precursorMZB, dpsB, "", "");
  }

  public void setScans(Double precursorMZA, DataPoint[] dpsA, Double precursorMZB, DataPoint[] dpsB,
      String labelA, String labelB) {
    this.precursorMZA = precursorMZA;
    this.dpsA = dpsA;
    this.precursorMZB = precursorMZB;
    this.dpsB = dpsB;

    boolean hasPrecursorMz = precursorMZA != null && precursorMZB != null;

    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    String precursorString =
        !hasPrecursorMz ? MessageFormat.format(": m/z {0}↔{1}; top↔bottom", labelA, labelB)
            : MessageFormat.format(": m/z {0}↔{1}; top↔bottom",
                mzFormat.format(precursorMZA) + labelA, mzFormat.format(precursorMZB) + labelB);

    pnMirror.setCenter(null);
    pnNLMirror.setCenter(null);

    final MZTolerance mzTol = getMzTolerance();

    mirrorSpecrumPlot = MirrorChartFactory.createMirrorPlotFromAligned(mzTol, true, dpsA,
        precursorMZA, dpsB, precursorMZB);
    pnMirror.setCenter(mirrorSpecrumPlot);
    lbTitleCos.setText("Fragment spectrum mirror" + precursorString);

    // create neutral loss spec
    if (precursorMZA != null && precursorMZA > 0 && precursorMZB != null && precursorMZB > 0) {
      lbTitleNL.setText("Neutral loss mirror" + precursorString);
      neutralLossMirrorSpecrumPlot = MirrorChartFactory.createMirrorPlotFromAligned(mzTol, false,
          ScanUtils.getNeutralLossSpectrum(dpsA, precursorMZA), precursorMZA,
          ScanUtils.getNeutralLossSpectrum(dpsB, precursorMZB), precursorMZB);
      pnNLMirror.setCenter(neutralLossMirrorSpecrumPlot);

    }
    //
    calcSpectralSimilarity(dpsA, precursorMZA, dpsB, precursorMZB);
  }

  private MZTolerance getMzTolerance() {
    parameterSetupPane.updateParameterSetFromComponents();
    return parameters.getValue(MirrorScanParameters.mzTol);
  }

  private void calcSpectralSimilarity(DataPoint[] dpsA, Double precursorMZA, DataPoint[] dpsB,
      Double precursorMZB) {

    lbMirrorStats.setText("");
    lbNeutralLossStats.setText("");
    lbMirrorModifiedStats.setText("");
    tableMirror.getItems().clear();
    tableNLMIrror.getItems().clear();

    parameterSetupPane.updateParameterSetFromComponents();
    final MZTolerance mzTol = parameters.getValue(MirrorScanParameters.mzTol);
    var signalFilters = parameters.getValue(MirrorScanParameters.signalFilters).createFilter();
    Weights weights = parameters.getValue(MirrorScanParameters.weight);

    dpsA = signalFilters.applyFilterAndSortByIntensity(dpsA, precursorMZA);
    dpsB = signalFilters.applyFilterAndSortByIntensity(dpsB, precursorMZB);

    SpectralSimilarity cosine = ModifiedCosineSpectralNetworkingTask.createMS2Sim(mzTol, dpsA, dpsB,
        2, weights);

    if (cosine != null) {
      lbMirrorStats.setText(String.format(
          "    cosine=%1.3f; matched signals=%d; top/bottom: explained intensity=%1.3f/%1.3f; matched signals=%1.3f/%1.3f",
          cosine.cosine(), cosine.overlap(), cosine.explainedIntensityB(),
          cosine.explainedIntensityA(), cosine.overlap() / (double) cosine.sizeB(),
          cosine.overlap() / (double) cosine.sizeA()));
    } else {
      lbMirrorStats.setText("");
    }

    //modified cosine
    if (precursorMZA != null && precursorMZB != null) {
      cosine = ModifiedCosineSpectralNetworkingTask.createMS2SimModificationAware(mzTol, weights,
          dpsA, dpsB, 2, ModifiedCosineSpectralNetworkingTask.SIZE_OVERLAP, precursorMZA,
          precursorMZB);
      if (cosine != null) {
        lbMirrorModifiedStats.setText(String.format(
            "modified=%1.3f; matched signals=%d; top/bottom: explained intensity=%1.3f/%1.3f; matched signals=%1.3f/%1.3f",
            cosine.cosine(), cosine.overlap(), cosine.explainedIntensityB(),
            cosine.explainedIntensityA(), cosine.overlap() / (double) cosine.sizeB(),
            cosine.overlap() / (double) cosine.sizeA()));
      }
    }
    if (cosine != null) {

      // get contributions of all data points
      final CosinePairContributions contributions = ModifiedCosineSpectralNetworkingTask.calculateModifiedCosineSimilarityContributions(
          mzTol, weights, dpsA, dpsB, precursorMZA, precursorMZB);

      if (contributions != null) {
        List<TableData> data = new ArrayList<>(contributions.size());
        for (int i = 0; i < contributions.size(); i++) {
          final DataPoint[] pair = contributions.pairs().get(i);
          Double mza = pair[0] != null ? pair[0].getMZ() : null;
          Double intensitya = pair[0] != null ? pair[0].getIntensity() : null;
          Double mzb = pair[1] != null ? pair[1].getMZ() : null;
          Double intensityb = pair[1] != null ? pair[1].getIntensity() : null;

          data.add(new TableData(mzb, intensityb, mza, intensitya, contributions.match()[i],
              contributions.contributions()[i]));
        }
        tableMirror.getItems().setAll(data);
        colContribution.setSortType(SortType.DESCENDING);
        tableMirror.getSortOrder().setAll(colContribution);
      }
    } else {
      lbMirrorModifiedStats.setText("");
    }

    var tabs = tabPane.getTabs();
    if (precursorMZA == null || precursorMZB == null) {
      tabs.remove(tabNeutralLoss);
      return;
    }

    // neutral loss
    final DataPoint[] nlA = ScanUtils.getNeutralLossSpectrum(dpsA, precursorMZA);
    final DataPoint[] nlB = ScanUtils.getNeutralLossSpectrum(dpsB, precursorMZB);
    Arrays.sort(nlA, DataPointSorter.DEFAULT_INTENSITY);
    Arrays.sort(nlB, DataPointSorter.DEFAULT_INTENSITY);

    cosine = ModifiedCosineSpectralNetworkingTask.createMS2Sim(mzTol, nlA, nlB, 2, weights);
    if (cosine != null) {
      lbNeutralLossStats.setText(String.format(
          "cosine=%1.3f; matched signals=%d; top/bottom: explained intensity=%1.3f/%1.3f; matched signals=%1.3f/%1.3f",
          cosine.cosine(), cosine.overlap(), cosine.explainedIntensityB(),
          cosine.explainedIntensityA(), cosine.overlap() / (double) cosine.sizeB(),
          cosine.overlap() / (double) cosine.sizeA()));

      // get contributions of all data points
      final CosinePairContributions contributions = ModifiedCosineSpectralNetworkingTask.calculateModifiedCosineSimilarityContributions(
          mzTol, weights, nlA, nlB, null, null);

      if (contributions != null) {
        List<TableData> data = new ArrayList<>(contributions.size());
        for (int i = 0; i < contributions.size(); i++) {
          final DataPoint[] pair = contributions.pairs().get(i);
          Double mza = pair[0] != null ? pair[0].getMZ() : null;
          Double intensitya = pair[0] != null ? pair[0].getIntensity() : null;
          Double mzb = pair[1] != null ? pair[1].getMZ() : null;
          Double intensityb = pair[1] != null ? pair[1].getIntensity() : null;

          data.add(new TableData(mza, intensitya, mzb, intensityb, contributions.match()[i],
              contributions.contributions()[i]));
        }

        tableNLMIrror.getItems().addAll(data);
        colContribution1.setSortType(SortType.DESCENDING);
        tableNLMIrror.getSortOrder().setAll(colContribution1);

        if (!tabs.contains(tabNeutralLoss)) {
          tabs.add(tabNeutralLoss);
        }
      }

    } else {
      lbNeutralLossStats.setText("");
    }

  }


  /**
   * Set scan and mirror scan and create chart
   */
  public void setScans(Scan scan, Scan mirror) {
    if (scan == null || mirror == null) {
      clearScans();
      return;
    }
    setScans(scan.getPrecursorMz(), ScanUtils.extractDataPoints(scan, true),
        mirror.getPrecursorMz(), ScanUtils.extractDataPoints(mirror, true));
  }

  public void clearScans() {
    tableMirror.getItems().clear();
    tableNLMIrror.getItems().clear();
    pnMirror.setCenter(null);
    pnNLMirror.setCenter(null);
  }

  public void setScans(Scan scan, Scan mirror, String labelA, String labelB) {
    setScans(scan, mirror);
  }

  /**
   * @param a upper spectrum in the mirror
   * @param b lower spectrum in the mirror
   * @return true if the two rows had MS2 spectra
   */
  public boolean setFeatureListRows(FeatureListRow a, FeatureListRow b) {
    Scan sa = a.getMostIntenseFragmentScan();
    Scan sb = b.getMostIntenseFragmentScan();
    if (sa == null || sb == null) {
      return false;
    }

    setScans(sa, sb);

    return true;
  }

  /**
   * Based on a data base match to a spectral library
   *
   * @param db
   */
  public void setScans(SpectralDBAnnotation db) {
    pnMirror.setCenter(null);
    pnNLMirror.setCenter(null);
    mirrorSpecrumPlot = MirrorChartFactory.createMirrorPlotFromSpectralDBPeakIdentity(db);
    pnMirror.setCenter(mirrorSpecrumPlot);
  }


  private void loadSpectra() {
    final String top = txtTop.getText();
    final String bottom = txtBottom.getText();
    loadGnpsLibrary(top, bottom);
  }


  public void openGnpsLibExample() {
//    tauro cholic acid
    txtTop.setText("CCMSLIB00005435561");
//    glycocholic acid
    txtBottom.setText("CCMSLIB00005435513");
//    alanine cholic acid
//    txtBottom.setText("CCMSLIB00005465895");
//    txtTop.setText("CCMSLIB00000579250");
//    txtBottom.setText("CCMSLIB00000579252");
  }

  private void loadGnpsLibrary(String id1, String id2) {
    try {
      final SpectralLibraryEntry top = GNPSUtils.accessLibraryOrUSISpectrum(id1);
      final SpectralLibraryEntry bottom = GNPSUtils.accessLibraryOrUSISpectrum(id2);

      setScans(top.getPrecursorMZ(), top.getDataPoints(), bottom.getPrecursorMZ(),
          bottom.getDataPoints());
    } catch (IOException e) {
      logger.log(Level.WARNING, "Could not access GNPS library spectrum." + e.getMessage(), e);
    }
  }

  public void openUSIExample1() {
    // Phenylalanine conjugated deoxycholic acid
    // Tyrosine conjugated deoxycholic acid putative [M-H2O+H]+
    txtTop.setText("mzspec:GNPS:GNPS-LIBRARY:accession:CCMSLIB00005716807");
    txtBottom.setText("mzspec:GNPS:GNPS-LIBRARY:accession:CCMSLIB00005467948");
  }

  public void openUSIExample2() {
    // Phenylalanine conjugated deoxycholic acid
    // Tyrosine conjugated deoxycholic acid putative
    txtTop.setText("mzspec:GNPS:GNPS-LIBRARY:accession:CCMSLIB00005716807");
    txtBottom.setText("mzspec:GNPS:GNPS-LIBRARY:accession:CCMSLIB00005467946");
  }

  @Override
  public boolean hasContent() {
    return !pnMirror.getChildren().isEmpty();
  }

  @Override
  public void setFeatureRows(final @NotNull List<? extends FeatureListRow> selectedRows) {
    if (selectedRows.size() >= 2) {
      setScans(selectedRows.get(0).getMostIntenseFragmentScan(),
          selectedRows.get(1).getMostIntenseFragmentScan());
    } else {
      clearScans();
    }
  }
}
