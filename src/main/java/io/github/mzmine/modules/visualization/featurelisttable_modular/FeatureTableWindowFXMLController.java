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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.data.types.numbers.BestScanNumberType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

public class FeatureTableWindowFXMLController {

  private static final Logger logger = Logger
      .getLogger(FeatureTableWindowFXMLController.class.getName());

  private static ParameterSet param;
  private TICPlot xicPlot;
  private SpectraPlot spectrumPlot;

  @FXML
  private BorderPane pnMain;

  @FXML
  private CheckMenuItem miShowXIC;

  @FXML
  private CheckMenuItem miShowSpectrum;

  @FXML
  private SplitPane pnSpectrumXICSplit;

  @FXML
  private SplitPane pnTablePreviewSplit;

  @FXML
  private MenuItem miParameters;

  @FXML
  private StackPane pnMainCenter;

  @FXML
  private StackPane pnPreview;

  @FXML
  private ChoiceBox<DataType> cmbFilter;

  @FXML
  private TextField txtSearch;

  @FXML
  private FeatureTableFX featureTable;


  public void initialize() {
    param = MZmineCore.getConfiguration()
        .getModuleParameters(FeatureTableFXModule.class);

    miShowSpectrum
        .setSelected(param.getParameter(FeatureTableFXParameters.showSpectrum).getValue());
    miShowXIC.setSelected(param.getParameter(FeatureTableFXParameters.showXIC).getValue());

    xicPlot = new TICPlot();
    xicPlot.setPlotType(TICPlotType.TIC);
    xicPlot.setMinHeight(150);
    xicPlot.setPrefHeight(150);

    spectrumPlot = new SpectraPlot();
    spectrumPlot.setMinHeight(150);
    spectrumPlot.setPrefHeight(150);

    cmbFilter.setConverter(new StringConverter<>() {
      @Override
      public String toString(DataType object) {
        return object == null ? null : object.getHeaderString();
      }

      @Override
      public DataType fromString(String string) {
        return string == null ? null : cmbFilter.getItems().stream().filter(dt -> dt.getHeaderString().equals(string))
            .findFirst()
            .orElse(null);
      }
    });

    pnTablePreviewSplit.setDividerPosition(0, 1);
    pnSpectrumXICSplit.getItems().addListener((ListChangeListener<? super Node>) change -> {
      change.next();
      if (change.getList().isEmpty()) {
        pnTablePreviewSplit.setDividerPosition(0, 1);
        pnPreview.setVisible(false);
        pnPreview.setPrefHeight(0.0);
        pnSpectrumXICSplit.setVisible(false);
        pnSpectrumXICSplit.setPrefHeight(0.0);
      } else {
        pnTablePreviewSplit.setDividerPosition(0, 0.7);
        pnPreview.setVisible(true);
        pnPreview.setPrefHeight(100);
        pnSpectrumXICSplit.setVisible(true);
        pnSpectrumXICSplit.setPrefHeight(100);
      }
    });

    featureTable.getSelectionModel().selectedItemProperty()
        .addListener(((obs, o, n) -> selectedRowChanged()));

    miShowXICOnAction(null);
    miShowSpectrumOnAction(null);
  }

  @FXML
  void miParametersOnAction(ActionEvent event) {
    Platform.runLater(() -> {
      ExitCode exitCode = param.showSetupDialog(true);
      if (exitCode == ExitCode.OK) {
        updateWindowToParameterSetValues();
        featureTable.applyColumnVisibility();
      }
    });
  }

  @FXML
  void miShowXICOnAction(ActionEvent event) {
    if (event != null) {
      event.consume();
    }

    if (miShowXIC.isSelected()) {
      if (!pnSpectrumXICSplit.getItems().contains(xicPlot)) {
        pnSpectrumXICSplit.getItems().add(xicPlot);
      }
    } else {
      pnSpectrumXICSplit.getItems().remove(xicPlot);
    }

    param.getParameter(FeatureTableFXParameters.showXIC).setValue(miShowXIC.isSelected());
  }

  @FXML
  void miShowSpectrumOnAction(ActionEvent event) {
    if (event != null) {
      event.consume();
    }

    if (miShowSpectrum.isSelected()) {
      if (!pnSpectrumXICSplit.getItems().contains(spectrumPlot)) {
        pnSpectrumXICSplit.getItems().add(spectrumPlot);
      }
    } else {
      pnSpectrumXICSplit.getItems().remove(spectrumPlot);
    }

    param.getParameter(FeatureTableFXParameters.showSpectrum).setValue(miShowSpectrum.isSelected());
  }


  /**
   * Updates the bottom xic to the selected feature.
   *
   * @param selectedRow
   */
  void updateXICPlot(ModularFeatureListRow selectedRow) {
    if (!miShowXIC.isSelected()) {
      return;
    }
    xicPlot.removeAllDataSets();
    // TODO: for now we take the first raw data file, we should take the one from the
    //  selected column, though.
    Set<RawDataFile> rawDataFileSet = selectedRow.getFeatures().keySet();
    List<RawDataFile> raws = new ArrayList<>();
    raws.addAll(rawDataFileSet);

    ModularFeature feature = selectedRow.getFeatures().get(raws.get(0));
    TICDataSet dataSet = new TICDataSet(feature);
    xicPlot.addTICDataSet(dataSet);
  }

  /**
   * Updates the bottom spectrum to the selected feature.
   *
   * @param selectedRow
   */
  void updateSpectrumPlot(ModularFeatureListRow selectedRow) {
    if (!miShowSpectrum.isSelected()) {
      return;
    }

    // TODO: for now we take the first raw data file, we should take the one from the
    //  selected column, though.
    Set<RawDataFile> rawDataFileSet = selectedRow.getFeatures().keySet();
    List<RawDataFile> raws = new ArrayList<>();
    raws.addAll(rawDataFileSet);

    ModularFeature feature = selectedRow.getFeatures().get(raws.get(0));
    ScanDataSet scanDataSet = new ScanDataSet(raws.get(0).getScan((Integer) feature.getValue(
        BestScanNumberType.class)));

    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    spectrumPlot.removeAllDataSets();
    spectrumPlot.addDataSet(scanDataSet, palette.getMainColorAWT(), false);
  }


  /**
   * In case the parameters are changed in the setup dialog, they are applied to the window.
   */
  void updateWindowToParameterSetValues() {
    miShowSpectrum
        .setSelected(param.getParameter(FeatureTableFXParameters.showSpectrum).getValue());
    miShowXIC.setSelected(param.getParameter(FeatureTableFXParameters.showXIC).getValue());

    miShowSpectrumOnAction(null);
    miShowXICOnAction(null);
  }


  public void setFeatureList(ModularFeatureList featureList) {
    featureTable.addData(featureList);
    setupFilter();
  }


  private void setupFilter() {
    ModularFeatureList flist = featureTable.getFeatureList();
    if (flist == null) {
      logger.info("Cannot setup filters for feature list window. Feature list not loaded.");
    }

    for (DataType<?> dataType : flist.getRowTypes().values()) {
      if (dataType instanceof SubColumnsFactory) {
        continue;
      }
      cmbFilter.getItems().add(dataType);
    }

    txtSearch.setOnKeyReleased(keyEvent -> searchFeatureTable());
    cmbFilter.valueProperty().addListener((observable, oldVal, newVal) -> searchFeatureTable());
  }

  void searchFeatureTable() {
    DataType type = cmbFilter.getValue();
    if (type == null) {
      return;
    }

    featureTable.getFilteredRowItems().setPredicate(item -> {
      ModularFeatureListRow row = item.getValue();
      String value = type.getFormattedString(row.get(type));
      String filter = txtSearch.getText().toLowerCase().trim();
      return value.contains(filter);
    });

    featureTable.getRoot().getChildren().clear();
    featureTable.getRoot().getChildren().addAll(featureTable.getFilteredRowItems());
  }


  void selectedRowChanged() {
    TreeItem<ModularFeatureListRow> selectedItem = featureTable.getSelectionModel()
        .getSelectedItem();
//    featureTable.getColumns().forEach(c -> logger.info(c.getText()));
    logger.info(
        "selected: " + featureTable.getSelectionModel().getSelectedCells().get(0).getTableColumn()
            .getText());

    if (selectedItem == null) {
      return;
    }

    ModularFeatureListRow selectedRow = selectedItem.getValue();
    if (selectedRow == null) {
      return;
    }

    updateXICPlot(selectedRow);
    updateSpectrumPlot(selectedRow);
  }
}
