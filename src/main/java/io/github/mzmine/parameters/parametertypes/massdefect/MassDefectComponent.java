/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.parameters.parametertypes.massdefect;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.logging.Logger;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.converter.NumberStringConverter;

public class MassDefectComponent extends HBox {

  private static final Logger logger = Logger.getLogger(MassDefectComponent.class.getName());

  private final TextField minTxtField, maxTxtField;
  private final Label minusLabel;

  private NumberFormat format;
  private NumberStringConverter formatConverter;

  public MassDefectComponent(NumberFormat format) {

    super.setSpacing(3.0);

    minTxtField = new TextField();
    minTxtField.setPrefColumnCount(8);
    // minTxtField.setMinWidth(100.0);

    maxTxtField = new TextField();
    maxTxtField.setPrefColumnCount(8);
    // maxTxtField.setMinWidth(100.0);

    minusLabel = new Label(" - ");
    minusLabel.setMinWidth(15.0);

    getChildren().addAll(minTxtField, minusLabel, maxTxtField);

    setMinWidth(200.0);
    // setStyle("-fx-border-color: red");

    setNumberFormat(format);
  }

  public TextField getMinTxtField() {
    return minTxtField;
  }

  public TextField getMaxTxtField() {
    return maxTxtField;
  }

  public void setNumberFormat(NumberFormat format) {
    this.format = format;
    this.formatConverter = new NumberStringConverter(format);
    minTxtField.setTextFormatter(new TextFormatter<Number>(formatConverter));
    maxTxtField.setTextFormatter(new TextFormatter<Number>(formatConverter));
  }

  public MassDefectFilter getValue() {
    String minString = minTxtField.getText();
    String maxString = maxTxtField.getText();

    try {
      Number minValue = format.parse(minString.trim());
      Number maxValue = format.parse(maxString.trim());

      if ((minValue == null) || (maxValue == null)) {
        return null;
      }

      return new MassDefectFilter(minValue.doubleValue(), maxValue.doubleValue());
    } catch (Exception e) {
      logger.info(e.toString());
      return null;
    }
  }

  public void setValue(MassDefectFilter massDefectFilter) {
    NumberFormat floorFormat = (NumberFormat) format.clone();
    floorFormat.setRoundingMode(RoundingMode.FLOOR);
    NumberFormat ceilFormat = (NumberFormat) format.clone();
    ceilFormat.setRoundingMode(RoundingMode.CEILING);
    minTxtField.setText(floorFormat.format(massDefectFilter.getLowerEndpoint()));
    maxTxtField.setText(ceilFormat.format(massDefectFilter.getUpperEndpoint()));
  }

  public void setToolTipText(String toolTip) {
    minTxtField.setTooltip(new Tooltip(toolTip));
    maxTxtField.setTooltip(new Tooltip(toolTip));
  }

}
