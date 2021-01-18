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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.FeatureDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

/**
 * This class extends ParameterSetupDialog class.
 */
public class PeakResolverSetupDialog extends ParameterSetupDialog {

  // Logger.
  private static final Logger logger = Logger.getLogger(PeakResolverSetupDialog.class.getName());

  // Combo-box font.
  private static final Font COMBO_FONT = new Font("SansSerif", Font.PLAIN, 10);

  // Maximum peak count.
  private static final int MAX_PEAKS = 100; // 30

  // TIC minimum size.
  private static final Dimension MINIMUM_TIC_DIMENSIONS = new Dimension(400, 300);

  // Preferred width of peak combo-box
  private static final int PREFERRED_PEAK_COMBO_WIDTH = 250;

  // Dialog components.
  private BorderPane pnlPlotXY;
  private BorderPane pnlVisible;
  private GridPane pnlLabelsFields;
  private BorderPane previewPanel;
  private ComboBox<FeatureList> comboPeakList;
  private ComboBox<FeatureListRow> comboPeak;
  private CheckBox preview;

  private TICPlot ticPlot;

  private PeakResolver peakResolver;
  private final ParameterSet parameters;

  /**
   * Create the dialog.
   *
   * @param resolverParameters resolver parameters.
   * @param resolverClass      resolver class.
   */
  public PeakResolverSetupDialog(boolean valueCheckRequired, final ParameterSet resolverParameters,
      final Class<? extends PeakResolver> resolverClass) {

    this(valueCheckRequired, resolverParameters, resolverClass, null);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message: html-formatted text
   */
  public PeakResolverSetupDialog(boolean valueCheckRequired, final ParameterSet resolverParameters,
      final Class<? extends PeakResolver> resolverClass, String message) {

    super(valueCheckRequired, resolverParameters, message);

    // Instantiate resolver.
    try {

      peakResolver = resolverClass.getDeclaredConstructor().newInstance();
    } catch (Throwable t) {

      logger.log(Level.SEVERE, "Peak deconvolution error", t);
      MZmineCore.getDesktop()
          .displayErrorMessage("Couldn't create peak resolver (" + t.getMessage() + ')');
    }

    parameters = resolverParameters;

    final FeatureList[] peakLists = MZmineCore.getProjectManager().getCurrentProject()
        .getFeatureLists().toArray(new FeatureList[0]);

    // Elements of panel.
    preview = new CheckBox("Show preview");
    preview.setOnAction(e -> {
      if (preview.isSelected()) {
        // Set the height of the preview to 200 cells, so it will span
        // the whole vertical length of the dialog (buttons are at row
        // no 100). Also, we set the weight to 10, so the preview
        // component will consume most of the extra available space.
        paramsPane.add(pnlPlotXY, 3, 0, 1, getNumberOfParameters() + 2);
        pnlVisible.setCenter(pnlLabelsFields);
        // Set selections.
        final FeatureList[] selected = MZmineCore.getDesktop().getSelectedPeakLists();
        if (selected.length > 0) {
          comboPeakList.getSelectionModel().select(selected[0]);
        } else {
          comboPeakList.getSelectionModel().select(0);
        }
      } else {
        paramsPane.getChildren().remove(pnlPlotXY);
        pnlVisible.getChildren().remove(pnlLabelsFields);
      }
      mainPane.getScene().getWindow().sizeToScene();
    });
    preview.setDisable(peakLists.length == 0);

    // Preview panel.
    previewPanel = new BorderPane();
    previewPanel.setTop(new Separator());
    previewPanel.setCenter(preview);
    // previewPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

    // Feature list combo-box.
    comboPeakList = new ComboBox<FeatureList>();
    // comboPeakList.setFont(COMBO_FONT);
    for (final FeatureList peakList : peakLists) {
      if (peakList.getNumberOfRawDataFiles() == 1) {
        comboPeakList.getItems().add(peakList);
      }
    }
    comboPeakList.valueProperty().addListener((obs, old, newVal) -> {
      // Remove current peaks (suspend listener).
      if (newVal == null) {
        return;
      }
      ObservableList<FeatureListRow> newItems = FXCollections
          .observableArrayList(newVal.getRows());
      comboPeak.setItems(newItems);
      if (newItems.size() > 0) {
        comboPeak.getSelectionModel().select(0);
      }
    });

    // Peaks combo box.
    comboPeak = new ComboBox<FeatureListRow>();
    comboPeak.setButtonCell(new ListCell<>() {
      @Override
      protected void updateItem(FeatureListRow item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setGraphic(null);
        } else {
          setGraphic(new PeakPreviewComboRenderer(item));
        }
      }
    });
    comboPeak.setCellFactory(p -> {
      return new ListCell<FeatureListRow>() {
        @Override
        protected void updateItem(FeatureListRow item, boolean empty) {
          super.updateItem(item, empty);
          if (item == null || empty) {
            setGraphic(null);
          } else {
            setGraphic(new PeakPreviewComboRenderer(item));
          }
        }
      };
    });
    comboPeak.valueProperty().addListener(((observable, oldValue, newValue) -> {
      parametersChanged();
    }));

    // comboPeak.setPreferredSize(
    // new Dimension(PREFERRED_PEAK_COMBO_WIDTH, comboPeak.getPreferredSize().height));

    pnlLabelsFields = new GridPane();
    pnlLabelsFields.add(new Label("Feature list"), 0, 0);
    pnlLabelsFields.add(comboPeakList, 0, 1);
    pnlLabelsFields.add(new Label("Chromatogram"), 1, 0);
    pnlLabelsFields.add(comboPeak, 1, 1);

    // Put all together.
    pnlVisible = new BorderPane();
    pnlVisible.setTop(previewPanel);

    // TIC plot.
    ticPlot = new TICPlot();
    ticPlot.setMinSize(400, 300);
    // ticPlot.setMinimumSize(MINIMUM_TIC_DIMENSIONS);

    // Tool bar.
    // final TICToolBar toolBar = new TICToolBar(ticPlot);
    // toolBar.getComponentAtIndex(0).setVisible(false);

    // Panel for XYPlot.
    pnlPlotXY = new BorderPane();
    // pnlPlotXY.setBackground(Color.white);
    pnlPlotXY.setCenter(ticPlot);
    // pnlPlotXY.setRight(toolBar);
    // GUIUtils.addMarginAndBorder(pnlPlotXY, 10);

    paramsPane.add(pnlVisible, 0, getNumberOfParameters() + 3, 4, 1);
  }


  @Override
  public void parametersChanged() {

    if (preview != null && preview.isSelected()) {

      final FeatureListRow previewRow = comboPeak.getSelectionModel().getSelectedItem();
      if (previewRow != null) {
        // Load the intensities and RTs into array.
        final Feature previewPeak = previewRow.getFeatures().get(0);
        logger.finest("Loading new preview peak " + previewRow);

        ticPlot.removeAllDataSets();
        ticPlot.addDataSet(new ChromatogramTICDataSet(previewPeak));

        // Auto-range to axes.
        ticPlot.getXYPlot().getDomainAxis().setAutoRange(true);
        ticPlot.getXYPlot().getDomainAxis().setAutoTickUnitSelection(true);
        ticPlot.getXYPlot().getRangeAxis().setAutoRange(true);
        ticPlot.getXYPlot().getRangeAxis().setAutoTickUnitSelection(true);

        updateParameterSetFromComponents();

        // If there is some illegal value, do not load the preview but
        // just exit.
        ArrayList<String> errors = new ArrayList<String>();
        if (!parameterSet.checkParameterValues(errors)) {
          logger.fine("Illegal parameter value: " + errors);
          return;
        }

        // Resolve peaks.
        ResolvedPeak[] resolvedPeaks = {};
        RSessionWrapper rSession;
        try {

          if (peakResolver.getRequiresR()) {
            // Check R availability, by trying to open the
            // connection.
            String[] reqPackages = peakResolver.getRequiredRPackages();
            String[] reqPackagesVersions = peakResolver.getRequiredRPackagesVersions();
            String callerFeatureName = peakResolver.getName();
            REngineType rEngineType = peakResolver.getREngineType(parameters);
            rSession = new RSessionWrapper(rEngineType, callerFeatureName, reqPackages,
                reqPackagesVersions);
            rSession.open();
          } else {
            rSession = null;
          }
          CenterFunction mzCenterFunction = new CenterFunction(CenterMeasure.MEDIAN);
          // preview doesn't show msms scans
          // set it to be default searching range
          resolvedPeaks =
              peakResolver.resolvePeaks(previewPeak, parameters, rSession, mzCenterFunction, 0, 0);

          // Turn off R instance.
          if (rSession != null) {
            rSession.close(false);
          }

        } catch (RSessionWrapperException e) {
          throw new IllegalStateException(e.getMessage());
        } catch (Throwable t) {
          t.printStackTrace();
          logger.log(Level.SEVERE, "Peak deconvolution error", t);
          MZmineCore.getDesktop().displayErrorMessage(t.toString());
        }

        // Add resolved peaks to TIC plot.
        final int peakCount = Math.min(MAX_PEAKS, resolvedPeaks.length);
        for (int i = 0; i < peakCount; i++) {

          final FeatureDataSet featureDataSet
              = new FeatureDataSet(FeatureConvertors.ResolvedPeakToMoularFeature(
              (ModularFeatureList) resolvedPeaks[i].getPeakList(), resolvedPeaks[i],
              ((ModularFeature) previewPeak).getFeatureData()));
          ticPlot.addFeatureDataSet(featureDataSet);
        }

        // Check peak count.
        if (resolvedPeaks.length > MAX_PEAKS) {
          // MZmineCore.getDesktop().displayMessage(null,
          // "Too many peaks detected, please adjust parameter
          // values");
          MZmineCore.getDesktop().displayMessage(null,
              "Too many peaks detected. Not all of the peaks might be displayed");
        }
      }

    }
  }


}
