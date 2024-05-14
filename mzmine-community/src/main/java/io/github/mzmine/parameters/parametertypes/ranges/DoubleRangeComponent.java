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

package io.github.mzmine.parameters.parametertypes.ranges;

import com.google.common.collect.Range;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.logging.Logger;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.converter.NumberStringConverter;

public class DoubleRangeComponent extends HBox {

  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  protected final TextField minTxtField, maxTxtField;
  protected final Label minusLabel;

  protected NumberFormat format;
  protected NumberStringConverter formatConverter;

  public DoubleRangeComponent(NumberFormat format) {

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

  public Range<Double> getValue() {
    String minString = minTxtField.getText();
    String maxString = maxTxtField.getText();
    if (minString.isBlank() && maxString.isBlank()) {
      return null;
    }

    try {
      Number minValue = format.parse(minString.trim());
      Number maxValue = format.parse(maxString.trim());

      if ((minValue == null) || (maxValue == null)) {
        return null;
      }
      return Range.closed(minValue.doubleValue(), maxValue.doubleValue());

    } catch (Exception e) {
      logger.info(e.toString());
      return null;
    }
  }

  public void setValue(Range<Double> value) {
    if (value == null) {
      minTxtField.setText("");
      maxTxtField.setText("");
      return;
    }
    NumberFormat floorFormat = (NumberFormat) format.clone();
    floorFormat.setRoundingMode(RoundingMode.FLOOR);
    NumberFormat ceilFormat = (NumberFormat) format.clone();
    ceilFormat.setRoundingMode(RoundingMode.CEILING);
    minTxtField.setText(floorFormat.format(value.lowerEndpoint()));
    maxTxtField.setText(ceilFormat.format(value.upperEndpoint()));
  }

  public void setToolTipText(String toolTip) {
    minTxtField.setTooltip(new Tooltip(toolTip));
    maxTxtField.setTooltip(new Tooltip(toolTip));
  }

  /**
   * Listens to changes
   *
   * @param list
   */
  /*
   * public void addDocumentListener(DelayedDocumentListener list) {
   * minTxtField.getDocument().addDocumentListener(list);
   * maxTxtField.getDocument().addDocumentListener(list); }
   */
}
