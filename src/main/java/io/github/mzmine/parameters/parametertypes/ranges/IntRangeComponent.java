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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

public class IntRangeComponent extends GridPane {

  private TextField minTxtField, maxTxtField;

  public IntRangeComponent() {

    // setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

    minTxtField = new TextField();
    minTxtField.setPrefColumnCount(8);

    maxTxtField = new TextField();
    maxTxtField.setPrefColumnCount(8);


    add(minTxtField, 0, 0);
    add(new Label(" - "), 1, 0);
    add(maxTxtField, 2, 0);
  }

  public Range<Integer> getValue() {
    String minString = minTxtField.getText();
    String maxString = maxTxtField.getText();

    try {
      if (!minString.isEmpty() && !maxString.isEmpty()) {
        return Range.closed(Integer.parseInt(minString), Integer.parseInt(maxString));
      } else if (!minString.isEmpty()) {
        return Range.closed(Integer.parseInt(minString), Integer.MAX_VALUE);
      } else if (!maxString.isEmpty()) {
        return Range.closed(0, Integer.parseInt(maxString));
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  public void setValue(Range<Integer> value) {
    minTxtField.setText(String.valueOf(value.lowerEndpoint()));
    maxTxtField.setText(String.valueOf(value.upperEndpoint()));
  }

  public void setToolTipText(String toolTip) {
    minTxtField.setTooltip(new Tooltip(toolTip));
    maxTxtField.setTooltip(new Tooltip(toolTip));
  }


}
