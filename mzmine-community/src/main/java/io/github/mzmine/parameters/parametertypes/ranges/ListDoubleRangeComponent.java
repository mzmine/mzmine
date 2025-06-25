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

package io.github.mzmine.parameters.parametertypes.ranges;

import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

/**
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */

public class ListDoubleRangeComponent extends GridPane {

  private TextField inputField;
  private Label parsedInput;

  public ListDoubleRangeComponent() {
    inputField = new TextField();
    inputField.setPrefColumnCount(25);

    parsedInput = new Label();
    // textField.setColumns(8);

    add(inputField, 0, 0);
    add(parsedInput, 0, 1);

    inputField.textProperty().addListener((_, _, _) -> update());
  }

  public List<Range<Double>> getValue() {
    try {
      return toRanges(inputField.getText());
    } catch (Exception e) {
      return null;
    }
  }

  public void setValue(List<Range<Double>> ranges) {
    String text = fromRanges(ranges);

    // textField.setForeground(Color.black);
    parsedInput.setText(text);
    inputField.setText(text);
  }

  public void setToolTipText(String toolTip) {
    parsedInput.setTooltip(new Tooltip(toolTip));
    inputField.setTooltip(new Tooltip(toolTip));
  }


  private void update() {
    try {
      List<Range<Double>> ranges = toRanges(inputField.getText());

      // textField.setForeground(Color.black);
      parsedInput.setText(fromRanges(ranges));
    } catch (IllegalArgumentException e) {
//       textField.setForeground(Color.red);
      parsedInput.setText(e.getMessage());
    }
  }

  static List<Range<Double>> toRanges(String text) {
    final String filtered = text.replaceAll("[1234567890.,\\- ]", "");
    if (!filtered.isBlank()) {
      throw new IllegalArgumentException(
          "String contains illegal characters. only 1234567890.,- are allowed.");
    }
    List<Range<Double>> result = new ArrayList<>();
    if (text.isEmpty()) {
      return result;
    }
    try {
      String[] split = text.split(",");
      for (String rangeStr : split) {
        String[] doubleStrings = rangeStr.split("-");
        switch (doubleStrings.length) {
          case 1 -> {
            double value = Double.parseDouble(doubleStrings[0]);
            result.add(Range.singleton(value));
          }
          case 2 -> {
            result.add(Range.closed(Double.parseDouble(doubleStrings[0]),
                Double.parseDouble(doubleStrings[1])));
          }
        }
      }

      return result;
    } catch (Exception e) {
      throw new IllegalArgumentException("Error while parsing range string." + e.getMessage());
    }
  }

  static String fromRanges(List<Range<Double>> ranges) {
    StringBuilder result = new StringBuilder();

    for (Range<Double> range : ranges) {
      if (Double.compare(range.lowerEndpoint(), range.upperEndpoint()) == 0) {
        result.append(range.lowerEndpoint() + ",");
      } else {
        result.append(range.lowerEndpoint() + "-" + range.upperEndpoint() + ",");
      }
    }

    final String str = result.toString();
    if (str.isBlank()) {
      return str;
    }
    return result.substring(0, str.length() - 1);
  }
}
