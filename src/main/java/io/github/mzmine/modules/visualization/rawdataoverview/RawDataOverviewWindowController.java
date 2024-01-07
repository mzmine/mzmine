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

package io.github.mzmine.modules.visualization.rawdataoverview;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.visualization.chromatogramandspectra.ChromatogramAndSpectraVisualizer;
import io.github.mzmine.project.impl.ImagingRawDataFileImpl;
import io.github.mzmine.util.DialogLoggerUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

/*
 * Raw data overview window controller class
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class RawDataOverviewWindowController {

  public static final Logger logger = Logger.getLogger(
      RawDataOverviewWindowController.class.getName());

  private boolean initialized = false;

  private final ObservableMap<RawDataFile, RawDataFileInfoPaneController> rawDataFilesAndControllers = FXCollections.observableMap(
      new HashMap<>());
  private final ObservableMap<RawDataFile, Tab> rawDataFilesAndTabs = FXCollections.observableMap(
      new HashMap<>());


  @FXML
  private ChromatogramAndSpectraVisualizer visualizer;

  @FXML
  private TabPane tpRawDataInfo;

  @FXML
  private BorderPane pnMaster;

  @FXML
  private SplitPane pnMain;

  public void initialize() {

    addChromatogramSelectedScanListener();

    initialized = true;
  }

  /**
   * Sets the raw data files to be displayed. Already present files are not removed to optimise
   * performance. This should be called over
   * {@link RawDataOverviewWindowController#addRawDataFileTab} if possible.
   * <p>
   * Only add LC-MS data sets, exclude imaging
   *
   * @param rawDataFiles
   */
  public void setRawDataFiles(Collection<RawDataFile> rawDataFiles) {
    if(rawDataFiles.size()>25) {
      boolean result = DialogLoggerUtil.showDialogYesNo("Raw data overview",
          "Visualizing %d data files at once might slow down MZmine, continue?".formatted(
              rawDataFiles.size()));

      if(!result) {
        // just visualize the first file if user selected false
        rawDataFiles = rawDataFiles.stream().findFirst().map(List::of).orElse(List.of());
      }
    }

    // remove files first
    List<RawDataFile> filesToProcess = new ArrayList<>();
    for (RawDataFile rawDataFile : rawDataFilesAndTabs.keySet()) {
      if (!rawDataFiles.contains(rawDataFile)) {
        filesToProcess.add(rawDataFile);
      }
    }
    filesToProcess.forEach(this::removeRawDataFile);

    // presence of file is checked in the add method
    rawDataFiles.forEach(r -> {
      if (!(r instanceof ImagingRawDataFileImpl)) {
        addRawDataFileTab(r);
      }
    });
    visualizer.setRawDataFiles(rawDataFiles);
  }

  /**
   * Adds a raw data file table to the tab.
   *
   * @param raw The raw dataFile
   */
  public void addRawDataFileTab(RawDataFile raw) {

    if (!initialized) {
      initialize();
    }
    if (rawDataFilesAndControllers.containsKey(raw)) {
      return;
    }

    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("RawDataFileInfoPane.fxml"));
      BorderPane pane = loader.load();
      rawDataFilesAndControllers.put(raw, loader.getController());
      RawDataFileInfoPaneController con = rawDataFilesAndControllers.get(raw);
      con.getRawDataTableView().getSelectionModel().selectedItemProperty()
          // TODO: this clears the spectrum plot, somehow bind to mouse input, currenty it is just
          // slower than the thread
          .addListener(((obs, old, newValue) -> {
            if (newValue == null) {
              // this is the case it the table was not populated before.
              // in that case we just select the table.
              return;
            }
            visualizer.setFocusedScan(raw, newValue);
          }));

      Tab rawDataFileTab = new Tab(raw.getName());
      rawDataFileTab.setContent(pane);
      tpRawDataInfo.getTabs().add(rawDataFileTab);

      rawDataFileTab.selectedProperty().addListener((obs, o, n) -> {
        if (n) {
          con.populate(raw);
        }
      });

      rawDataFileTab.setOnClosed((e) -> {
        logger.fine("Removing raw data file " + raw.getName());
        removeRawDataFile(raw);
      });

      if (rawDataFileTab.selectedProperty().getValue()) {
        con.populate(raw);
      }

      rawDataFilesAndTabs.put(raw, rawDataFileTab);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not load RawDataFileInfoPane.fxml", e);
    }

    logger.fine("Added raw data file tab for " + raw.getName());
  }

  public void removeRawDataFile(RawDataFile raw) {
    visualizer.removeRawDataFile(raw);
    rawDataFilesAndControllers.remove(raw);
    Tab tab = rawDataFilesAndTabs.remove(raw);
    tpRawDataInfo.getTabs().remove(tab);
  }

  // plot update methods

  /**
   * Updates the selected row in the raw data table if the user clicks in the chromatogram plot.
   */
  private void addChromatogramSelectedScanListener() {

    visualizer.chromPositionProperty().addListener((observable, oldValue, pos) -> {
      RawDataFile selectedRawDataFile = pos.getDataFile();
      if (selectedRawDataFile == null || selectedRawDataFile instanceof ImagingRawDataFileImpl) {
        return;
      }
      RawDataFileInfoPaneController con = rawDataFilesAndControllers.get(selectedRawDataFile);
      if (con == null) {
        logger.info("Cannot find controller for raw data file " + selectedRawDataFile.getName());
        return;
      }

      TableView<Scan> rawDataTableView = con.getRawDataTableView();
      tpRawDataInfo.getSelectionModel().select(rawDataFilesAndTabs.get(selectedRawDataFile));

      if (rawDataTableView.getItems() != null) {
        try {
          Scan scan = pos.getScan();
          rawDataTableView.getItems().stream().filter(item -> item.equals(scan)).findFirst()
              .ifPresent(item -> {
                rawDataTableView.getSelectionModel().select(item);
                rawDataTableView.getSelectionModel().focus(rawDataTableView.getItems().indexOf(item));
                if (!con.getVisibleRange().contains(rawDataTableView.getItems().indexOf(item))) {
                  rawDataTableView.scrollTo(item);
                }
              });
        } catch (Exception e) {
          e.getStackTrace();
        }
      }
    });

  }

  public RawDataFile getSelectedRawDataFile() {
    return visualizer.getSelectedRawDataFile();
  }

  @NotNull
  public Collection<RawDataFile> getRawDataFiles() {
    return visualizer.getRawDataFiles();
  }

}
