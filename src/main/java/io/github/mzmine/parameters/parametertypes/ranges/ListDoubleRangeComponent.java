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

import java.util.List;
import com.google.common.collect.Range;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */

public class ListDoubleRangeComponent extends GridPane {
  private TextField inputField;
  private Label textField;

  public ListDoubleRangeComponent() {
    inputField = new TextField();
    inputField.setPrefColumnCount(16);
    /*
     * inputField.getDocument().addDocumentListener(new DocumentListener() {
     * 
     * @Override public void changedUpdate(DocumentEvent e) { update(); }
     * 
     * @Override public void removeUpdate(DocumentEvent e) { update(); }
     * 
     * @Override public void insertUpdate(DocumentEvent e) { update(); } });
     */

    textField = new Label();
    // textField.setColumns(8);

    add(inputField, 0, 0);
    add(textField, 0, 1);
  }

  public List<Range<Double>> getValue() {
    try {
      return dulab.adap.common.algorithms.String.toRanges(textField.getText());
    } catch (Exception e) {
      return null;
    }
  }

  public void setValue(List<Range<Double>> ranges) {
    String text = dulab.adap.common.algorithms.String.fromRanges(ranges);

    // textField.setForeground(Color.black);
    textField.setText(text);
    inputField.setText(text);
  }

  public void setToolTipText(String toolTip) {
    textField.setTooltip(new Tooltip(toolTip));
    inputField.setTooltip(new Tooltip(toolTip));
  }



  private void update() {
    try {
      List<Range<Double>> ranges =
          dulab.adap.common.algorithms.String.toRanges(inputField.getText());

      // textField.setForeground(Color.black);
      textField.setText(dulab.adap.common.algorithms.String.fromRanges(ranges));
    } catch (IllegalArgumentException e) {
      // textField.setForeground(Color.red);
      textField.setText(e.getMessage());
    }
  }
}
