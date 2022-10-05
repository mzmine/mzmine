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
