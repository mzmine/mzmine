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

package io.github.mzmine.util.dialogs;

import java.util.logging.Logger;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.NumberStringConverter;

public class AxesSetupDialog extends Stage {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final ValueAxis xAxis;
  private final ValueAxis yAxis;

  private ExitCode exitCode = ExitCode.UNKNOWN;

  private final DialogPane mainPane;
  private final Scene mainScene;

  // Buttons
  private final Button btnOK, btnApply, btnCancel;

  private final GridPane pnlLabelsAndFields;
  private final TextField fieldXMin;
  private final TextField fieldXMax;
  private final TextField fieldXTick;

  private final TextField fieldYMin;
  private final TextField fieldYMax;
  private final TextField fieldYTick;

  private final CheckBox checkXAutoRange, checkXAutoTick;
  private final CheckBox checkYAutoRange, checkYAutoTick;

  /**
   * Constructor
   */
  public AxesSetupDialog(@NotNull Window parent, @NotNull XYPlot plot) {

    assert parent != null;
    assert plot != null;

    initModality(Modality.APPLICATION_MODAL);

    mainPane = new DialogPane();

    mainScene = new Scene(mainPane);

    // Use main CSS
    mainScene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(mainScene);

    initOwner(parent);

    xAxis = plot.getDomainAxis();
    yAxis = plot.getRangeAxis();

    NumberStringConverter xConverter = new NumberStringConverter();
    NumberStringConverter yConverter = new NumberStringConverter();

    if (xAxis instanceof NumberAxis)
      xConverter = new NumberStringConverter(((NumberAxis) xAxis).getNumberFormatOverride());
    if (yAxis instanceof NumberAxis)
      yConverter = new NumberStringConverter(((NumberAxis) yAxis).getNumberFormatOverride());

    // Create labels and fields
    Label lblXTitle = new Label(xAxis.getLabel());
    lblXTitle.setStyle("-fx-font-size: 10pt; -fx-font-weight: bold;");
    lblXTitle.getStylesheets().clear();
    Label lblXAutoRange = new Label("Auto range");
    Label lblXMin = new Label("Minimum");
    Label lblXMax = new Label("Maximum");
    Label lblXAutoTick = new Label("Auto tick size");
    Label lblXTick = new Label("Tick size");

    Label lblYTitle = new Label(yAxis.getLabel());
    lblYTitle.setStyle("-fx-font-size: 10pt; -fx-font-weight: bold;");
    Label lblYAutoRange = new Label("Auto range");
    Label lblYMin = new Label("Minimum");
    Label lblYMax = new Label("Maximum");
    Label lblYAutoTick = new Label("Auto tick size");
    Label lblYTick = new Label("Tick size");


    checkXAutoRange = new CheckBox();
    checkXAutoTick = new CheckBox();
    fieldXMin = new TextField();
    fieldXMin.setTextFormatter(new TextFormatter<>(xConverter));
    fieldXMax = new TextField();
    fieldXMax.setTextFormatter(new TextFormatter<>(xConverter));
    fieldXTick = new TextField();
    fieldXTick.setTextFormatter(new TextFormatter<>(xConverter));

    fieldXMin.disableProperty().bind(checkXAutoRange.selectedProperty());
    fieldXMax.disableProperty().bind(checkXAutoRange.selectedProperty());
    fieldXTick.disableProperty().bind(checkXAutoTick.selectedProperty());


    checkYAutoRange = new CheckBox();
    checkYAutoTick = new CheckBox();
    fieldYMin = new TextField();
    fieldYMin.setTextFormatter(new TextFormatter<>(yConverter));
    fieldYMax = new TextField();
    fieldYMax.setTextFormatter(new TextFormatter<>(yConverter));
    fieldYTick = new TextField();
    fieldYTick.setTextFormatter(new TextFormatter<>(yConverter));

    fieldYMin.disableProperty().bind(checkYAutoRange.selectedProperty());
    fieldYMax.disableProperty().bind(checkYAutoRange.selectedProperty());
    fieldYTick.disableProperty().bind(checkYAutoTick.selectedProperty());

    // Create a panel for labels and fields
    pnlLabelsAndFields = new GridPane();
    pnlLabelsAndFields.setHgap(5);
    pnlLabelsAndFields.setVgap(10);
    int row = 0;
    pnlLabelsAndFields.add(lblXTitle, 0, row);

    row++;
    pnlLabelsAndFields.add(lblXAutoRange, 0, row);
    pnlLabelsAndFields.add(checkXAutoRange, 1, row);

    row++;
    pnlLabelsAndFields.add(lblXMin, 0, row);
    pnlLabelsAndFields.add(fieldXMin, 1, row);

    row++;
    pnlLabelsAndFields.add(lblXMax, 0, row);
    pnlLabelsAndFields.add(fieldXMax, 1, row);
    if (xAxis instanceof NumberAxis) {
      row++;
      pnlLabelsAndFields.add(lblXAutoTick, 0, row);
      pnlLabelsAndFields.add(checkXAutoTick, 1, row);

      row++;
      pnlLabelsAndFields.add(lblXTick, 0, row);
      pnlLabelsAndFields.add(fieldXTick, 1, row);
    }

    // One empty row
    row++;
    row++;
    pnlLabelsAndFields.add(lblYTitle, 0, row);

    row++;
    pnlLabelsAndFields.add(lblYAutoRange, 0, row);
    pnlLabelsAndFields.add(checkYAutoRange, 1, row);

    row++;
    pnlLabelsAndFields.add(lblYMin, 0, row);
    pnlLabelsAndFields.add(fieldYMin, 1, row);

    row++;
    pnlLabelsAndFields.add(lblYMax, 0, row);
    pnlLabelsAndFields.add(fieldYMax, 1, row);

    if (yAxis instanceof NumberAxis) {
      row++;
      pnlLabelsAndFields.add(lblYAutoTick, 0, row);
      pnlLabelsAndFields.add(checkYAutoTick, 1, row);

      row++;
      pnlLabelsAndFields.add(lblYTick, 0, row);
      pnlLabelsAndFields.add(fieldYTick, 1, row);
    }

    // Create buttons
    mainPane.getButtonTypes().add(ButtonType.OK);
    mainPane.getButtonTypes().add(ButtonType.APPLY);
    mainPane.getButtonTypes().add(ButtonType.CANCEL);

    btnOK = (Button) mainPane.lookupButton(ButtonType.OK);
    btnOK.setOnAction(e -> {
      if (setValuesToPlot())
        hide();
    });
    btnApply = (Button) mainPane.lookupButton(ButtonType.APPLY);
    btnApply.setOnAction(e -> {
      setValuesToPlot();
    });
    btnCancel = (Button) mainPane.lookupButton(ButtonType.CANCEL);
    btnCancel.setOnAction(e -> hide());

    mainPane.setContent(pnlLabelsAndFields);

    sizeToScene();
    centerOnScreen();

    setTitle("Please set ranges for axes");
    setResizable(false);

    loadValuesToControls();

  }



  private void loadValuesToControls() {

    checkXAutoRange.setSelected(xAxis.isAutoRange());
    fieldXMin.setText(String.valueOf(xAxis.getRange().getLowerBound()));
    fieldXMax.setText(String.valueOf(xAxis.getRange().getUpperBound()));
    if (xAxis instanceof NumberAxis) {
      checkXAutoTick.setSelected(xAxis.isAutoTickUnitSelection());
      fieldXTick.setText(String.valueOf(((NumberAxis) xAxis).getTickUnit().getSize()));
    }

    checkYAutoRange.setSelected(yAxis.isAutoRange());
    fieldYMin.setText(String.valueOf(yAxis.getRange().getLowerBound()));
    fieldYMax.setText(String.valueOf(yAxis.getRange().getUpperBound()));
    if (yAxis instanceof NumberAxis) {
      checkYAutoTick.setSelected(yAxis.isAutoTickUnitSelection());
      fieldYTick.setText(String.valueOf(((NumberAxis) yAxis).getTickUnit().getSize()));
    }

  }



  private boolean setValuesToPlot() {
    if (checkXAutoRange.isSelected()) {
      xAxis.setAutoRange(true);
    } else {
      try {
        double lower = Double.parseDouble(fieldXMin.getText());
        double upper = Double.parseDouble(fieldXMax.getText());
        if (lower > upper) {
          displayMessage("Invalid " + xAxis.getLabel() + " range.");
          return false;
        }
        xAxis.setRange(lower, upper);
      } catch (NumberFormatException e) {
        displayMessage("Could not parse number " + e);
        return false;
      }
    }

    if (xAxis instanceof NumberAxis) {
      if (checkXAutoTick.isSelected()) {
        xAxis.setAutoTickUnitSelection(true);
      } else {
        try {
          double tickSize = Double.valueOf(fieldXTick.getText());
          ((NumberAxis) xAxis).setTickUnit(new NumberTickUnit(tickSize));
        } catch (NumberFormatException e) {
          displayMessage("Could not parse number " + e);
          return false;
        }
      }

    }

    if (checkYAutoRange.isSelected()) {
      yAxis.setAutoRange(true);
    } else {
      try {
        double lower = Double.parseDouble(fieldYMin.getText());
        double upper = Double.parseDouble(fieldYMax.getText());
        if (lower > upper) {
          displayMessage("Invalid " + yAxis.getLabel() + " range.");
          return false;
        }
        yAxis.setRange(lower, upper);
      } catch (NumberFormatException e) {
        displayMessage("Could not parse number " + e);
        return false;
      }

    }

    if (yAxis instanceof NumberAxis) {
      if (checkYAutoTick.isSelected()) {
        yAxis.setAutoTickUnitSelection(true);
      } else {
        try {
          double tickSize = Double.parseDouble(fieldYTick.getText());
          ((NumberAxis) yAxis).setTickUnit(new NumberTickUnit(tickSize));
        } catch (NumberFormatException e) {
          displayMessage("Could not parse number " + e);
          return false;
        }
      }
    }

    return true;
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
   * Method for reading exit code
   *
   */
  public ExitCode getExitCode() {
    return exitCode;
  }

}

