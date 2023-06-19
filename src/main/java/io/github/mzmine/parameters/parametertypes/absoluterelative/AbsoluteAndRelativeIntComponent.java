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

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.Nullable;

public class AbsoluteAndRelativeIntComponent extends HBox {

  private final TextField absField;
  private final TextField relField;

  public AbsoluteAndRelativeIntComponent(String absUnit) {
    setAlignment(Pos.CENTER_LEFT);
    setSpacing(5);

    absField = new TextField();
    absField.setPrefColumnCount(4);
    absField.setAlignment(Pos.CENTER_RIGHT);

    relField = new TextField();
    relField.setPrefColumnCount(4);
    relField.setAlignment(Pos.CENTER_RIGHT);

    getChildren().addAll(new Label("Max of"), absField, new Label(absUnit + "  or "), relField,
        new Label("%"));
  }

  public AbsoluteAndRelativeInt getValue() {
    try {
      int abs = Integer.parseInt(absField.getText().trim());
      float rel = Float.parseFloat(relField.getText().trim()) / 100.f;
      AbsoluteAndRelativeInt value = new AbsoluteAndRelativeInt(abs, rel);
      return value;
    } catch (NumberFormatException e) {
      return null;
    }

  }

  public void setValue(@Nullable AbsoluteAndRelativeInt value) {
    if(value==null){
      absField.setText("");
      relField.setText("");
      return;
    }

    absField.setText(String.valueOf(value.getAbsolute()));
    relField.setText(String.valueOf(value.getRelative() * 100.f));
  }

  public void setToolTipText(String toolTip) {
    absField.setTooltip(new Tooltip(toolTip));
    relField.setTooltip(new Tooltip(toolTip));
  }

}
