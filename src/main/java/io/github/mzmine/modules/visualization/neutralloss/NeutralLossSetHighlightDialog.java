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

import com.google.common.collect.Range;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import org.jfree.data.general.DatasetChangeEvent;

/**
 * Dialog for selection of highlighted precursor m/z range
 */
public class NeutralLossSetHighlightDialog extends Dialog {

  /**
   *
   */

  private Logger logger = Logger.getLogger(this.getClass().getName());

  static final int PADDING_SIZE = 5;

  // dialog components
  private Button btnOK, btnCancel;
  private TextField fieldMinMZ, fieldMaxMZ;

  private Desktop desktop;

  private String rangeType;

  private NeutralLossPlot plot;

  public NeutralLossSetHighlightDialog(NeutralLossPlot plot, String command) {

    // Make dialog modal
    initModality(Modality.APPLICATION_MODAL);

    this.desktop = MZmineCore.getDesktop();
    this.plot = plot;
    this.rangeType = command;

    String title = "Highlight ";
    if (command.equals("HIGHLIGHT_PRECURSOR")) {
      title += "precursor m/z range";
    } else if (command.equals("HIGHLIGHT_NEUTRALLOSS")) {
      title += "neutral loss m/z range";
    }
    setTitle(title);

    Node comp;

    GridPane components = new GridPane();
    components.setHgap(5);
    components.setVgap(10);
    NumberFormat format = NumberFormat.getNumberInstance();

    comp = new Label("Minimum m/z");
    components.add(comp, 0, 0, 1, 1);

    fieldMinMZ = new TextField();
    fieldMinMZ.setTextFormatter(new TextFormatter<>(new NumberStringConverter(format)));
    fieldMinMZ.setPrefWidth(50);
    components.add(fieldMinMZ, 1, 0, 1, 1);

    comp = new Label("Maximum m/z");
    components.add(comp, 0, 1, 1, 1);

    fieldMaxMZ = new TextField();
    fieldMaxMZ.setTextFormatter(new TextFormatter<>(new NumberStringConverter(format)));
    fieldMaxMZ.setPrefWidth(50);
    components.add(fieldMaxMZ, 1, 1, 1, 1);

    comp = new Separator();
    components.add(comp, 0, 2, 3, 1);

    ButtonBar buttonsPanel = new ButtonBar();
    btnOK = new Button("OK");
    btnCancel = new Button("Cancel");
    btnOK.setOnAction(this::actionPerformed);
    btnCancel.setOnAction(this::actionPerformed);

    components.add(buttonsPanel, 0, 3, 3, 1);
    this.getDialogPane().setContent(components);
    setResizable(false);
  }


  public void actionPerformed(ActionEvent ae) {

    Object src = ae.getSource();

    if (src == btnOK) {

      try {

        if ((fieldMinMZ.getText() == null) || (fieldMinMZ.getText() == null)) {
          desktop.displayErrorMessage("Invalid bounds");
          return;
        }

        double mzMin = Double.parseDouble(fieldMinMZ.getText());
        double mzMax = Double.parseDouble(fieldMaxMZ.getText());

        Range<Double> range = Range.closed(mzMin, mzMax);
        if (rangeType.equals("HIGHLIGHT_PRECURSOR")) {
          plot.setHighlightedPrecursorRange(range);
        } else if (rangeType.equals("HIGHLIGHT_NEUTRALLOSS")) {
          plot.setHighlightedNeutralLossRange(range);
        }
        logger.info("Updating Neutral loss plot window");

        NeutralLossDataSet dataSet = (NeutralLossDataSet) plot.getXYPlot().getDataset();
        dataSet.updateOnRangeDataPoints(rangeType);
        plot.getXYPlot().datasetChanged(new DatasetChangeEvent(plot, dataSet));

        Stage s = (Stage) (this.btnOK.getScene().getWindow());
        s.close();
      } catch (IllegalArgumentException iae) {
        desktop.displayErrorMessage(iae.getMessage());
      } catch (Exception e) {
        logger.log(Level.FINE, "Error while setting highlighted range", e);
      }
    }

    if (src == btnCancel) {
      Stage s = (Stage) (this.btnCancel.getScene().getWindow());
      s.close();
    }

  }
}
