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

package io.github.mzmine.modules.visualization.neutralloss;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.general.DatasetChangeEvent;
import com.google.common.collect.Range;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.main.MZmineCore;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.NumberStringConverter;

/**
 * Dialog for selection of highlighted precursor m/z range
 */
public class NeutralLossSetHighlightDialog extends Stage {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  static final int PADDING_SIZE = 5;

  // dialog components
  private final DialogPane mainPane;
  private final Scene mainScene;
  private final Button btnOK, btnCancel;
  private final TextField fieldMinMZ, fieldMaxMZ;
  private final GridPane pnlLabelsAndFields;

  private Desktop desktop;

  private String rangeType;
  private ValueAxis axis;

  private NeutralLossPlot plot;

  public NeutralLossSetHighlightDialog(@Nonnull Stage parent, @Nonnull NeutralLossPlot plot,
      @Nonnull String command) {

    assert parent != null;
    assert plot != null;
    assert command != null;

    desktop = MZmineCore.getDesktop();
    this.plot = plot;
    rangeType = command;
    mainPane = new DialogPane();
    mainScene = new Scene(mainPane);

    // Use main CSS
    mainScene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(mainScene);

    initOwner(parent);

    String title = "Highlight ";
    if (command.equals("HIGHLIGHT_PRECURSOR")) {
      title += "precursor m/z range";
      axis = plot.getXYPlot().getDomainAxis();
    } else if (command.equals("HIGHLIGHT_NEUTRALLOSS")) {
      title += "neutral loss m/z range";
      axis = plot.getXYPlot().getRangeAxis();
    }
    setTitle(title);

    Label lblMinMZ = new Label("Minimum m/z");
    Label lblMaxMZ = new Label("Maximum m/z");

    NumberStringConverter converter = new NumberStringConverter();

    fieldMinMZ = new TextField();
    fieldMinMZ.setTextFormatter(new TextFormatter<>(converter));
    fieldMaxMZ = new TextField();
    fieldMaxMZ.setTextFormatter(new TextFormatter<>(converter));

    pnlLabelsAndFields = new GridPane();
    pnlLabelsAndFields.setHgap(5);
    pnlLabelsAndFields.setVgap(10);

    int row = 0;
    pnlLabelsAndFields.add(lblMinMZ, 0, row);
    pnlLabelsAndFields.add(fieldMinMZ, 1, row);

    row++;
    pnlLabelsAndFields.add(lblMaxMZ, 0, row);
    pnlLabelsAndFields.add(fieldMaxMZ, 1, row);

    // Create buttons
    mainPane.getButtonTypes().add(ButtonType.OK);
    mainPane.getButtonTypes().add(ButtonType.CANCEL);

    btnOK = (Button) mainPane.lookupButton(ButtonType.OK);
    btnOK.setOnAction(e -> {
      if (highlightDataPoints())
        hide();
    });
    btnCancel = (Button) mainPane.lookupButton(ButtonType.CANCEL);
    btnCancel.setOnAction(e -> hide());

    mainPane.setContent(pnlLabelsAndFields);

    sizeToScene();
    centerOnScreen();
    setResizable(false);

  }

  public boolean highlightDataPoints() {


    try {
      double lower = Double.parseDouble(fieldMinMZ.getText());
      double upper = Double.parseDouble(fieldMaxMZ.getText());
      if (lower > upper) {
        displayMessage("Invalid " + axis.getLabel() + " range.");
        return false;
      }
      Range<Double> range = Range.closed(lower, upper);
      if (rangeType.equals("HIGHLIGHT_PRECURSOR"))
        plot.setHighlightedPrecursorRange(range);
      else if (rangeType.equals("HIGHLIGHT_NEUTRALLOSS"))
        plot.setHighlightedNeutralLossRange(range);
      logger.info("Updating Neutral loss plot window");

      NeutralLossDataSet dataSet = (NeutralLossDataSet) plot.getXYPlot().getDataset();
      dataSet.updateOnRangeDataPoints(rangeType);
      plot.getXYPlot().datasetChanged(new DatasetChangeEvent(plot, dataSet));
      return true;
    } catch (NumberFormatException e) {
      displayMessage("Could not parse number " + e);
      return false;
    } catch (IllegalArgumentException iae) {
      desktop.displayErrorMessage(iae.getMessage());
      return false;
    } catch (Exception e) {
      logger.log(Level.FINE, "Error while setting highlighted range", e);
      return false;
    }
  }

  private void displayMessage(String msg) {
    logger.info(msg);
    final Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.initStyle(StageStyle.UTILITY);
    alert.setTitle("Information");
    alert.setHeaderText("Error");
    alert.setContentText(msg);
    alert.showAndWait();
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  /*
   * public void actionPerformed(ActionEvent ae) {
   * 
   * Object src = ae.getSource();
   * 
   * if (src == btnOK) {
   * 
   * try {
   * 
   * if ((fieldMinMZ.getText() == null) || (fieldMinMZ.getText() == null)) {
   * desktop.displayErrorMessage("Invalid bounds"); return; }
   * 
   * double mzMin = (Double.parseDouble(fieldMinMZ.getText())); double mzMax =
   * (Double.parseDouble(fieldMaxMZ.getText()));
   * 
   * Range<Double> range = Range.closed(mzMin, mzMax); if (rangeType.equals("HIGHLIGHT_PRECURSOR"))
   * plot.setHighlightedPrecursorRange(range); else if (rangeType.equals("HIGHLIGHT_NEUTRALLOSS"))
   * plot.setHighlightedNeutralLossRange(range); logger.info("Updating Neutral loss plot window");
   * 
   * NeutralLossDataSet dataSet = (NeutralLossDataSet) plot.getXYPlot().getDataset();
   * dataSet.updateOnRangeDataPoints(rangeType); plot.getXYPlot().datasetChanged(new
   * DatasetChangeEvent(plot, dataSet)); hide();
   * 
   * } catch (IllegalArgumentException iae) { desktop.displayErrorMessage(iae.getMessage()); } catch
   * (Exception e) { logger.log(Level.FINE, "Error while setting highlighted range", e); } }
   * 
   * if (src == btnCancel) { hide(); }
   * 
   * }
   */
}
