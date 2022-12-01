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

package io.github.mzmine.parameters.parametertypes.absoluterelative;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

public class AbsoluteNRelativeDoubleComponent extends FlowPane {

  private static final long serialVersionUID = 1L;
  private final TextField absField, relField;

  public AbsoluteNRelativeDoubleComponent() {
    absField = new TextField();
    absField.setPrefColumnCount(8);


    relField = new TextField();
    relField.setPrefColumnCount(8);

    getChildren().addAll(new Label("abs="),absField, new Label("rel="), relField,
            new Label("%"));
  }

  public void setValue(AbsoluteNRelativeDouble value) {
    absField.setText(String.valueOf(value.getAbsolute()));
    relField.setText(String.valueOf(value.getRelative() * 100.0));
  }

  public AbsoluteNRelativeDouble getValue() {
    try {
      double abs = Double.parseDouble(absField.getText().trim());
      double rel = Double.parseDouble(relField.getText().trim()) / 100.0;
      AbsoluteNRelativeDouble value = new AbsoluteNRelativeDouble(abs, rel);
      return value;
    } catch (NumberFormatException e) {
      return null;
    }

  }

  public void setToolTipText(String toolTip) {
    absField.setTooltip(new Tooltip(toolTip));
    relField.setTooltip(new Tooltip(toolTip));
  }
}
