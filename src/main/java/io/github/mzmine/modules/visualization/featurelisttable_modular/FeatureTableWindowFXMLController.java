package io.github.mzmine.modules.visualization.featurelisttable_modular;

import com.google.common.collect.Range;
import dulab.adap.datamodel.RawData;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.RawFileType;
import io.github.mzmine.datamodel.data.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.data.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.data.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.data.types.numbers.RTRangeType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.rawdataimport.RawDataFileType;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerWindow;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.SpectraPlotUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.ArrayList;
import java.util.Collection;
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
import javafx.scene.control.TreeTablePosition;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

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
  private ChoiceBox<Class<? extends DataType>> cmbFilter;

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

    spectrumPlot = new SpectraPlot();

    pnSpectrumXICSplit.getItems().addListener((ListChangeListener<? super Node>) change -> {
      if (change.getList().isEmpty()) {
        pnTablePreviewSplit.setDividerPosition(0, 1);
        pnPreview.setVisible(false);
        pnPreview.setPrefHeight(0.0);
        pnSpectrumXICSplit.setVisible(false);
        pnSpectrumXICSplit.setPrefHeight(0.0);
      } else {
        pnTablePreviewSplit.setDividerPosition(0, 0.65);
        pnPreview.setVisible(true);
        pnPreview.setPrefHeight(100);
        pnSpectrumXICSplit.setVisible(true);
        pnSpectrumXICSplit.setPrefHeight(100);
      }
    });

    featureTable.getSelectionModel().getSelectedCells().addListener(
        (ListChangeListener<? super TreeTablePosition<ModularFeatureListRow, ?>>) change -> {
          change.next();
          if (change.wasAdded()) {
            selectedRowChanged();
          }
        }
    );

    miShowXICOnAction(null);
    miShowSpectrumOnAction(null);
  }


  @FXML
  void miParametersOnAction(ActionEvent event) {
    Platform.runLater(() -> {
      ExitCode exitCode = param.showSetupDialog(true);

      if (exitCode == ExitCode.OK) {
        updateWindowToParameterSetValues();
      }
    });
  }

  @FXML
  void miShowXICOnAction(ActionEvent event) {
    if (event != null) {
      event.consume();
    }

    if (miShowXIC.isSelected()) {
      pnSpectrumXICSplit.getItems().add(xicPlot);
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
      pnSpectrumXICSplit.getItems().add(spectrumPlot);
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
    xicPlot.removeAllTICDataSets();
    // TODO: for now we take the first raw data file, we should take the one from the
    //  selected column, though.
    Set<RawDataFile> rawDataFileSet = selectedRow.getFeatures().getValue().keySet();
    List<RawDataFile> raws = new ArrayList<>();
    rawDataFileSet.forEach(r -> raws.add(r));
    ModularFeature feature = selectedRow.getFeatures().getValue().get(raws.get(0));
    TICDataSet dataSet = new TICDataSet(feature);
    xicPlot.addTICDataset(dataSet);
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
    Set<RawDataFile> rawDataFileSet = selectedRow.getFeatures().getValue().keySet();
    List<RawDataFile> raws = new ArrayList<>();
    rawDataFileSet.forEach(r -> raws.add(r));

    ModularFeature feature = selectedRow.getFeatures().getValue().get(raws.get(0));
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
      cmbFilter.getItems().add(dataType.getClass());
    }

    txtSearch.setOnKeyReleased(keyEvent -> {
      Class<? extends DataType> type = cmbFilter.getValue();
      if (type == null) {
        return;
      }

      featureTable.getFilteredRowItems().setPredicate(item -> {

        ModularFeatureListRow row = item.getValue();

        Object value = row.getValue(type);

//        DataType<?> dt = row.get(type); // does not work
//        row.getTypeColumn(type); // does not work
        // how to access DataType.getFormattedString?

        String lowValue = value.toString().toLowerCase(); // dt.getFormattedString(value);
        String filter = txtSearch.getText().toLowerCase().trim();

        return lowValue.contains(filter);
      });

      featureTable.getRoot().getChildren().clear();
      featureTable.getRoot().getChildren().addAll(featureTable.getFilteredRowItems());
    });
  }


  void selectedRowChanged() {
    TreeItem<ModularFeatureListRow> selectedItem = featureTable.getSelectionModel()
        .getSelectedItem();
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
