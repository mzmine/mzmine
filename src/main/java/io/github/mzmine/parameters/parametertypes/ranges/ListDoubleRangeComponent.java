/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
