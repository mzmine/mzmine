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
package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

/**
 * Parameter for center measure: median, avg, weighted avg
 *
 */
public class CenterMeasureComponent extends FlowPane {

  private final ComboBox<CenterMeasure> comboCenterMeasure;
  private ComboBox<Weighting> comboTransform;
  private Label labelTrans;

  public CenterMeasureComponent() {
    this(CenterMeasure.values(), Weighting.values());
  }

  public CenterMeasureComponent(CenterMeasure choices[]) {
    this(choices, Weighting.values());
  }

  public CenterMeasureComponent(Weighting[] avgTransform) {
    this(CenterMeasure.values(), avgTransform);
  }

  public CenterMeasureComponent(CenterMeasure choices[], Weighting[] avgTransform) {
    this(choices, avgTransform, CenterMeasure.values()[0], Weighting.values()[0]);
  }

  /**
   *
   * @param choices
   * @param avgTransform
   * @param selected selected center measure
   * @param selWeighting selected weighting
   */
  public CenterMeasureComponent(CenterMeasure choices[], Weighting[] avgTransform,
      CenterMeasure selected, Weighting selWeighting) {
    // setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

    comboCenterMeasure = new ComboBox<>(FXCollections.observableArrayList(choices));
    comboCenterMeasure.getSelectionModel().select(selected);
    getChildren().add(comboCenterMeasure);

    if (avgTransform != null && avgTransform.length > 0) {
      labelTrans = new Label("weighting: ");
      getChildren().add(labelTrans);
      comboTransform = new ComboBox<>(FXCollections.observableArrayList(avgTransform));
      comboTransform.getSelectionModel().select(selWeighting);
      getChildren().add(comboTransform);

      // do not show weighting for median
      comboCenterMeasure.setOnAction(il -> {
        checkWeightingComponentsVisibility();
      });
      //
      checkWeightingComponentsVisibility();
    }
  }

  private void checkWeightingComponentsVisibility() {
    boolean visible =
        comboCenterMeasure.getSelectionModel().getSelectedItem().equals(CenterMeasure.AVG);
    comboTransform.setVisible(visible);
    labelTrans.setVisible(visible);
  }

  public void setToolTipText(String toolTip) {
    comboCenterMeasure.setTooltip(new Tooltip(toolTip));
  }

  public CenterFunction getSelectedFunction() {
    CenterMeasure measure = comboCenterMeasure.getSelectionModel().getSelectedItem();
    Weighting trans = Weighting.NONE;
    if (comboTransform != null && comboTransform.isVisible())
      trans = comboTransform.getSelectionModel().getSelectedItem();
    return new CenterFunction(measure, trans);
  }

  public void setSelectedItem(CenterMeasure newValue, Weighting transform) {
    comboCenterMeasure.getSelectionModel().select(newValue);
    comboTransform.getSelectionModel().select(transform);
  }

  /*
   * public void addItemListener(ItemListener il) { comboCenterMeasure.addItemListener(il);
   * comboTransform.addItemListener(il); }
   */

  public void setSelectedItem(CenterFunction newValue) {
    comboCenterMeasure.getSelectionModel().select(newValue.getMeasure());
    if (comboTransform != null)
      comboTransform.getSelectionModel().select(newValue.getWeightTransform());
  }
}
